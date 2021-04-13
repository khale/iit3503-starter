#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdnoreturn.h>

#include <signal.h>

#include "common.h"
#include "shell.h"
#include "iit3503.h"
#include "ram.h"

#include "VTop.h"
#include <readline/history.h>
#include <readline/readline.h>


// Returns the next whitespace-delimited token in the string pointed to by
// `line_ptr`. A token which starts with the first character in `*line_ptr`
// still counts. This destructively modifies `*line_ptr` by writing a NULL
// character into the token boundry. `*line_ptr` is modified to point to the
// character immediately after that which ended the token, or the end of the
// string if it's reached. If `*line_ptr` contains only whitespace or is empty,
// an empty string is returned. The returned string aliases contents from
// `*line_ptr`, and therefore has the same lifetime.
static inline char *
next_token (char ** line_ptr)
{
	while (isspace(**line_ptr)) {
		(*line_ptr)++;
	}

	char * token_start = *line_ptr;

	bool done = false;
	while (!isspace(**line_ptr) && !(done = !**line_ptr)) {
		(*line_ptr)++;
	}

	if (!done) {
		**line_ptr = 0;
		(*line_ptr)++;
	}

	return token_start;
}

// Tries to parse the next whitespace-delimited token in `*line_ptr` as an
// unsigned hexadecimal integer. If successful, the parsed value is stored in
// `result`, and `NULL` is returned. Otherwise, the contents of `result` are
// undefined, and the token which failed to parse is returned.
static inline char *
next_hex (char ** line_ptr, size_t * result)
{
	char * original_token = next_token(line_ptr);

	if (!*original_token) {
		return original_token;
	}

	char * remaining_token = original_token;
	*result = 0;
	goto skip_shift;

	do {
		*result *= 16;
	skip_shift:
		if (*remaining_token > 47 && *remaining_token < 58) {
			*result += (size_t)(*remaining_token - '0');
		}
		else if (*remaining_token > 64 && *remaining_token < 71) {
			*result += (size_t)(*remaining_token - 'A' + 10);
		}
		else if (*remaining_token > 96 && *remaining_token < 103) {
			*result += (size_t)(*remaining_token - 'a' + 10);
		}
		else {
			return original_token;
		}
		remaining_token++;
	} while (*remaining_token);

	return NULL;
}

// Tries to parse the next whitespace-delimited token in `*line_ptr` as an
// unsigned hexadecimal integer. If successful, the parsed value is stored in
// `result`, and `NULL` is returned. Otherwise, the contents of `result` are
// undefined, and the token which failed to parse is returned.
static inline char *
next_dec (char ** line_ptr, size_t * result)
{
	char * original_token = next_token(line_ptr);

	if (!*original_token) {
		return original_token;
	}

	char * remaining_token = original_token;
	*result = 0;
	goto skip_shift;

	do {
		*result *= 10;
	skip_shift:
		if (*remaining_token > 47 && *remaining_token < 58) {
			*result += (size_t)(*remaining_token - '0');
		}
		else {
			return original_token;
		}
		remaining_token++;
	} while (*remaining_token);

	return NULL;
}

// Similar to `next_hex`, except it `ERROR_PRINT`s on failure and returns a
// non-zero error code.
static inline int
try_next_hex (char ** line_ptr, size_t * result)
{
	char * token = next_hex(line_ptr, result);
	if (token) {
		ERROR_PRINT("  '%s' is not a valid positive hexadecimal integer", token);
		return -1;
	}
	return 0;
}

// Similar to `next_dec`, except it `ERROR_PRINT`s on failure and returns a
// non-zero error code.
static inline int
try_next_dec (char ** line_ptr, size_t * result)
{
	char * token = next_dec(line_ptr, result);
	if (token) {
		ERROR_PRINT("  '%s' is not a valid positive decimal integer", token);
		return -1;
	}
	return 0;
}

// Checked during CPU stepping to abort early on SIGINT
static bool sigint_received;

// break point level 2
static uint8_t * bptl2[256];

#define BP_L2_IDX(x) (((x) >> 8) & 0xFF)
#define BP_L1_IDX(x) (((x)&0xFF))
#define BP_PRESENT(x) ((x)&0x1)
#define BP_SET_PRESENT(x) ((x) | 0x1)
#define BP_SET_NOT_PRESENT(x) ((x)&0xFE)

