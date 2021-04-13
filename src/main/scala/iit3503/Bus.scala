package iit3503

import chisel3._
import chisel3.util._

/**
  *  Our 16-bit bus for the 3503's data path.
  *  Note that the control unit is in charge of specifying
  *  who gets to use the bus. A vector of 8 sources is provided
  *  as input, and one of these sources will be routed to the output.
  *  When the control unit wants to enable a particular source, it
  *  does so by asserting the bit in input_sel that corresponds to that
  *  source. 
  *
  */
class Bus extends Module {
  val io = IO(new Bundle {
    // the input select is an 8-bit unsigned, where each bit corresponds to a
    // device wanting to drive the bus. We can only have one driver at any given time.
    // Thus, this integer will be a "one-hot" encoding--only one bit is set to one.
    val inputSel = Input(UInt(8.W))
    val inputs   = Input(Vec(8, UInt(16.W))) // this is a vector of the actual lines coming from the inputs
    val output   = Output(UInt(16.W)) // this is the *chosen* output
  })

  // we could also achieve this using a chisel utils one-liner:
  //    io.output := io.inputs(OHToUInt(Reverse(io.inputSel)))
  val oneHotMux = Mux1H(Seq(
    io.inputSel(0) -> io.inputs(0),
    io.inputSel(1) -> io.inputs(1),
    io.inputSel(2) -> io.inputs(2),
    io.inputSel(3) -> io.inputs(3),
    io.inputSel(4) -> io.inputs(4),
    io.inputSel(5) -> io.inputs(5),
    io.inputSel(6) -> io.inputs(6),
    io.inputSel(7) -> io.inputs(7)
  ))

  io.output := oneHotMux
}
