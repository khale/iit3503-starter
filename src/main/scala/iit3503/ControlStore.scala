package iit3503
import chisel3._

import iit3503._

/*
 * This implements the microcode ROM for the iit3503. 
 * Note that it is just a lookup table, where each entry
 * contains the values for all the 52 control signals at a 
 * particular clock cycle (i.e. in a particular state of the FSM).
 *
 * You won't normally see a microcode ROM constructed this way. Usually
 * it will be represented as a big array of binary data. That's not very 
 * easy to read for humans though, so for educational purposes we've
 * specified each row of the microcode ROM very explicitly. All signals are
 * initialized to zero and only set to other values if specified below.
 *
 * In either case, a microcode ROM is a nice flexible way to implement
 * a control because we can update it easily if there are bugs, or if
 * we want to add new instructions to our machine. We just have to
 * update the data store din the microcode ROM! If you've ever installed
 * a "microcode update" for your machine, this is what you were changing.
 */

/*
 * From P&P microcode ROM specification
 * P&P App C, Fig C.9, pp 720
 *
 */
class CtrlSigs extends Bundle {
  val LDMAR      = Bool()
  val LDMDR      = Bool()
  val LDIR       = Bool()
  val LDBEN      = Bool()
  val LDREG      = Bool()
  val LDCC       = Bool()
  val LDPC       = Bool()
  val LDPriv     = Bool()
  val LDSavedSSP = Bool()
  val LDSavedUSP = Bool()
  val LDVector   = Bool()
  val LDPriority = Bool()
  val LDACV      = Bool()
  val GatePC     = Bool()
  val GateMDR    = Bool()
  val GateALU    = Bool()
  val GateMARMUX = Bool()
  val GateVector = Bool()
  val GatePCm1   = Bool()
  val GatePSR    = Bool()
  val GateSP     = Bool()
  val PCMUX      = UInt(2.W)
  val DRMUX      = UInt(2.W)
  val SR1MUX     = UInt(2.W)
  val ADDR1MUX   = Bool()
  val ADDR2MUX   = UInt(2.W)
  val SPMUX      = UInt(2.W)
  val MARMUX     = Bool()
  val TableMUX   = Bool()
  val VectorMUX  = UInt(2.W)
  val PSRMUX     = Bool()
  val ALUK       = UInt(2.W)
  val MIOEN      = Bool()
  val RW         = Bool()
  val SetPriv    = Bool()
}

/*
 * From Table C.2
 *
 * COND/3
 *   - COND0 : unconditional
 *   - COND1 : mem ready
 *   - COND2 : branch
 *   - COND3 : addr mode
 *   - COND4 : priv mode
 *   - COND5 : interrupt test
 *   - COND6 : ACV test
 */
class CtrlFeedback extends Bundle {
  val IRD  = Bool()
  val COND = UInt(3.W)
  val J    = UInt(6.W)
}

// see P&P Fig C.9, pp. 720
class MicroInstr extends Bundle {
  val feed = new CtrlFeedback
  val sigs = new CtrlSigs
}

class ControlStore extends Module {

  // the control unit accepts an address (state number)
  // as input, and outputs the row of control signals
  // for that state
  val io = IO(new Bundle {
    val addr = Input(UInt(6.W))
    val out  = Output(new MicroInstr)
  })

  // we'll fill in each individual entry manually 
  val cStore = VecInit(Seq.fill(64)(0.U.asTypeOf(new MicroInstr)))

  // each of the below blocks correspond to one row of the
  // microcode ROM. We only set the signals that will be
  // used for that state. 
  // Note that the "feedback" signals are used for states
  // in the FSM that have more than one outgoing edge (i.e., when
  // there is a "choice") between next states. These "choices" depend
  // on external conditions. The "COND" field of the row corresponds
  // to "which external condition are we checking for." The "J" field
  // specifies the next state that we'll go to *if that condition is not met*. 
  // It can be seen as the "default next state."  Take a look at the microsequencer
  // figure in the book (Fig C.5) to see how this works.
  

  // Note that the "IRD" line of the microsequencer is *only* used
  // in the DECODE stage of instruction processing (state 32). This state
  // is special because the next state entirely depends on the opcode of the
  // fetched instruction. Thus, IRD is only set on row 32.
  