static int
insert_bp (uint16_t bp_addr)
{
	uint8_t bpt_idx = BP_L2_IDX(bp_addr);
	uint8_t * bpt = NULL;
	uint8_t bpte;

	// no bpt yet
	if (!bptl2[bpt_idx]) {
		// allocate new bpt
		bptl2[bpt_idx] = (uint8_t*)malloc(256);
		memset(bptl2[bpt_idx], 0, 256);
	}

	bpt = bptl2[bpt_idx];
	bpte = bpt[BP_L1_IDX(bp_addr)];

	if (BP_PRESENT(bpte)) {
		ERROR_PRINT("  Breakpoint at $%04X already exists", bp_addr);
		return -1;
	}
	else {
		bpt[BP_L1_IDX(bp_addr)] = BP_SET_PRESENT(bpte);
	}

	return 0;
}

static void
bp_list (void)
{
	int c = 0;
	printf("Breakpoint List:\n");
	for (size_t i = 0; i < 256; i++) {
		if (bptl2[i]) {
			uint8_t * bpt = bptl2[i];
			for (size_t j = 0; j < 256; j++) {
				uint8_t bpte = bpt[j];
				if (BP_PRESENT(bpte)) {
					INFO_PRINT("  %d: $%04x", c++, (uint16_t)((i << 8) | j));
				}
			}
		}
	}
}

static bool
is_valid_bp (uint16_t bp_addr)
{
	uint8_t bpt_idx = BP_L2_IDX(bp_addr);
	uint8_t * bpt = NULL;
	uint8_t bpte;

	if (!bptl2[bpt_idx]) {
		return 0;
	}

	bpt  = bptl2[bpt_idx];
	bpte = bpt[BP_L1_IDX(bp_addr)];

	return BP_PRESENT(bpte);
}

static int
remove_bp (uint16_t bp_addr)
{
	uint8_t bpt_idx = BP_L2_IDX(bp_addr);
	uint8_t * bpt = NULL;
	uint8_t bpte;

	if (!bptl2[bpt_idx]) {
		return -1;
	}

	bpt  = bptl2[bpt_idx];
	bpte = bpt[BP_L1_IDX(bp_addr)];

	if (BP_PRESENT(bpte)) {
		bpt[BP_L1_IDX(bp_addr)] = BP_SET_NOT_PRESENT(bpte);
		return 0;
	}

	return -1;
}


// Prints a helpful message (for when the PC changes) that indicates the new PC
// and the corresponding instruction
static void
print_pc_update (dut_t * dut)
{
	char buffer[32] = {0};
	iit3503_instr_repr(dut, dut->top->io_debugPC, buffer, sizeof(buffer));
    INFO_PRINT("  Cycles elapsed: %lu", dut->cycle_count);
    INFO_PRINT("  uPC now at %u", dut->top->io_debuguPC);
	INFO_PRINT("  PC now at x%04x: %s", dut->top->io_debugPC, buffer);
}


#define GET_HEX_ADDR(id)                                           \
	if (try_next_hex(&args, &id)) {                            \
		return -1;                                         \
	}                                                          \
	if (id > UINT16_MAX) {                                     \
		ERROR_PRINT("  Address $%zx is out of range", id); \
		return 0;                                          \
	}

static void print_usage(void);
static int
cmd_help (dut_t * cpu, char * args)
{
	print_usage();
	return 0;
}

static int
cmd_step_cycle (dut_t * dut, char * args)
{
	size_t n;
	if (*args) {
		if (try_next_dec(&args, &n)) {
			return -1;
		}
	}
	else {
		n = 1;
	}

	bool bp_hit = false;
	uint16_t last_pc = dut->top->io_debugPC;

	for (; n && !((bp_hit = is_valid_bp(dut->top->io_debugPC)) && (dut->top->io_debuguPC == 18)) && !sigint_received; n--) {
		last_pc = dut->top->io_debugPC;
        if (iit3503_step_cycle(dut, false)) {
            break;
        }
	}

	if (bp_hit) {
		INFO_PRINT("Breakpoint at x%04x reached", dut->top->io_debugPC);
		remove_bp(dut->top->io_debugPC);
	} 

	print_pc_update(dut);
	return 0;
}

