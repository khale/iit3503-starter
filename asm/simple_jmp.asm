;; should loop forever just like BRnzp
.ORIG x0000
    ADD R0, R0, #1
    LEA R0, FOO
FOO
    JMP R0
.END
