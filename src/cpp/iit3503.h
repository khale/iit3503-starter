#ifndef __IIT3502_H__
#define __IIT3502_H__

#include <stdlib.h>
#include <stdint.h>

#define IIT3503_RAMSIZE (1<<16)

struct ram;
struct Vtop;
struct VerilatedVcdC;

typedef struct dut {
    struct VTop * top;
    struct VerilatedVcdC* tfp;

    bool trace_en;
    bool haltquit;

    struct ram * ram;
    uint64_t cycle_count;

    uint64_t main_time;
    uint64_t timeout;

    uint16_t resetvec;

    const char * image;
    const char * trace;
    const char * os_image;
} dut_t;

dut_t * iit3503_init(bool trace_en, bool haltquit, char * trace, char * image, char * os_image);

bool iit3503_step_cycle(dut_t * dut, bool reset);
bool iit3503_step_instr(dut_t * dut, bool reset);
void iit3503_reset(dut_t * dut);
void iit3503_deinit(dut_t * dut);
void iit3503_raise_irq (dut_t * dut, uint8_t irq, uint8_t priority, uint16_t data);
void iit3503_instr_repr (dut_t * dut, uint16_t addr, char * buf, size_t buflen);

void uart_rx(uint8_t rxd);


#endif