static int
cmd_step_instr (dut_t * dut, char * args)
{
	size_t n;
	if (*args) {
		if (try_next_dec(&args, &n)) {
			return -1;
		}
	}
	else {
		n = 1;
	}

	bool bp_hit = false;
	uint16_t last_pc = dut->top->io_debugPC;

	for (; n && !((bp_hit = is_valid_bp(dut->top->io_debugPC)) && (dut->top->io_debuguPC == 18)) && !sigint_received; n--) {
		if (iit3503_step_instr(dut, false)) {
            break;
        }
	}

	if (bp_hit) {
		INFO_PRINT("Breakpoint at $%04x reached", dut->top->io_debugPC);
		remove_bp(dut->top->io_debugPC);
	} 

	print_pc_update(dut);
	return 0;
}


static int
cmd_regs (dut_t * dut, char * args)
{
    INFO_PRINT("PC -> x%04x", dut->top->io_debugPC);
    INFO_PRINT("PSR -> x%04x: CC -> [N=%01u; Z=%01u; P=%01u] Priv -> %01hhx Priority=x%01hhx",
            dut->top->io_debugPSR,
            (dut->top->io_debugPSR >> 2) & 1,
            (dut->top->io_debugPSR >> 1) & 1,
            dut->top->io_debugPSR & 1,
            (dut->top->io_debugPSR >> 15) & 0x1,
            (dut->top->io_debugPSR >> 8) & 0x7);
    INFO_PRINT("R0 -> x%04x  ;  R1 -> x%04x", 
            dut->top->io_debugR0, dut->top->io_debugR1);
    INFO_PRINT("R2 -> x%04x  ;  R3 -> x%04x", 
            dut->top->io_debugR2, dut->top->io_debugR3);
    INFO_PRINT("R4 -> x%04x  ;  R5 -> x%04x", 
            dut->top->io_debugR4, dut->top->io_debugR5);
    INFO_PRINT("R6 -> x%04x  ;  R7 -> x%04x", 
            dut->top->io_debugR6, dut->top->io_debugR7);
	return 0;
}

static int
cmd_ustate (dut_t * dut, char * args)
{
    INFO_PRINT("Bus Output -> x%04x", dut->top->io_debugBus);
    INFO_PRINT("IR         -> x%04x", dut->top->io_debugIR);
    INFO_PRINT("uPC        -> %u", dut->top->io_debuguPC);
    INFO_PRINT("MAR -> x%04x  ;  MDR -> x%04x", dut->top->io_debugMAR, dut->top->io_debugMDR);
    INFO_PRINT("DSR -> x%04x  ;  DDR -> x%04x", dut->top->io_debugDSR, dut->top->io_debugDDR);
    INFO_PRINT("MCR -> x%04x", dut->top->io_debugMCR);
	return 0;
}

static int
cmd_allregs (dut_t * dut, char * args)
{
    cmd_regs(dut, args);
    INFO_PRINT("==============================");
    cmd_ustate(dut, args);
    return 0;
}

static int
cmd_peek (dut_t * dut, char * args)
{
	size_t addr;
	GET_HEX_ADDR(addr);

	INFO_PRINT("  x%04x: x%04x", (uint16_t)addr, dut->ram->ram[addr]);
	return 0;
}

static int
cmd_poke (dut_t * dut, char * args)
{
	size_t addr;
	GET_HEX_ADDR(addr);

	size_t val;
	if (try_next_hex(&args, &val)) {
		return -1;
	}
	if (val > UINT8_MAX) {
		ERROR_PRINT("  Byte value $%zx is out of range", val);
	}

    dut->ram->ram[addr] = val;
	return 0;
}



#define C_RED 91
#define C_GREEN 92
#define C_YELLOW 93
#define C_BLUE 94
#define C_MAGENTA 95
#define C_CYAN 96

#define C_RESET 0
#define C_GRAY 90

static void set_color(int code) {
  static int current_color = 0;
  if (code != current_color) {
    printf("\x1b[%dm", code);
    current_color = code;
  }
}

