; test LDI (depends on STI)
.ORIG x0000
    LD R0, BAR ; R0 <- xDEAD
    STI R0, FOO ; Mem[x5050] <- xDEAD
    LDI R5, FOO ; R5 <- xDEAD
DONE
    BRnzp DONE
FOO
    .FILL x5050
BAR 
    .FILL xDEAD

.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary
.END
