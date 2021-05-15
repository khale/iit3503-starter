;;
;; Tests AND
;;
.ORIG x0000
    AND R0, R0, #0
    ADD R0, R0, #7
    AND R1, R1, #0
    ADD R1, R1, #2
    AND R3, R0, R1 ; should produce 2 in R3
FOO
    BRnzp FOO
.END
