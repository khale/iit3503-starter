; test STI
.ORIG x0000
    LD R0, VAL ; R0 <- xDEAD
    STI R0, BIGADDR ; x3000 should have xDEAD
DONE
    BRnzp DONE
BIGADDR
    .FILL x3000
VAL
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

.END
