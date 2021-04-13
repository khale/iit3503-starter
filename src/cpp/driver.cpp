#include <verilated.h>
#include <verilated_vcd_c.h>
#include <iostream>
#include <fstream>
#include <cstring>
#include <unistd.h>
#include <getopt.h>
#include "common.h"
#include "iit3503.h"
#include "shell.h"

#define MAX_IMAGE_NAME_LEN 256

using namespace std;

dut_t * dut;

static void
print_version (void)
{
    INFO_PRINT("iit3503 Simulator Version " IIT3503_VERSION_STRING);
    INFO_PRINT("Kyle C. Hale (c) 2021, Illinois Institute of Technology");
}


double sc_time_stamp() {
    return dut->main_time;
}


static void print_usage (char ** argv) {
    SUGGESTION_PRINT("Usage: " UNBOLD("%s [options]"), argv[0]);
    SUGGESTION_PRINT("Options:");
    SUGGESTION_PRINT("  " UNBOLD("--interactive ") "or " UNBOLD("-i        ")  ": Start the debug shell immediately");
    SUGGESTION_PRINT("  " UNBOLD("--os-image    ") "or " UNBOLD("-o <path> ")  ": Use OS image at " UNBOLD("<path>") ". If no OS image is provided, the provided program will run in supervisor mode.");
    SUGGESTION_PRINT("  " UNBOLD("--binary      ") "or " UNBOLD("-b <path> ")  ": Use the user program image at " UNBOLD("<path>"));
    SUGGESTION_PRINT("  " UNBOLD("--trace       ") "or " UNBOLD("-t <path> ")  ": Output a waveform file at " UNBOLD("<path>"));
    SUGGESTION_PRINT("  " UNBOLD("--haltquit    ") "or " UNBOLD("-q        ")  ": Quit the simulator when the iit3503 halts");
}

static struct option long_options[] = {
	{"interactive", no_argument, 0, 'i'},
	{"binary",      required_argument, 0, 'b'},
	{"trace",       required_argument, 0, 't'},
	{"os-image",    required_argument, 0, 'o'},
	{"help",        no_argument, 0, 'h'},
	{"version",     no_argument, 0, 'V'},
	{"haltquit",    no_argument, 0, 'q'},
	{0, 0, 0, 0}};


typedef struct machine_opts {
    bool interactive;
    bool trace_en;
    char * trace;
    char * image;
    char * os_image;
    bool haltquit;
} machine_opts_t;


static int
parse_args (int argc, 
            char *argv[], 
            machine_opts_t * opts)
{
    if (argc == 1) {
        print_usage(argv);
        exit(EXIT_SUCCESS);
    }

    int retcode = 0;

    while (1) {
        int opt_idx = 0;
        int c = getopt_long(argc, argv, "b:t:hiVqo:", long_options, &opt_idx);

        if (c == -1) {
            break;
        }

        switch (c) {
            case 0:
                break;
            case 'i':
                opts->interactive = true;
                break;
            case 'b':
                opts->image = optarg;
                break;
            case 'o':
                opts->os_image = optarg;
                break;
            case 't':
                opts->trace_en = true;
                opts->trace    = optarg;
                break;
            case 'V':
                print_version();
                exit(0);
            case 'q':
                opts->haltquit = true;
                goto ret;
            case 'h':
                print_usage(argv);
                exit(0);
            case '?':
                break;
            default:
                ERROR_PRINT("Unknown option '%o'", c);
                retcode = -1;
                goto ret;
        }
    }

ret:
    return retcode;
}

static void
print_banner () 
{
    print_version();
    INFO_PRINT("Welcome to the iit3503 interactive debugger.\n\n"
            "Type \"help\" for help.\n"
            "Type ctrl+d or \"quit\" to quit.\n");
}


int 
main (int argc, char **argv)
{
    machine_opts_t opts = {0};

    int ret = parse_args(argc, argv, &opts);
    if (ret) {
        return ret;
    }
    Verilated::commandArgs(argc, argv);

    print_banner();

    dut = iit3503_init(opts.trace_en, opts.haltquit, opts.trace, opts.image, opts.os_image);

    if (!dut) {
        ERROR_PRINT("Could not initialize 3503\n");
        exit(EXIT_FAILURE);
    }

    iit3503_reset(dut);

    cout << "Starting Simulation." << endl;
        
    run_shell(dut, opts.interactive);

    iit3503_deinit(dut);

    return 0;
}
