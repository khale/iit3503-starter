package iit3503

import chisel3._
import chisel3.util._


class ProcessorStatus extends Bundle {
  val priv     = Bool()
  val unused1  = UInt(4.W)
  val priority = UInt(3.W)
  val unused2  = UInt(5.W)
  val cc       = UInt(3.W) // N, Z, P
}

/*
 * 3503 Datapath
 *
 * Based on P&P App. C, Figs C.3, C.6, C.8, etc.
 *
 */

class DataPath extends Module {

  val io = IO(new Bundle {
    val intPriority    = Input(UInt(3.W))
    val ctrlSigs       = Input(new CtrlSigs)
    val mdrVal         = Input(UInt(16.W))
    val devIntEnable   = Input(Bool())
    val intHandlerAddr = Input(UInt(16.W))
    val resetVec       = Input(UInt(16.W))

    val ir    = Output(UInt(16.W))
    val bus   = Output(UInt(16.W))
    val psr15 = Output(Bool())
    val n     = Output(Bool())
    val z     = Output(Bool())
    val p     = Output(Bool())
    val bEn   = Output(Bool())
    val ACV   = Output(Bool())
    val irq   = Output(Bool())

    /* DEBUG OUTPUTS */
   val debugPC  = Output(UInt(16.W))
   val debugIR  = Output(UInt(16.W))
   val debugPSR = Output(UInt(16.W))
   val debugR0  = Output(UInt(16.W))
   val debugR1  = Output(UInt(16.W))
   val debugR2  = Output(UInt(16.W))
   val debugR3  = Output(UInt(16.W))
   val debugR4  = Output(UInt(16.W))
   val debugR5  = Output(UInt(16.W))
   val debugR6  = Output(UInt(16.W))
   val debugR7  = Output(UInt(16.W))

  })

  val ctrl = io.ctrlSigs

  val ACV = RegInit(false.B)
  val BEN = RegInit(false.B)

  val IR  = RegInit(0.U(16.W))
  val PSR = RegInit(2.U.asTypeOf(new ProcessorStatus)) // Z set to true on reset

  val PC  = RegInit(io.resetVec)

  val N = PSR.cc(2)
  val Z = PSR.cc(1)
  val P = PSR.cc(0)

  val SavedSSP = RegInit(0.U(16.W))
  val SavedUSP = RegInit("hfdff".U(16.W))

  val ALU  = Module(new ALU)
  val regs = Module(new RegFile)

  // for interrupt priority
  val aGbReg = RegInit(false.B)

  // wire up outputs to the control path
  io.ir    := IR
  io.psr15 := PSR.priv
  io.n     := N
  io.z     := Z
  io.p     := P
  io.bEn   := BEN
  io.ACV   := ACV

  /*========== BUS SETUP ==============*/
  val bus    = Module(new Bus)
  val busOut = bus.io.output

  io.bus := busOut

  // who gets the bus is determined by
  // the GateX control lines
  bus.io.inputSel := Cat(Seq(
    ctrl.GatePC,
    ctrl.GateMDR,
    ctrl.GateALU,
    ctrl.GateMARMUX,
    ctrl.GateVector,
    ctrl.GatePCm1,
    ctrl.GatePSR,
    ctrl.GateSP
  ))


  val MARMUX = Wire(UInt(16.W))
  val SPMUX  = Wire(UInt(16.W))

  // we have to actually wire up the components
  // to the vector of bus inputs
  bus.io.inputs(7) := PC
  bus.io.inputs(6) := io.mdrVal
  bus.io.inputs(5) := ALU.io.out
  bus.io.inputs(4) := MARMUX
  bus.io.inputs(3) := io.intHandlerAddr
  bus.io.inputs(2) := PC - 1.U
  bus.io.inputs(1) := PSR.asUInt()
  bus.io.inputs(0) := SPMUX

  /* !========= BUS SETUP =============! */

  /*========== MUX SETUP ==============*/

 // TODO: FILL ME IN!
  val ADDR1MUX = 0.U(16.W)

  // TODO: FILL ME IN!
  val ADDR2MUX = 0.U(16.W)

  val addrCalc = ADDR1MUX + ADDR2MUX

  val PCMUX = MuxLookup(ctrl.PCMUX, (PC + 1.U), Seq(
    0.U -> (PC + 1.U),
    1.U -> addrCalc,
    2.U -> busOut
  ))

  SPMUX := MuxLookup(ctrl.SPMUX, regs.io.sr1Out, Seq(
    0.U -> (regs.io.sr1Out + 1.U),
    1.U -> (regs.io.sr1Out - 1.U),
    2.U -> SavedSSP,
    3.U -> SavedUSP
  ))

