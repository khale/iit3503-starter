;;
;; simple hello world program that requires no OS
;; used mostly for testing FPGA platform
;;
.ORIG x0000

START
    LEA R0, HELLO

LOOP
    LDR R1, R0, #0
    BRz DONE
    
DISPLAYWAIT
    LDI R2, DSR
    BRzp DISPLAYWAIT
    STI R1, DDR

    ADD R0, R0, #1
    BRnzp LOOP

DONE
    LD R0, COUNT
LOOPCOUNT
    BRz START
    ADD R0, R0, #-1
    BRnzp LOOPCOUNT

HELLO .STRINGZ "Hello, World!\n"
DSR .FILL xFE04
DDR .FILL xFE06

COUNT .FILL xFFFF

.END
