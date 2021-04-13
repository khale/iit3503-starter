#ifndef __RAM_H__
#define __RAM_H__
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>

typedef struct ram {
    unsigned short * ram;
    size_t size;
} ram_t;

struct dut;

ram_t * create_ram (size_t size, char * img, char * os_image, uint16_t * entry);
void destroy_ram(ram_t * ram);

#endif