  // 00 = BR - check for branch enable
  val c00        = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(0)     := c00

  // 01 = ADD
  val c01           = 0.U.asTypeOf(new MicroInstr)
  c01.feed.J       := 18.U   // our next state will be back to FETCH
  c01.sigs.ALUK    := 0.U    // we tell our ALU we want the "ADD" op
  c01.sigs.LDCC    := true.B // ADD should set condition codes
  c01.sigs.PSRMUX  := true.B // load PSR from logic (based on bus output)
  c01.sigs.GateALU := true.B // ALU output goes out on (drives) the bus
  c01.sigs.SR1MUX  := 1.U    // we get our source reg from IR[8..6]
  c01.sigs.LDREG   := true.B // we'll be writing a value to the register file (note that *which* reg we write is determined by logic in the datapath, see DRMUX there)
  cStore(1)        := c01    // set the row to the signals. We do it this way because it would be tedious to write something like cstore(1) = "b00000000100101010110000000000000000000100101010"...imagine the bugs

  // 02 = LD (direct mem read step 1) 
  val c02              = 0.U.asTypeOf(new MicroInstr)
  c02.feed.J          := 35.U
  c02.sigs.GateMARMUX := true.B // marmux drives bus
  c02.sigs.ADDR1MUX   := false.B // PC + 1
  c02.sigs.ADDR2MUX   := 2.U // SEXT(IR[8..0]) (PCoffset9)
  c02.sigs.MARMUX     := true.B
  c02.sigs.LDACV      := true.B // set ACV
  c02.sigs.LDMAR      := true.B
  cStore(2)           := c02

  // 03 = ST (direct mem write step 1) - load MAR
  val c03              = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(3)           := c03
  
  // 04 = JSR (step 1) - check for JSR or JSRR
  val c04        = 0.U.asTypeOf(new MicroInstr)
  c04.feed.J    := 20.U
  c04.feed.COND := "b011".U // IR[11] check
  cStore(4)     := c04
  
  // 05 = AND
  val c05           = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(5)        := c05

  // 06 = LDR (direct mem read step 1)
  val c06              = 0.U.asTypeOf(new MicroInstr)
  c06.feed.J          := 35.U
  c06.sigs.LDMAR      := true.B
  c06.sigs.LDACV      := true.B
  c06.sigs.GateMARMUX := true.B // marmux drives bus
  c06.sigs.MARMUX     := true.B
  c06.sigs.ADDR1MUX   := true.B // SR1 Out
  c06.sigs.ADDR2MUX   := 1.U // SEXT(IR[5..0]) (offset6)
  c06.sigs.SR1MUX     := 1.U // IR[8..6] (baseR)
  cStore(6)           := c06

  // 07 = STR 
  val c07              = 0.U.asTypeOf(new MicroInstr)
  c07.feed.J          := 23.U
  c07.sigs.LDACV      := true.B
  c07.sigs.LDMAR      := true.B
  c07.sigs.GateMARMUX := true.B
  c07.sigs.MARMUX     := true.B // addr calc
  c07.sigs.ADDR1MUX   := true.B // baseR (SR1 Out)
  c07.sigs.ADDR2MUX   := 1.U // SEXT(IR[5..0]) (offset6)
  c07.sigs.SR1MUX     := 1.U // IR[8..6] (baseR)
  cStore(7)           := c07

  // 08 = RTI (step 1) - load MAR
  val c08              = 0.U.asTypeOf(new MicroInstr)
  c08.feed.J          := 36.U
  c08.feed.COND       := "b100".U // PSR[15] check
  c08.sigs.LDMAR      := true.B
  c08.sigs.SR1MUX     := 2.U // R6 (SP)
  c08.sigs.ADDR1MUX   := true.B // SR1 Out
  c08.sigs.ADDR2MUX   := 0.U // 0
  c08.sigs.GateMARMUX := true.B
  c08.sigs.MARMUX     := true.B
  cStore(8)           := c08

  // 09 = NOT
  val c09           = 0.U.asTypeOf(new MicroInstr)
  c09.feed.J       := 18.U // back to fetch
  c09.sigs.LDCC    := true.B
  c09.sigs.ALUK    := 2.U // NOT
  c09.sigs.PSRMUX  := true.B // load PSR (CC) from logic
  c09.sigs.GateALU := true.B
  c09.sigs.SR1MUX  := 1.U // IR[8..6]
  c09.sigs.LDREG   := true.B
  cStore(9)        := c09

