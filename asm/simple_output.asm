; tests keyboard output (1 char)
.ORIG x0000
TRYOUT
    LD R1, THECHAR
    LDI R0, DSR
    BRzp TRYOUT
    STI R1, DDR
DONE
    BRnzp DONE

THECHAR
    .FILL x0041 ;; 'A'
DSR
    .FILL xFE04
DDR
    .FILL xFE06

.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary
.FILL xFFFF ; IR canary

.END
