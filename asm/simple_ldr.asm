; tests LDR
.ORIG x0000
    LEA R0, FOO
    LDR R1, R0, #1 ; should load R1 with xF00D
DONE
    BRnzp DONE
FOO
    .FILL xFEED
BAR
    .FILL xF00D
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
