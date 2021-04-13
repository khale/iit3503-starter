#include "common.h"
#include <arpa/inet.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include "ram.h"
#include "iit3503.h"

extern dut_t * dut;

static uint16_t
load_image (ram_t * ram, const char *img, const char * desc) {
    int ret;
    struct stat st;
    size_t size = 0;
    uint16_t orig;

    stat(img, &st);

    if (st.st_size-2 > (ram->size*sizeof(word_t))) {
        size = ram->size*sizeof(word_t);
    } else {
        size = st.st_size-2;
    }

    FILE *fp = fopen(img, "rb");
    if (fp == NULL) {
        ERROR_PRINT("Could not open file '%s'", img);
        exit(EXIT_FAILURE);
    }

    // lop off the .ORIG space (for the OS this should be x0000)
    ret = fread(&orig, 1, 2, fp);
    assert(ret == 2);

    ret = fread(&ram->ram[htobe16(orig)], 1, size, fp);
    assert(ret == size);

    DEBUG_PRINT("Loading %s image at x%04x", desc, htobe16(orig));

    fclose(fp);

    return orig;
}


static inline uint16_t
load_os_image (ram_t * ram, const char * img)
{
    return load_image(ram, img, "OS");
}


static inline uint16_t
load_program_image (ram_t * ram, const char * img, bool super)
{
    uint16_t orig = load_image(ram, img, "program");

    if (!super) {
        // set the user entry point
        ram->ram[0x0200] = orig;
    }

    return orig;
}



ram_t *
create_ram (size_t size, char * img, char * os_img, uint16_t * entry) 
{
    ram_t * ram = (ram_t*)malloc(sizeof(ram_t));

    if (!ram) {
        ERROR_PRINT("Could not create RAM state");
        return NULL;
    }
    memset(ram, 0, sizeof(ram_t));

    ram->size = size;
    ram->ram = (word_t*)malloc(sizeof(word_t)*size);
    if (!ram->ram) {
        ERROR_PRINT("Could not allocate RAM");
        goto out_err1;
    }

    if (os_img) {
        *entry = 0x2ca;
        load_os_image(ram, os_img);
        load_program_image(ram, img, false);
    } else {
        *entry = load_program_image(ram, img, true);
    }

    for(int i = 0; i < size; i++) {
        ram->ram[i] = htobe16(ram->ram[i]);
    }

    return ram;

out_err1:
    free(ram);
    return NULL;
}

void
destroy_ram (ram_t * ram)
{
    free(ram->ram);
    free(ram);
}

// hooks into verilog
extern "C" void extern_ram (uint8_t en, 
                            uint8_t wEn,
                            word_t dataIn,
                            paddr_t addr,
                            word_t* dataOut,
                            uint8_t* R) {

    if (en) {
#if 0
        DEBUG_PRINT("RAM access:\n"
                "\taddr=%04hx\n" 
                "\t*dataOut=%04hx\n"
                "\tdataIn=%04hx\n"
                "\tEn=%01hhx\n" 
                "\twEn=%01hhx\n"
                "\tR=%01hhx\n", 
                addr,  
                *dataOut, 
                dataIn, 
                en, 
                wEn,
                *R);
#endif
        *dataOut = dut->ram->ram[addr];
        if (wEn) {
            dut->ram->ram[addr] = dataIn;
        }
        *R = 1;
    } else {
        *R = 0;
    }
}
