; test BRn
.ORIG x0000
    AND R0, R0, #0
    ADD R0, R0, #2
TEST
    BRn DONE
    ADD R0, R0, #-1
    BRnzp TEST
DONE
    BRnzp DONE

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
