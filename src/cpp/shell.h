#ifndef __SHELL_H__
#define __SHELL_H__

#define PROMPT_STR GREEN(BOLD("(3503-dbg-shell)$> "))

struct dut;
void run_shell (struct dut * dut, bool interactive);
#endif
