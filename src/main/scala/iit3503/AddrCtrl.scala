package iit3503
import chisel3._

/* 
 * Address control logic for the memory
 * controller.
 *
 * This logic is in charge of determining which
 * memory accesses are actually to memory-mapped
 * I/O addresses. There are three possible devices
 * that can be accessed other than memory:
 *  - Keyboard (KBSR/KBDR)
 *  - Output Device (DSR/DDR)
 *  - Machine Control Reg (MCR)
 */

trait InMuxConsts {
  val memSel  = 0
  val dsrSel  = 1
  val kbsrSel = 2
  val kbdrSel = 3
  val mcrSel  = 4
}

// See Fig C.3, P&P pp. 712. This logic
// implements the address control box at the
// bottom of that figure
class AddrCtrl extends Module with InMuxConsts {
  val io = IO(new Bundle {
    val MAR   = Input(UInt(16.W))
    val MIOEN = Input(Bool())
    val RW    = Input(Bool())

    val MEMEN     = Output(Bool())
    val INMUX_SEL = Output(UInt(2.W))
    val LDKBSR    = Output(Bool())
    val LDDSR     = Output(Bool())
    val LDDDR     = Output(Bool())
    val LDMCR     = Output(Bool())

    // reads to the KBSR should clear
    // the KBSR ready bit, which the memory
    // controller manages. We need to tell
    // it that a read occured to KBSR so
    // it can clear that bit for us.
    val kbsrRead = Output(Bool())
  })

  io.INMUX_SEL := DontCare
  io.MEMEN     := false.B
  io.LDKBSR    := false.B
  io.LDDSR     := false.B
  io.LDDDR     := false.B
  io.LDMCR     := false.B

  io.kbsrRead := false.B

  when (io.MIOEN) {
    // KBSR
    when (io.MAR === "hFE00".U) {
      when (io.RW) { // write
        io.LDKBSR := true.B
        } .otherwise {
          io.INMUX_SEL := kbsrSel.U
          io.kbsrRead := true.B
        }
    // KBDR
    } .elsewhen (io.MAR === "hFE02".U) {
      when (io.RW === false.B) { // reads only on KBDR
        io.INMUX_SEL := kbdrSel.U
      }
    // DSR
    } .elsewhen (io.MAR === "hFE04".U) {
      when (io.RW) { // write
        io.LDDSR := true.B
      } .otherwise {
        io.INMUX_SEL := dsrSel.U
      }
    // DDR
    } .elsewhen (io.MAR === "hFE06".U) {
      when (io.RW) { // write, no reads on DDR
        io.LDDDR := true.B
      } 
    // MCR
    } .elsewhen (io.MAR === "hFFFE".U) {
      when (io.RW) { // write
        io.LDMCR := true.B
      } .otherwise {
        io.INMUX_SEL := mcrSel.U
      }
    // Memory
    } .otherwise {
      io.MEMEN := true.B
      when (io.RW === false.B) { 
        io.INMUX_SEL := memSel.U
      }
    }
  }
}