  // 10 = LDI (indirect read step 1)
  val c10              = 0.U.asTypeOf(new MicroInstr)
  c10.feed.J          := 17.U
  c10.sigs.LDACV      := true.B
  c10.sigs.LDMAR      := true.B
  c10.sigs.GateMARMUX := true.B
  c10.sigs.MARMUX     := true.B
  c10.sigs.ADDR1MUX   := false.B // PC + 1
  c10.sigs.ADDR2MUX   := 2.U // SEXT(IR[8..0]) (PCoffset9)
  cStore(10)          := c10

  // 11 = STI (indirect mem write step 1) - load MAR
  val c11              = 0.U.asTypeOf(new MicroInstr)
  c11.feed.J          := 19.U
  c11.sigs.LDMAR      := true.B
  c11.sigs.LDACV      := true.B
  c11.sigs.GateMARMUX := true.B
  c11.sigs.MARMUX     := true.B // addr calc
  c11.sigs.ADDR1MUX   := false.B // PC + 1
  c11.sigs.ADDR2MUX   := 2.U // SEXT(IR[8..0]) (PCoffset9)
  cStore(11)          := c11

  // 12 = JMP
  val c12            = 0.U.asTypeOf(new MicroInstr)
  c12.feed.J        := 18.U // back to ifetch
  c12.sigs.LDPC     := true.B
  c12.sigs.PCMUX    := 1.U // SR1 + 0
  c12.sigs.SR1MUX   := 1.U // IR[8..6] (baseR)
  c12.sigs.ADDR1MUX := true.B // SR1 Out
  c12.sigs.ADDR2MUX := 0.U // 0
  cStore(12)        := c12

  // 13 = Undefined Opcode
  val c13           = 0.U.asTypeOf(new MicroInstr)
  c13.feed.J       := 62.U
  c13.feed.COND    := "b011".U
  c13.sigs.LDPriv  := true.B
  c13.sigs.PSRMUX  := true.B
  c13.sigs.SetPriv := false.B
  cStore(13)       := c13

  // 14 = LEA (no mem access)
  val c14              = 0.U.asTypeOf(new MicroInstr)
  c14.feed.J          := 18.U // back to ifetch
  c14.sigs.LDREG      := true.B
  c14.sigs.ADDR1MUX   := false.B // PC + 1
  c14.sigs.ADDR2MUX   := 2.U // SEXT(IR[8..0]) (PCoffset9)
  c14.sigs.MARMUX     := true.B
  c14.sigs.GateMARMUX := true.B // MARMux drives bus
  cStore(14)          := c14

  // 15 = TRAP (step 1)
  val c15            = 0.U.asTypeOf(new MicroInstr)
  c15.feed.J        := 47.U
  c15.sigs.LDMDR    := true.B
  c15.sigs.GatePSR  := true.B
  c15.sigs.LDPC     := true.B
  c15.sigs.PCMUX    := false.B // PC + 1
  c15.sigs.TableMUX := true.B // x00
  c15.sigs.LDVector := true.B // KCH: Vector reg will be loaded unnecessarily
  cStore(15)        := c15

  // 16 = STx (direct mem write step 3) - wait for mem. write completion
  val c16         = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(16)     := c16

  // 17 = LDI (indirect mem read step 2) - indirect access violation check
  val c17        = 0.U.asTypeOf(new MicroInstr)
  c17.feed.COND := "b110".U // check ACV
  c17.feed.J    := 24.U // back to ifetch
  cStore(17)    := c17

  // 18 = IFETCH (step 1) - begin instruction fetch
  val c18          = 0.U.asTypeOf(new MicroInstr)
  c18.feed.J      := 33.U
  c18.feed.COND   := "b101".U // check for IRQ
  c18.sigs.LDMAR  := true.B
  c18.sigs.GatePC := true.B
  c18.sigs.LDPC   := true.B
  c18.sigs.PCMUX  := 0.U // PC <- PC + 1
  c18.sigs.LDACV  := true.B // set ACV
  cStore(18)      := c18