static void set_color_for(char c) {
  if (c >= 'A' && c <= 'z') {
    set_color(C_YELLOW);
  } else if (c >= '!' && c <= '~') {
    set_color(C_CYAN);
  } else if (c == '\n' || c == '\r') {
    set_color(C_GREEN);
  } else if (c == '\a' || c == '\b' || c == 0x1b || c == '\f' || c == '\n' || c == '\r') {
    set_color(C_RED);
  } else if ((unsigned char)c == 0xFF) {
    set_color(C_MAGENTA);
  } else {
    set_color(C_GRAY);
  }
}
void debug_hexdump_grouped(uint32_t base, void *vbuf, size_t len, int grouping) {
  unsigned awidth = 4;

  if (len > 0xFFFFL) awidth = 8;

  unsigned char *buf = (unsigned char *)vbuf;
  int w = 16;

	bool color = isatty(1);

  for (unsigned long long i = 0; i < len; i += w) {
    unsigned char *line = buf + i;

    if (color) set_color(C_RESET);
    printf("|");
    if (color) set_color(C_GRAY);

    printf("%.*llx", awidth, i + base);

    if (color) set_color(C_RESET);
    printf("|");
    for (int c = 0; c < w; c++) {
      if (c % grouping == 0) printf(" ");

      if (i + c >= len) {
        if (color) set_color(C_RED);
        printf("--");
      } else {
        if (color) set_color_for(line[c]);
        printf("%02X", line[c]);
      }
    }

    if (color) set_color(C_RESET);
    printf(" |");
    for (int c = 0; c < w; c++) {
      if (c != 0 && (c % 8 == 0)) {
        if (color) set_color(C_RESET);
        printf(" ");
      }

      if (i + c >= len) {
        if (color) set_color(C_RED);
        printf("-");
      } else {
        if (color) set_color_for(line[c]);
        printf("%c", (line[c] < 0x20) || (line[c] > 0x7e) ? '.' : line[c]);
      }
    }
    if (color) set_color(C_RESET);
    printf("|\n");
  }
}

static int
cmd_dumpmem (dut_t * dut, char * args)
{
	size_t addr;
	GET_HEX_ADDR(addr);

	size_t count = 0;
	if (try_next_dec(&args, &count)) {
		return -1;
	}

	debug_hexdump_grouped(addr, dut->ram->ram + addr, count * 2, 2);
	return 0;
}

static int
cmd_irq (dut_t * dut, char * args)
{
    size_t irq;
    size_t priority = 4;
    size_t data = 0;
    
    GET_HEX_ADDR(irq);
    
    if (try_next_dec(&args, &priority)) {
        return -1;
    }

    GET_HEX_ADDR(data);

    INFO_PRINT("Raising IRQ: x%02lx, Priority=%ld, Data=x%04lx",
            irq,
            priority,
            data);

	iit3503_raise_irq(dut, (uint8_t)irq, (uint8_t)priority, (uint16_t)data);
	return 0;
}


static int
cmd_print_instr (dut_t * dut, char * args)
{
	char buffer[32];
	iit3503_instr_repr(dut, dut->top->io_debugPC, buffer, sizeof(buffer));
	INFO_PRINT("  x%04x: %s", dut->top->io_debugPC, buffer);
	return 0;
}

static int
cmd_cont (dut_t * dut, char * args)
{
	bool hit_bp = false;
	uint16_t last_pc = dut->top->io_debugPC;

	while (!((hit_bp = is_valid_bp(dut->top->io_debugPC)) && (dut->top->io_debuguPC == 18)) && !sigint_received) {
		last_pc = dut->top->io_debugPC;
		if (iit3503_step_cycle(dut, false)) {
            break;
        }
	}

	if (hit_bp) {
		INFO_PRINT("  Breakpoint at $%04x reached", dut->top->io_debugPC);
		remove_bp(dut->top->io_debugPC);
	} 

	print_pc_update(dut);

	return 0;
}

static int
cmd_break_rm (dut_t * cpu, char * args)
{
	size_t addr;
	GET_HEX_ADDR(addr);

	if (remove_bp((uint16_t)addr)) {
		ERROR_PRINT("  Couldn't remove a breakpoint at $%04x", (uint16_t)addr);
		return 0;
	}

	INFO_PRINT("  Breakpoint at $%04x removed", (uint16_t)addr);
	return 0;
}

static int
cmd_break_list (dut_t * cpu, char * args)
{
	bp_list();
	return 0;
}

static int
cmd_break (dut_t * cpu, char * args)
{
	size_t addr;
	GET_HEX_ADDR(addr);

	if (insert_bp((uint16_t)addr)) {
		ERROR_PRINT("  Couldn't set a breakpoint at $%04x", (uint16_t)addr);
		return 0;
	}

	INFO_PRINT("  Breakpoint set at $%04x", (uint16_t)addr);
	return 0;
}

static int __attribute__((noreturn))
cmd_quit (dut_t * cpu, char * args)
{
	printf("  Quitting. Goodbye.\n");
	exit(0);
}

#define SPELLINGS(...) ((const char * []){__VA_ARGS__, NULL})

typedef struct command_descriptor {
	const char * const * spellings;
	const char * usage;
	const char * description;
	int (*handler)(dut_t *, char *);
} command_descriptor_t;

