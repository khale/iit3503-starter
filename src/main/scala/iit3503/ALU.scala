package iit3503

import chisel3._
import chisel3.util._


/**
  * Implements the simple 4-op 3503 ALU
  * Following the LC-3 ISA specification,
  * we only support:
  *   - ADD
  *   - AND
  *   - NOT
  *   - PASSA : just passes the A reg out to the output
  */
class ALU extends Module {
  val io = IO(new Bundle {
    val opSel = Input(UInt(2.W))
    val in_a  = Input(UInt(16.W))
    val in_b  = Input(UInt(16.W))
    val out   = Output(UInt(16.W))
  })

  // TODO: who needs math anyway? 
  io.out := 0.U
}
