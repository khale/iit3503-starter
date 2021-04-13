.ORIG x0000
    AND R0, R0, #0
    ADD R0, R0, #1
FOO
    BRnzp FOO
.END
