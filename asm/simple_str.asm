; test STR
.ORIG x0000
    LEA R0, FOO     ;; R0 <- base address
    LD R3, BAZ      ;; R3 <- xF00D
    STR R3, R0, #1  ;; BAR should now hold xF00D too
DONE
    BRnzp DONE
    
FOO
    .FILL xDEAD
BAR
    .FILL xBEEF
BAZ
    .FILL xF00D


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
.FILL xEEEE ; IR canary
.FILL xEEEE ; IR canary

.END