static const command_descriptor_t commands[] = {
	{SPELLINGS("help", "?", "h"),
		"",
		"Prints a list of the available commands",
		cmd_help},

	{SPELLINGS("step", "s"),
		"[dec n] ",
		"Steps the CPU by n cycles (default 1)",
		cmd_step_cycle},

	{SPELLINGS("stepi", "si"),
		"[dec n] ",
		"Steps the CPU by n instructions (default 1)",
		cmd_step_instr},

	{SPELLINGS("continue", "c"),
		"",
		"Continues simulation execution",
		cmd_cont},

	{SPELLINGS("regs", "r"),
		"",
		"Prints the values of all CPU registers",
		cmd_regs},

    {SPELLINGS("ustate", "u"),
        "",
        "Prints the values of internal datapath/control registers",
        cmd_ustate},

    {SPELLINGS("allregs", "ar"),
        "",
        "Prints the values of all registers in the machine",
        cmd_allregs},

	{SPELLINGS("peek", "pk"),
		"<hex16 addr> ",
		"Prints the byte at addr",
		cmd_peek},

	{SPELLINGS("poke", "po"),
		"<hex16 addr> <hex8 value> ",
		"Sets the byte at addr to value",
		cmd_poke},

	{SPELLINGS("dumpmem", "dm"),
		"<hex16 start> <dec length> ",
		"Dumps the memory in the address range [start, start+length)",
		cmd_dumpmem},

	{SPELLINGS("quit", "exit", "q"),
		"",
		"Terminates the simulation",
		cmd_quit},

	{SPELLINGS("irq"),
		"<hex16 irqnum> <dec priority> <hex16 data>",
		"Raises the specified IRQ",
		cmd_irq},

	{SPELLINGS("pr", "print"),
		"",
		"Prints the current instruction",
		cmd_print_instr},

	{SPELLINGS("break-rm", "b-rm"),
		"<hex16 addr> ",
		"Removes a breakpoint at addr",
		cmd_break_rm},

	{SPELLINGS("break-list", "b-list"),
		"",
		"Lists all active breakpoints",
		cmd_break_list},

	{SPELLINGS("break", "b"),
		"<hex16 addr> ",
		"Sets a breakpoint at addr",
		cmd_break},
};

static void
print_usage (void)
{
	for (size_t i = 0; i < sizeof(commands) / sizeof(*commands); i++) {
		const char * const * spelling_ptr = commands[i].spellings;
		SUGGESTION_PRINT_NOBRK("  ");
		goto skip_or;

		for (; *spelling_ptr; spelling_ptr++) {
			SUGGESTION_PRINT_NOBRK(UNBOLD(" or "));
		skip_or:
			SUGGESTION_PRINT_NOBRK("%s", *spelling_ptr);
		}

		SUGGESTION_PRINT("  %s" UNBOLD("-- %s"), commands[i].usage, commands[i].description);
	}
}

static int
handle_cmd (dut_t * dut, char * line)
{
	char * cmd = next_token(&line);
	for (size_t i = 0; i < sizeof(commands) / sizeof(command_descriptor_t); i++) {
		for (const char * const * spelling_ptr = commands[i].spellings; *spelling_ptr; spelling_ptr++) {
			if (!strcmp(cmd, *spelling_ptr)) {
				sigint_received = false;

				int retcode = commands[i].handler(dut, line);
				if (retcode) {
					ERROR_PRINT("  Invalid syntax");
					SUGGESTION_PRINT("  Usage: %s %s", *spelling_ptr, commands[i].usage);
				}

				return retcode;
			}
		}
	}
	ERROR_PRINT("  Unknown command");
	return -1;
}

static void
handle_sigint (int signum)
{
	sigint_received = true;
}

static const struct sigaction sigint_action = {
    handle_sigint,
    0,
    0,
        0,
    0
};

void
run_shell (dut_t * dut, bool interactive)
{
	char * line = NULL;
	if (sigaction(SIGINT, &sigint_action, NULL)) {
		ERROR_PRINT("  Couldn't register a SIGINT handler");
		return;
	}

	if (interactive) {
		goto prompt;
	}

	do {
		if (line && line[0]) {
			add_history(line);
			handle_cmd(dut, line);
		} else if (!line) {
			sigint_received = false;
			cmd_cont(dut, NULL);
		}

		if (line) {
			free(line);
		}
	prompt:
		continue;
	} while ((line = readline(PROMPT_STR)));
}
