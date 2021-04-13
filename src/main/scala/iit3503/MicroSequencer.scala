package iit3503

import chisel3._
import chisel3.util._

/* Microsequencer: See P&P App C.4, p.707 
 *
 * This module acts as the address generator for our
 * microcode ROM. That is, it will generate an 
 * address of the control store to get a microinstruction
 * from *on the next clock*.
 *
 * This address depends on various external signals coming from
 * the data path and the higher-level control unit.
 * 
 */
class MicroSequencer extends Module {

  val io = IO(new Bundle {
    val cFeed    = Input(new CtrlFeedback)
    val INT      = Input(Bool())
    val R        = Input(Bool())
    val IR15_11  = Input(UInt(5.W))
    val BEN      = Input(Bool())
    val PSR15    = Input(Bool())
    val ACV      = Input(Bool())

    val ctrlAddr = Output(UInt(6.W)) // address of next state in control store
  })

  io.ctrlAddr := DontCare

  val condSide = Wire(UInt(6.W))


  // See Figure C.5
  io.ctrlAddr := Mux(io.cFeed.IRD,   // selector
                     Cat(0.U(2.W), io.IR15_11(4, 1)),  // if true 00 ++ IR[15:12]
                     condSide) // else logic below

  // Faults/Exceptions
  val and1 = io.cFeed.COND(2) & 
             io.cFeed.COND(1) &
            ~io.cFeed.COND(0) & 
             io.ACV

  // Interrupt Present
  val and2 = io.cFeed.COND(2) &
            ~io.cFeed.COND(1) &
             io.cFeed.COND(0) &
             io.INT

  // User Privilege Mode
  val and3 = io.cFeed.COND(2) &
            ~io.cFeed.COND(1) &
            ~io.cFeed.COND(0) &
             io.PSR15

  // Branch 
  // TODO: job security
  val and4 = false.B

  // Ready
  val and5 = ~io.cFeed.COND(2) &
             ~io.cFeed.COND(1) &
              io.cFeed.COND(0) &
              io.R

  // Addr mode
  val and6 = ~io.cFeed.COND(2) &
              io.cFeed.COND(1) &
              io.cFeed.COND(0) &
              io.IR15_11(0)


  val x = Cat(and1, and2, and3, and4, and5, and6)

  condSide := x | io.cFeed.J
}
