package iit3503

import chisel3._

/*
 *
 * Double-ported, 16-bit register
 * file for the 3503. 8 GPRs: [R0-R7]
 *
 */
class RegFile extends Module {
  val io = IO(new Bundle {
    val wEn    = Input(Bool())
    val sr1Sel = Input(UInt(3.W))
    val sr2Sel = Input(UInt(3.W))
    val drSel  = Input(UInt(3.W))
    val drIn   = Input(UInt(16.W))

    val sr1Out = Output(UInt(16.W))
    val sr2Out = Output(UInt(16.W))

    // DEBUG OUTPUTS
    val debugR0  = Output(UInt(16.W))
    val debugR1  = Output(UInt(16.W))
    val debugR2  = Output(UInt(16.W))
    val debugR3  = Output(UInt(16.W))
    val debugR4  = Output(UInt(16.W))
    val debugR5  = Output(UInt(16.W))
    val debugR6  = Output(UInt(16.W))
    val debugR7  = Output(UInt(16.W))
  })

  // use Reg of Vec, not Vec of Reg!
  val regs = RegInit(VecInit(Seq.fill(8)(0.U(16.W))))
;
  
  // TODO: implement register writes
  // wEn just means "write a register"
  // This is driven by the LDREG control signal from
  // the control unit

  // TODO: implement register reads
  io.sr1Out := 0.U
  io.sr2Out := 0.U

  // DEBUG OUTPUTS
  io.debugR0 := regs(0)
  io.debugR1 := regs(1)
  io.debugR2 := regs(2)
  io.debugR3 := regs(3)
  io.debugR4 := regs(4)
  io.debugR5 := regs(5)
  io.debugR6 := regs(6)
  io.debugR7 := regs(7)
}
