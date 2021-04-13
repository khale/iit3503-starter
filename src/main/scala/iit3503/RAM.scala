package iit3503
import chisel3._

/* 
 * This means that the RAM is actually
 * *external* to chisel. It is not synthesized
 * in hardware, but rather hooking into some
 * other provider. We use this so our simulator
 * can set up RAM for us. 
 *
 */
class ExternalRAM extends BlackBox {
  val io = IO(new Bundle {
    val clk     = Input(Clock())
    val en      = Input(Bool())
    val wEn     = Input(Bool())
    val dataIn  = Input(UInt(16.W))
    val addr    = Input(UInt(16.W))

    val dataOut = Output(UInt(16.W))
    val R       = Output(Bool())
  })
}


class RAM(size: Int) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())
    val wEn    = Input(Bool())
    val dataIn = Input(UInt(16.W))
    val addr   = Input(UInt(16.W))

    val dataOut = Output(UInt(16.W))
    val R       = Output(Bool())
  })

  val mem = SyncReadMem(size, UInt(16.W))

  io.dataOut := DontCare

  when (io.en) {
    val rdwrPort = mem(io.addr)
    when (io.wEn) { // write
      mem(io.addr) := io.dataIn
    } .otherwise {
      io.dataOut := rdwrPort
    }
  }

  io.R := true.B // single cycle access assumed
}