  // 19 = STI (indirect mem write step 2) - indirect access violation check
  val c19        = 0.U.asTypeOf(new MicroInstr)
  c19.feed.J    := 29.U
  c19.feed.COND := "b110".U // check ACV
  cStore(19)    := c19

  // 20 = JSRR (step 2) - perform jump w/base reg
  val c20            = 0.U.asTypeOf(new MicroInstr)
  c20.feed.J        := 18.U // back to ifetch
  c20.sigs.GatePC   := true.B
  c20.sigs.LDPC     := true.B
  c20.sigs.PCMUX    := 1.U // new PC comes from addr calc
  c20.sigs.LDREG    := true.B
  c20.sigs.DRMUX    := 1.U // writing to R7
  c20.sigs.SR1MUX   := 1.U // IR[8..6] (baseR)
  c20.sigs.ADDR1MUX := true.B // SR 1 Out
  c20.sigs.ADDR2MUX := 0.U // SR1 + 0
  cStore(20)        := c20

  // 21 = JSR (step 2) - perform jump
  val c21            = 0.U.asTypeOf(new MicroInstr)
  c21.feed.J        := 18.U // back to ifetch
  c21.sigs.GatePC   := true.B
  c21.sigs.LDREG    := true.B
  c21.sigs.DRMUX    := 1.U // R7
  c20.sigs.PCMUX    := 1.U // new PC comes from addr calc
  c21.sigs.LDPC     := true.B
  c21.sigs.ADDR1MUX := false.B // PC + 1
  c21.sigs.ADDR2MUX := 3.U // SEXT(IR[10..0]) (PCoffset11)
  cStore(21)        := c21

  // 22 = BR (step 2) - load PC with branch target
  val c22            = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(22)        := c22

  // 23 = STx (direct mem write step 2) - load MDR, ACV check
  val c23           = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(23)       := c23

  // 24 = LDI (indirect mem read step 3) - wait for memory
  val c24         = 0.U.asTypeOf(new MicroInstr)
  c24.feed.J     := 24.U
  c24.feed.COND  := "b001".U // wait for R
  c24.sigs.LDMDR := true.B
  c24.sigs.MIOEN := true.B
  c24.sigs.RW    := false.B // read
  cStore(24)     := c24

  // 25 = LDx (direct mem read step 3) - wait for memory
  val c25         = 0.U.asTypeOf(new MicroInstr)
  c25.feed.J     := 25.U
  c25.feed.COND  := "b001".U // wait for R
  c25.sigs.LDMDR := true.B
  c25.sigs.MIOEN := true.B
  c25.sigs.RW    := false.B // read
  cStore(25)     := c25

  // 26 = LDI (indirect mem read step 4) - indirect addr load
  // after this point, LDI shares state transitions with LDR and LD
  val c26           = 0.U.asTypeOf(new MicroInstr)
  c26.feed.J       := 35.U
  c26.sigs.GateMDR := true.B // MDR drives bus
  c26.sigs.LDMAR   := true.B
  c26.sigs.LDACV   := true.B // set ACV
  cStore(26)       := c26

  // 27 = LDx (direct mem read step 4) - transfer to regs, set CC
  val c27           = 0.U.asTypeOf(new MicroInstr)
  c27.feed.J       := 18.U
  c27.sigs.LDCC    := true.B
  c27.sigs.GateMDR := true.B // MDR drives bus
  c27.sigs.PSRMUX  := true.B // load PSR (CC) from logic
  c27.sigs.LDREG   := true.B
  cStore(27)       := c27

  // 28 = IFETCH (step 3) - read instruction from mem
  val c28         = 0.U.asTypeOf(new MicroInstr)
  c28.feed.J     := 28.U
  c28.feed.COND  := "b001".U // wait for R
  c28.sigs.LDMDR := true.B
  c28.sigs.MIOEN := true.B
  c28.sigs.RW    := false.B // read
  cStore(28)     := c28

  // 29 = STI (indirect mem write step 3) - wait for memory
  val c29         = 0.U.asTypeOf(new MicroInstr)
  c29.feed.J     := 29.U
  c29.feed.COND  := "b001".U // wait for R
  c29.sigs.LDMDR := true.B
  c29.sigs.MIOEN := true.B
  c29.sigs.RW    := false.B // indirect write via read
  cStore(29)     := c29

