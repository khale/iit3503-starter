;; test jsrr
.ORIG x0000
    ADD R0, R0, #2
    ADD R1, R1, #5
    LEA R4, ADDIT
    JSRR R4
DONE
    BRnzp DONE
ADDIT
    ADD R3, R0, R1
    RET

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
