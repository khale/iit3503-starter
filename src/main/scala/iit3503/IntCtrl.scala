package iit3503

import chisel3._
import chisel3.util._

/*
 * Interrupt and exception controller for the 3503
 *
 * This unit is in charge of loading the bus with the
 * proper address for an OS service routine. This depends
 * on:
 * - whether it is an interrupt, trap, or exception
 * - if an interrupt, what interrupt number was provided by the device
 *
 * This module roughly corresponds to the trap/interrupt/exception control
 * logic shown at the bottom right of Figure C.8 in P&P (pp. 714)
 *
 */
class IntCtrl extends Module {
  val io = IO(new Bundle {
    val bus       = Input(UInt(16.W))
    val VectorMux = Input(UInt(2.W))
    val TableMux  = Input(Bool())
    val INTV      = Input(UInt(8.W))
    val LDVector  = Input(Bool())

    val out = Output(UInt(16.W)) // OS service routine address
  })

  val tableReg = RegInit(0.U(8.W))
  val vecReg   = RegInit(0.U(8.W))

  val vMux = MuxLookup(io.VectorMux, io.INTV, Seq(
    0.U -> io.INTV,
    1.U -> "h00".U(8.W),
    2.U -> "h01".U(8.W),
    3.U -> "h02".U(8.W)
  ))

  val vecLoadMux = Mux(io.TableMux, io.bus(7,0), vMux)
  val tabLoadMux = Mux(io.TableMux, 0.U(8.W), 1.U(8.W))

  when (io.LDVector) {
    vecReg   := vecLoadMux
    tableReg := tabLoadMux
  }

  io.out := Cat(tableReg, vecReg)
}
