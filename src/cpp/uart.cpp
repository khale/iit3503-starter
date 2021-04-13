#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <stdint.h>
#include <cstdio>

#include "common.h"

#define FREQ 50000000
#define BAUD 115200


// TODO: CLEANUP

static int rx_bit_count = ((FREQ + BAUD/2) / BAUD-1);
static int rx_start_cnt = ((3*FREQ/2+BAUD/2)/BAUD-1);
static int rx_shift_reg = 0;
static int rx_cnt_reg = 0;
static int rx_bits_reg = 0;
static int rx_val_reg = 0;

#define MAX_LINE_LEN 80

static char console_buf[MAX_LINE_LEN];
static int console_ptr = 0;

static inline void
uart_buf_flush () {
    console_ptr = 0;
    printf("%s", console_buf);
    fflush(stdout);
    memset(console_buf, 0, MAX_LINE_LEN);
}


static void 
uart_push (char c)
{
#if 0
    if (c == '\n' || c == '\0') {
        uart_buf_flush();
        return;
    } else {
        console_buf[console_ptr++] = c;
        if (console_ptr == MAX_LINE_LEN) {
            uart_buf_flush();
        }
    }
#endif
    putc(c, stdout); // TODO: if this isn't flushed we'll miss prompts
    //fflush(stdout);
}


void 
uart_rx (uint8_t rxd) 
{
    if (rx_cnt_reg) {
        rx_cnt_reg--;
    } else if (rx_bits_reg) {
        rx_cnt_reg = rx_bit_count;
        rx_shift_reg = (rx_shift_reg >> 1) | (rxd << 7);

        if (rx_bits_reg == 1) {
            rx_val_reg = 1;
        }

        rx_bits_reg--;
    } else if (!rxd) {
        rx_cnt_reg = rx_start_cnt;
        rx_bits_reg = 8;
    }

    if (rx_val_reg) {
        uart_push(rx_shift_reg);
        rx_val_reg = 0;
        rx_shift_reg = 0;
    }
}
