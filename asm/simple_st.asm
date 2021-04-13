.ORIG x0000
    AND R0, R0, #0
    ADD R0, R0, #2
    ST R0, x0018
DONE
    BRnzp DONE
.END
