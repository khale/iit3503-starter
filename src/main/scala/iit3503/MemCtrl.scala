package iit3503

import chisel3._
import chisel3.util._


class DeviceRegister extends Bundle {
  val ready  = Bool()
  val int_en = Bool()
  val unused = UInt(14.W)
}

/*
 * Memory controller for the 3503
 * Interfaces with:
 * - the datapath
 * - I/O devices (just 2 for now)
 * - main memory
 *
 * The memory controller implements the logic on the bottom of Figure C.3
 * and C.8 (pp. 704 and 714). The memory controller controls memorya
 * access
 *
 */
class MemCtrl extends Module {
  val io = IO(new Bundle {
    // inputs from memory 
    val memR    = Input(Bool())
    val memData = Input(UInt(16.W))

    // inputs from control unit
    val LDMDR = Input(Bool())
    val MIOEN = Input(Bool())
    val LDMAR = Input(Bool())
    val RDWR  = Input(Bool())

    // from datapath
    val bus   = Input(UInt(16.W))

    // for keyboard
    val devReady     = Input(Bool())
    val devData      = Input(UInt(16.W))

    // to datapath 
    val mdrOut       = Output(UInt(16.W))

    // tells datapath an int has fired
    val devIntEnable = Output(Bool())

    // goes to top-level and control unit
    val mcrOut = Output(UInt(16.W))

    // goes to control
    val R      = Output(Bool())

    // these go out to memory
    val en     = Output(Bool())
    val wEn    = Output(Bool())
    val dataIn = Output(UInt(16.W))
    val addr   = Output(UInt(16.W))

    // out to serial port
    val tx = DecoupledIO(UInt(8.W))

    // DEBUG OUTPUTS
    val debugMDR = Output(UInt(16.W))
    val debugMAR = Output(UInt(16.W))
    val debugDSR = Output(UInt(16.W))
    val debugDDR = Output(UInt(16.W))
    val debugMCR = Output(UInt(16.W))
  })

  val MDR = RegInit(0.U(16.W))
  val MAR = RegInit(0.U(16.W))

  val addrCtrl = Module(new AddrCtrl)

  // device registers
  val DSR = RegInit(0.U(16.W)) // device status reg
  val DDR = RegInit(0.U(16.W)) // device data reg

  // keyboard regsiters
  val KBSR = RegInit(0.U.asTypeOf(new DeviceRegister))
  val KBDR = RegInit(0.U(16.W))                   // keyboard data reg

  val MCR = RegInit(Cat(1.U(1.W), 0.U(15.W))) // machine control reg (bit 15 is clock enable bit)

  // short hands for control signals
  // from address control logic
  val ldKBSR   = addrCtrl.io.LDKBSR
  val ldDSR    = addrCtrl.io.LDDSR
  val ldDDR    = addrCtrl.io.LDDDR
  val ldMCR    = addrCtrl.io.LDMCR
  val inMuxSel = addrCtrl.io.INMUX_SEL

  // tell datapath whether or not the device is set
  // to enable interrupts
  io.devIntEnable := KBSR.int_en & KBSR.ready

  // connect DSR, DDR to serial output
  DSR := Cat(io.tx.ready, 0.U(15.W))
  io.tx.valid := RegNext(ldDDR)
  io.tx.bits  := DDR(7, 0)

  // expose MDR to datapath
  io.mdrOut := MDR

  // expose MCR to control and top-level
  io.mcrOut := MCR

  // wire up address controller
  addrCtrl.io.MAR   := MAR
  addrCtrl.io.MIOEN := io.MIOEN
  addrCtrl.io.RW    := io.RDWR

  // wire up the memory
  io.en     := addrCtrl.io.MEMEN
  io.wEn    := io.RDWR
  io.dataIn := MDR
  io.addr   := MAR

  io.R := MuxLookup(inMuxSel, io.memData, Seq( 
    //0.U -> io.memR,
    0.U -> true.B,
    1.U -> true.B,
    2.U -> true.B,
    3.U -> true.B,
    4.U -> true.B
  ))


  // MMIO select: controls whether the MDR is loaded from:
  // - DSR (device status)
  // - KBSR (keyboard status)
  // - KBDR (keyboard data)
  // - MCR (machine control)
  // - Memory
  val INMUX  = MuxLookup(inMuxSel, io.memData, Seq(
    0.U -> io.memData,
    1.U -> DSR,
    2.U -> KBSR.asUInt(),
    3.U -> KBDR,
    4.U -> MCR
  ))

  // Controls whether the MDR is loaded from the bus
  // (for writes to devices/memory) or from the INMUX (above)
  // This essentially controls the "directionality" of
  // the MDR
  val MDRMUX = Mux(io.MIOEN, INMUX, io.bus)

  // TODO: MDR and MAR never get loaded???

  when (ldKBSR) {
    KBSR := MDR.asTypeOf(new DeviceRegister)
  } 

  KBSR.ready := io.devReady

  when (ldDSR) {
    DSR := MDR
  }

  when (ldDDR) {
    DDR := MDR
  }

  when (ldMCR) {
    MCR := MDR
  }

  KBDR := io.devData

  // wire up debug signals
  io.debugMDR := MDR
  io.debugMAR := MAR
  io.debugDDR := DDR
  io.debugDSR := DSR
  io.debugMCR := MCR
}