  // 30 = IFETCH (step 4) - load instruction into IR
  val c30           = 0.U.asTypeOf(new MicroInstr)
  // FILL ME IN!
  cStore(30)       := c30

  // 31 = STI (indirect mem write step 4) - indirect addr load
  val c31           = 0.U.asTypeOf(new MicroInstr)
  c31.feed.J       := 23.U
  c31.sigs.LDACV   := true.B
  c31.sigs.LDMAR   := true.B
  c31.sigs.GateMDR := true.B
  cStore(31)       := c31

  // 32 = DECODE 
  val c32         = 0.U.asTypeOf(new MicroInstr)
  c32.feed.IRD   := true.B // only enabled in decode!
  c32.sigs.LDBEN := true.B // set up CCs
  cStore(32)     := c32

  // 33 = IFETCH (step 2) - ifetch access violation check
  val c33        = 0.U.asTypeOf(new MicroInstr)
  c33.feed.J    := 28.U
  c33.feed.COND := "b110".U // check ACV
  cStore(33)    := c33

  // 34 = RTI (step 7) - pop stack, check for stack switch 
  val c34          = 0.U.asTypeOf(new MicroInstr)
  c34.feed.J      := 51.U
  c34.feed.COND   := "b100".U // PSR[15] check
  c34.sigs.GateSP := true.B
  c34.sigs.SPMUX  := 0.U // SP + 1
  c34.sigs.SR1MUX := 2.U // read SP
  c34.sigs.LDREG  := true.B // writing SP
  c34.sigs.DRMUX  := 2.U // select SP
  cStore(34)     := c34

  // 35 = LDx (direct mem read step 2) - direct access violation check
  val c35        = 0.U.asTypeOf(new MicroInstr)
  c35.feed.J    := 25.U // next state if no ACV
  c35.feed.COND := "b110".U // ACV check
  cStore(35)    := c35
  
  // 36 = RTI (step 2a) - read old PC from top of stack
  val c36         = 0.U.asTypeOf(new MicroInstr)
  c36.feed.J     := 36.U
  c36.feed.COND  := "b001".U // wait for R
  c36.sigs.LDMDR := true.B
  c36.sigs.MIOEN := true.B
  c36.sigs.RW    := false.B // read
  cStore(36)     := c36

  // 37 = INT (step 2a) - bump stack pointer
  val c37          = 0.U.asTypeOf(new MicroInstr)
  c37.feed.J      := 41.U
  c37.sigs.LDMAR  := true.B
  c37.sigs.GateSP := true.B
  c37.sigs.LDREG  := true.B
  c37.sigs.DRMUX  := 2.U // writing SP
  c37.sigs.SPMUX  := 1.U // SP - 1
  c37.sigs.SR1MUX := 2.U // read SP
  cStore(37)      := c37

  // 38 = RTI (step 3) - set PC to old PC
  val c38           = 0.U.asTypeOf(new MicroInstr)
  c38.feed.J       := 39.U
  c38.sigs.LDPC    := true.B
  c38.sigs.PCMUX   := 2.U // get MDR from bus
  c38.sigs.GateMDR := true.B // MDR is output
  cStore(38)       := c38

  // 39 = RTI (step 4) - pop stack
  val c39          = 0.U.asTypeOf(new MicroInstr)
  c39.feed.J      := 40.U
  c39.sigs.LDMAR  := true.B
  c39.sigs.GateSP := true.B
  c39.sigs.SPMUX  := 0.U // SP + 1
  c39.sigs.SR1MUX := 2.U // SP
  c39.sigs.LDREG  := true.B // writing SP
  c39.sigs.DRMUX  := 2.U // select SP
  cStore(39)      := c39

  // 40 = RTI (step 5) - read PSR from top of stack
  val c40         = 0.U.asTypeOf(new MicroInstr)
  c40.feed.J     := 40.U
  c40.feed.COND  := "b001".U // wait for R
  c40.sigs.LDMDR := true.B
  c40.sigs.MIOEN := true.B
  c40.sigs.RW    := false.B //read
  cStore(40)     := c40

