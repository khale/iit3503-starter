.ORIG x3000
    LD R0, STUFF
    LD R1, MORESTUFF
    ADD R3, R0, R1
    LDI R0, KERNEL_ADDR ;; protected access to IO space, should fail with ACV
DONE                    ;; shouldn't get here
    BRnzp DONE

STUFF
    .FILL x1
MORESTUFF
    .FILL x2
KERNEL_ADDR
    .FILL xFE06
.END
