; tests keyboard output (full string)
.ORIG x0000

    LEA R3, THESTR
LOOP
    LDR R0, R3, #0
    BRz DONE
DISPLAYWAIT
    LDI R1, DSR
    BRzp DISPLAYWAIT
    STI R0, DDR
    ADD R3, R3, #1
    BRnzp LOOP
DONE
    BRnzp DONE

THESTR
    .STRINGZ "Hello, World!\n"
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
