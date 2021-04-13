#include <string.h>
#include <iostream>
#include "common.h"
#include "iit3503.h"
#include "shell.h"
#include "ram.h"

#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VTop.h"
using namespace std;

const char * mnemonics[16] = {
    "BR",
    "ADD",
    "LD",
    "ST",
    "JSR",
    "AND",
    "LDR",
    "STR",
    "RTI",
    "NOT",
    "LDI",
    "STI",
    "RET",
    "(undefined)",
    "LEA",
    "TRAP",
};

const char * regnames[8] = {
    "R0",
    "R1",
    "R2",
    "R3",
    "R4",
    "R5",
    "R6",
    "R7",
};


static bool
check_should_halt (dut_t * dut)
{
    if (dut->top->io_halt) {
        INFO_PRINT("Machine halted.");
        if (dut->haltquit) {
            printf("  Quitting. Goodbye.\n");
            exit(0);
        }
        return true;
    }
    return false;
}

static void
check_for_kbd (dut_t * dut)
{
    fd_set rfds;
    FD_ZERO(&rfds);
    struct timeval tv;

    FD_SET(fileno(stdin), &rfds);
    tv.tv_sec = 0;
    tv.tv_usec = 0; 

    if (select(fileno(stdin)+1, &rfds, NULL, NULL, &tv) > 0) {
        unsigned char c = fgetc(stdin);
        iit3503_raise_irq(dut, 0x80, 4, (uint16_t)c);
    }
}

bool
iit3503_step_cycle (dut_t * dut, bool reset)
{
    if (!reset)
        uart_rx((uint8_t)dut->top->io_uartTxd);

    check_for_kbd(dut);

    // reset device interrupt ready signal
    if (dut->top->io_intAck) {
        dut->top->io_devReady = 0;
        dut->top->io_intAck = 0;
    }


    dut->top->clock = 1;
    dut->top->eval();
    if (dut->trace_en) {
        dut->tfp->dump((double)dut->main_time);
    }
    dut->main_time++;

    dut->top->clock = 0;
    dut->top->eval();
    if (dut->trace_en) {
        dut->tfp->dump((double)dut->main_time);
    }
    dut->main_time++;
    dut->cycle_count++;

    if (!reset)
        return check_should_halt(dut);

    return false;
}


bool
iit3503_step_instr (dut_t * dut, bool reset)
{
    bool halt = false;
    do {
        halt = iit3503_step_cycle(dut, reset);
    } while (dut->top->io_debuguPC != 18 && !halt);

    return halt;
}


void
iit3503_reset (dut_t * dut)
{
    cout << "Reset." << endl;

    // reset
    dut->top->reset = 1;
    for (int i = 0; i < 5; i++) {
        iit3503_step_cycle(dut, true);
    }

    dut->top->reset = 0;
}


dut_t *
iit3503_init (bool trace_en, bool haltquit, char * trace, char * image, char * os_image)
{
    uint16_t entry;
    dut_t * dut = (dut_t*)malloc(sizeof(dut_t));
    if (!dut) {
        ERROR_PRINT("Could not allocate DUT");
        return NULL;
    }
    memset(dut, 0, sizeof(dut_t));

    dut->trace    = trace;
    dut->image    = image;
    dut->os_image = os_image;
    dut->top      = new VTop;

    dut->trace_en = trace_en;
    dut->haltquit = haltquit;

    if (dut->trace_en) {
        dut->tfp = new VerilatedVcdC;
        cout << "Enabling timing output." << endl;
        Verilated::traceEverOn(true);
        dut->top->trace(dut->tfp, 99); // trace 99 levels of module hierarchy
        dut->tfp->open(dut->trace);
    }

    dut->ram = (ram_t*)create_ram(IIT3503_RAMSIZE, image, os_image, &entry);
    if (!dut->ram) {
        ERROR_PRINT("Could not create RAM");
        return NULL;
    }

    dut->resetvec = entry;

    dut->top->io_resetVec = entry;

    return dut;
}


void
iit3503_deinit (dut_t * dut)
{
    destroy_ram(dut->ram);
    dut->top->final();
    delete dut->top;

    if (dut->trace_en) {
        dut->tfp->close();
        delete dut->tfp;
    }

    free(dut);
}

/*
 * We only have a keyboard device:
 *  - int priority = 4
 *  - int vec = x80
 */
void
iit3503_raise_irq (dut_t * dut, uint8_t irqnum, uint8_t priority, uint16_t data)
{
    dut->top->io_intPriority = priority;
    dut->top->io_intv        = irqnum;
    dut->top->io_devReady = 1;
    dut->top->io_devData  = data;
}


static int
sign_ext (unsigned val, int len)
{
    uint8_t sign = (val >> (len-1)) & 1;

    if (sign) {
        unsigned mask = ~((1<<len) - 1);
        unsigned newval = val | mask;
        return (int)newval;
    } 
    return (int)val;
}

