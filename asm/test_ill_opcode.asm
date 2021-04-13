.ORIG x3000
    AND R0, R0, #0
    ADD R0, R0, #1

BAD_INSTR .FILL xD000 ; restricted opcode (1101)

DONE
    BRnzp DONE ; shouldn't get here

.END