  // 41 = INT (step 3) - push old PSR on stack
  val c41         = 0.U.asTypeOf(new MicroInstr)
  c41.feed.J     := 41.U
  c41.feed.COND  := "b001".U // wait for R
  c41.sigs.MIOEN := true.B
  c41.sigs.RW    := true.B // write
  cStore(41)     := c41

  // 42 = RTI (step 6) - restore PSR to old PSR
  val c42              = 0.U.asTypeOf(new MicroInstr)
  c42.feed.J          := 34.U
  c42.sigs.LDPriv     := true.B
  c42.sigs.LDCC       := true.B
  c42.sigs.LDPriority := true.B
  c42.sigs.PSRMUX     := false.B // PSR loaded from bus
  c42.sigs.GateMDR    := true.B
  cStore(42)          := c42

  // 43 = INT (step 4) - save old PC
  val c43            = 0.U.asTypeOf(new MicroInstr)
  c43.feed.J        := 46.U
  c43.sigs.LDMDR    := true.B
  c43.sigs.GatePCm1 := true.B
  cStore(43)        := c43

  // 44 = RTI (step 2b) - RTI attempt from user mode
  val c44             = 0.U.asTypeOf(new MicroInstr)
  c44.feed.J         := 45.U
  c44.sigs.LDPriv    := true.B
  c44.sigs.PSRMUX    := true.B // load Priv from control unit
  c44.sigs.SetPriv   := false.B // Priv <- 0
  c44.sigs.LDMDR     := true.B
  c44.sigs.GatePSR   := true.B
  c44.sigs.LDVector  := true.B
  c44.sigs.TableMUX  := false.B // x01
  c44.sigs.VectorMUX := 1.U // x00
  cStore(44)         := c44

  // 45 = INT (step 2b) - switch to kernel stack if necessary
  // KCH NOTE: this state is actually shown incorrectly in P&P pp 713 (load orders are swapped)
  val c45              = 0.U.asTypeOf(new MicroInstr)
  c45.feed.J          := 37.U
  c45.sigs.SR1MUX     := 2.U // read current SP
  c45.sigs.LDSavedUSP := true.B // store it in saved USP
  c45.sigs.LDREG      := true.B // writing SP
  c45.sigs.DRMUX      := 2.U // select SP to write
  c45.sigs.SPMUX      := 2.U // SavedSSP
  c45.sigs.GateSP     := true.B // curr SP goes out on bus
  cStore(45)          := c45

  // 46 = INT (step 5) - prep MAR for write of old PC
  val c46          = 0.U.asTypeOf(new MicroInstr)
  c46.feed.J      := 52.U
  c46.sigs.LDMAR  := true.B
  c46.sigs.LDREG  := true.B
  c46.sigs.DRMUX  := 2.U // write to SP
  c46.sigs.SR1MUX := 2.U // read form SP
  c46.sigs.GateSP := true.B
  c46.sigs.SPMUX  := 1.U // SP - 1
  cStore(46)      := c46

  // 47 = TRAP (step 2)
  val c47              = 0.U.asTypeOf(new MicroInstr)
  c47.feed.J          := 37.U
  c47.feed.COND       := "b100".U // PSR[15] check
  c47.sigs.LDPriv     := true.B
  c47.sigs.PSRMUX     := true.B // priv comes from control
  c47.sigs.SetPriv    := false.B // Priv <- 0
  c47.sigs.LDVector   := true.B
  c47.sigs.TableMUX   := true.B
  c47.sigs.GateMARMUX := true.B // IR goes out on bus, provides vector
  c47.sigs.MARMUX     := false.B // ZEXT(IR[7..0]) (trap vector)
  cStore(47)          := c47

  // 48 = ACV occurred
  val c48             = 0.U.asTypeOf(new MicroInstr)
  c48.feed.J         := 45.U
  c48.sigs.GatePSR   := true.B
  c48.sigs.LDMDR     := true.B
  c48.sigs.LDVector  := true.B
  c48.sigs.LDPriv    := true.B
  c48.sigs.TableMUX  := false.B // x01
  c48.sigs.VectorMUX := 3.U // x02
  c48.sigs.PSRMUX    := true.B // psr loaded from control
  c48.sigs.SetPriv   := false.B // Priv <- 0
  cStore(48)         := c48

