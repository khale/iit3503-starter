.ORIG x3000
    LD R0, STUFF
    LD R1, MORESTUFF
    ADD R3, R0, R1
    HALT
DONE            ; we don't get here
    BRnzp DONE

STUFF
    .FILL x1
MORESTUFF
    .FILL x2
.END
