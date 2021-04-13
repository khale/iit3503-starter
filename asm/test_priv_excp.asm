.ORIG x3000
    AND R0, R0, #0
    RTI            ; not allowed to execute RTI in user mode
DONE
    BRnzp DONE     ; should not get here

.END