  // 49 = INT (step 1) - load IRQ vector
  val c49              = 0.U.asTypeOf(new MicroInstr)
  c49.feed.J          := 37.U
  c49.feed.COND       := "b100".U // PSR[15] check
  c49.sigs.LDVector   := true.B
  c49.sigs.VectorMUX  := 0.U // INTV
  c49.sigs.TableMUX   := false.B // x01
  c49.sigs.LDPriority := true.B
  c49.sigs.PSRMUX     := true.B // device provides IRQ priority
  c49.sigs.LDMDR      := true.B
  c49.sigs.GatePSR    := true.B // MDR <- PSR
  c49.sigs.LDPriv     := true.B
  c49.sigs.SetPriv    := false.B // control provides new priv (Priv <- 0)
  cStore(49)          := c49

  // 50 = Unused
  
  // 51 = RTI (step 8a) - Nothing
  val c51     = 0.U.asTypeOf(new MicroInstr)
  c51.feed.J := 18.U // back to ifetch
  cStore(51) := c51

  // 52 = INT (step 6) - complete push of old PC
  val c52         = 0.U.asTypeOf(new MicroInstr)
  c52.feed.J     := 52.U
  c52.feed.COND  := "b001".U // wait for R
  c52.sigs.MIOEN := true.B
  c52.sigs.RW    := true.B // write
  cStore(52)     := c52

  // 53 = INT (step 8) - read TRAP table entry
  val c53         = 0.U.asTypeOf(new MicroInstr)
  c53.feed.J     := 53.U
  c53.feed.COND  := "b001".U // wait for R
  c53.sigs.LDMDR := true.B
  c53.sigs.MIOEN := true.B
  c53.sigs.RW    := false.B // read
  cStore(53)     := c53

  // 54 = INT (step 7) - prepare TRAP vector table entry
  val c54              = 0.U.asTypeOf(new MicroInstr)
  c54.feed.J          := 53.U
  c54.sigs.LDMAR      := true.B
  c54.sigs.GateVector := true.B
  cStore(54)          := c54

  // 55 = INT (step 9) - control transfer to IRQ handler
  val c55             = 0.U.asTypeOf(new MicroInstr)
  c55.feed.J := 18.U // back to ifetch
  c55.sigs.LDPC := true.B
  c55.sigs.GateMDR := true.B
  c55.sigs.PCMUX := 2.U // PC <- bus (driven by MDR)
  cStore(55) := c55

  // 56 = ACV occurred
  cStore(56) := c48

  // 57 = ACV occurred
  cStore(57) := c48

  // 58 = Unused

  // 59 = RTI (step 8b) - restore saved user SP
  val c59              = 0.U.asTypeOf(new MicroInstr)
  c59.feed.J          := 18.U // back to ifetch
  c59.sigs.LDSavedSSP := true.B
  c59.sigs.SR1MUX     := 2.U // read SP
  c59.sigs.LDREG      := true.B // writing SP
  c59.sigs.DRMUX      := 2.U // select SP for write
  c59.sigs.SPMUX      := 3.U // send saved USP to bus
  c59.sigs.GateSP     := true.B
  cStore(59)          := c59

  // 60 = ACV occurred
  cStore(60) := c48 

  // 61 = ACV occurred
  cStore(61) := c48

  // 62 = ??? 
  val c62             = 0.U.asTypeOf(new MicroInstr)
  c62.feed.J         := 37.U
  c62.feed.COND      := "b100".U // check PSR[15]
  c62.sigs.LDVector  := true.B
  c62.sigs.TableMUX  := false.B // x01
  c62.sigs.VectorMUX := 2.U // x01
  c62.sigs.LDPriv    := true.B
  c62.sigs.SetPriv   := false.B // Priv <- 0
  c62.sigs.PSRMUX    := true.B // set PSR[15] from control
  cStore(62)         := c62

  // 63 = ???
  val c63           = 0.U.asTypeOf(new MicroInstr)
  c63.feed.J       := 18.U
  c63.sigs.LDPriv  := true.B
  c63.sigs.SetPriv := false.B
  c63.sigs.PSRMUX  := true.B
  cStore(63)       := c63

  // output <- ucodeROM(statenum)
  io.out := cStore(io.addr)
}