  // TODO: FILL ME IN!
  MARMUX := 0.U

  // see Fig C.6 (pp. 709)
  val DRMUX = MuxLookup(ctrl.DRMUX, IR(11, 9), Seq(
    0.U -> IR(11, 9), // DR field of IR
    1.U -> "b111".U,  // R7
    2.U -> "b110".U   // R6 (stack ptr)
  ))

  val SR1MUX = MuxLookup(ctrl.SR1MUX, IR(11, 9), Seq(
    0.U -> IR(11, 9), // SR 1 of IR
    1.U -> IR(8, 6),  // SR 2 of IR
    2.U -> "b110".U   // R6 (stack ptr)
  ))

  // Controls whether the ALU is fed from the second register
  // file output port (SR2) or from a SEXT16(IR[4:0])
  // sel. line for this one is "imm" bit of operate instrs (bit 5)
  // TODO: FILL ME IN!
  val SR2MUX = 0.U(16.W)

  // Logic for computing condition codes
  val newN    = busOut(15)
  val newZ    = ~busOut.orR() // fan-in NOR
  val newP    = ~newN & ~newZ
  val ccLogic = Cat(newN, newZ, newP)

  // Controls whether the condition codes are loaded from
  // the ccLogic (on writes to the reg file) or from the bus,
  // e.g. when we're reading a PSR value from memory (the stack)
  val CCMUX = Mux(ctrl.PSRMUX, ccLogic, busOut(2, 0))

  // Controls whether Supervisor mode bit (PSR[15]) is loaded
  // from the bus (i.e. from a memory read) or from the control unit
  val PRIVMUX = Mux(ctrl.PSRMUX, ctrl.SetPriv, busOut(15))

  // controls whether the priority field of PSR is loaded
  // from the bus (i.e. from a memory read) or from the device
  // requests (externally provided) priority level
  val PRIOMUX = Mux(ctrl.PSRMUX, io.intPriority, busOut(10, 8))


  /*!========= MUX SETUP =============!*/



  /* ========== Register Updates ============= */

   // TODO: I feel like something's missing here...

   when (ctrl.LDPC) {
     PC := PCMUX
   }

   when (ctrl.LDPriv) {
     PSR.priv := PRIVMUX
   }

   when (ctrl.LDPriority) {
     PSR.priority := PRIOMUX
   }

   when (ctrl.LDCC) {
     PSR.cc := CCMUX
   }

   when (ctrl.LDSavedSSP) {
     SavedSSP := regs.io.sr1Out
   }

   when (ctrl.LDSavedUSP) {
     SavedUSP := regs.io.sr1Out
   }

   // set branch enable if IR CC mask matches CC processor state
   when (ctrl.LDBEN) {
     BEN := (IR(11) & N) |
            (IR(10) & Z) |
            (IR(9)  & P) 
   }

   // see Fig. C.6 (pp 709) and Fig 9.2 (pp. 316)
   // userspace cannot access memory in ranges:
   // - xFE00 -> xFFFF (I/O space)
   // - x0000 -> x2FFF (supervisor area/system stack)
   // KCH: note Fig C.6 has a bug for ACV in 3rd edition
   when (ctrl.LDACV) {
     val busOr = busOut(15,9).andR() |  
                 busOut(15,12).asUInt() < 3.U
     ACV := busOr & PSR.priv
   }

   when (io.intPriority > PSR.priority) {
     aGbReg := true.B
   }

   // wire up the interrupt request line
   io.irq    := aGbReg & io.devIntEnable

  /* !========== Register Updates =============! */

  // wire up the ALU inputs
  ALU.io.in_a  := regs.io.sr1Out
  ALU.io.in_b  := SR2MUX
  ALU.io.opSel := ctrl.ALUK

  // wire up the register file
  regs.io.wEn    := ctrl.LDREG
  regs.io.drSel  := DRMUX
  regs.io.drIn   := busOut
  regs.io.sr1Sel := SR1MUX
  regs.io.sr2Sel := IR(2, 0)


  // wire up DEBUG ports
  io.debugPC  := PC
  io.debugIR  := IR
  io.debugPSR := PSR.asUInt()
  io.debugR0  := regs.io.debugR0
  io.debugR1  := regs.io.debugR1
  io.debugR2  := regs.io.debugR2
  io.debugR3  := regs.io.debugR3
  io.debugR4  := regs.io.debugR4
  io.debugR5  := regs.io.debugR5
  io.debugR6  := regs.io.debugR6
  io.debugR7  := regs.io.debugR7
}