void
iit3503_instr_repr (dut_t * dut, uint16_t addr, char * buf, size_t buflen)
{
    uint8_t u_pc = (uint8_t)dut->top->io_debuguPC;
    uint16_t pc = (uint16_t)dut->top->io_debugPC;
    uint16_t ir;
    uint8_t op;

    switch (u_pc) {
        case 18:
        case 33:
        case 28:
        case 30:
            ir = dut->ram->ram[pc]; // IR hasn't been loaded yet, so we get it directly
            break;
        default:
            ir = (uint16_t)dut->top->io_debugIR;
            break;
    }

    op = (ir >> 12) & 0xf;

    switch (op) {
        case 0: 
            snprintf(buf, buflen, "%s (%s%s%s) PCoffset9=%d", 
                    mnemonics[op],
                    ((ir >> 11) & 1) ? "n" : "",
                    ((ir >> 10) & 1) ? "z" : "",
                     ((ir >> 9) & 1) ? "p" : "",
                     sign_ext(ir&0x1ff, 9));

            break;
        case 2:
            snprintf(buf, buflen, "%s DR=%s, PCoffset9=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    sign_ext(ir&0x1ff, 9));
            break;
        case 3:
            snprintf(buf, buflen, "%s SR=%s, PCoffset9=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    sign_ext(ir&0x1ff, 9));
            break;
        case 4: {
            if ((ir >> 11) & 1) {
                snprintf(buf, buflen, "%s PCoffset11=%d",
                        mnemonics[op],
                        sign_ext(ir & 0x7ff, 11));
            } else {

                snprintf(buf, buflen, "%s BaseR=%s",
                        "JSRR",
                        regnames[(ir >> 6) & 0x7]);
            }
            break;
        }
        case 1:
        case 5: {
            if ((ir >> 5) & 1) { // imm
                snprintf(buf, buflen, "%s DR=%s, SR1=%s, imm5=%d",
                        mnemonics[op],
                        regnames[(ir >> 9) & 0x7],
                        regnames[(ir >> 6) & 0x7],
                        sign_ext(ir & 0x1f, 5));
            } else {
                snprintf(buf, buflen, "%s DR=%s, SR1=%s, SR2=%s",
                        mnemonics[op],
                        regnames[(ir >> 9) & 0x7],
                        regnames[(ir >> 6) & 0x7],
                        regnames[ir & 0x7]);
            }
        }
        break;
        case 6:
            snprintf(buf, buflen, "%s DR=%s, baseR=%s, offset6=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    regnames[(ir >> 6) & 0x7],
                    sign_ext(ir&0x3f, 6));
            break;
        case 7:
            snprintf(buf, buflen, "%s SR=%s, baseR=%s, offset6=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    regnames[(ir >> 6) & 0x7],
                    sign_ext(ir&0x3f, 6));
            break;
        case 8:
            snprintf(buf, buflen, "%s", mnemonics[op]);
            break;
        case 9:
            snprintf(buf, buflen, "%s DR=%s, SR=%s",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    regnames[(ir >> 6) & 0x7]);
            break;
        case 10:
            snprintf(buf, buflen, "%s DR=%s, PCoffset9=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    sign_ext(ir&0x1ff, 9));
            break;
        case 11:
            snprintf(buf, buflen, "%s SR=%s, PCoffset9=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    sign_ext(ir&0x1ff, 9));
            break;
        case 12:
            snprintf(buf, buflen, "%s BaseR=%s",
                    ((ir>>6) & 0x7) == 7 ? "RET" : mnemonics[op],
                    regnames[(ir >> 9) & 0x7]);
            break;
        case 13:
            snprintf(buf, buflen, "Reserved opcode (1101)");
            break;
        case 14: 
            snprintf(buf, buflen, "%s DR=%s PCoffset9=%d",
                    mnemonics[op],
                    regnames[(ir >> 9) & 0x7],
                    sign_ext(ir&0x1ff, 9));
            break;
        case 15: {
            const char * tn = "unknown trap";
            uint8_t trap_no = ir & 0xff;

            if (trap_no == 0x20) 
                tn = "GETC";
            else if (trap_no == 0x21) 
                tn = "OUT";
            else if (trap_no == 0x22)
                tn = "PUTS";
            else if (trap_no == 0x23)
                tn = "IN";
            else if (trap_no == 0x24)
                tn = "PUTSP";
            else if (trap_no == 0x25)
                tn = "HALT";

            snprintf(buf, buflen, "%s vec=x%02x (%s)",
                    mnemonics[op],
                    trap_no,
                    tn);
        }
        break;
        default:
            snprintf(buf, buflen, "Unknown instruction");
            break;
    }

}
