;
; tests outputting a single character 
; to the console. Mostly for testing the FPGA
; platform
.ORIG x0000
    LD R0, HELLO
    STI R0, DDR
DONE
    BRnzp DONE

DDR .FILL xFE06
HELLO .STRINGZ "f"
.END
