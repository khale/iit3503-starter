package iit3503
import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

/*
 * Top-level module of 3503
 *
 * This encapsulates the machine as a whole.
 * The role of this module is basicaly to "wire
 * everything together." We connect the inputs
 * of one module to the appropriate outputs
 * of other modules. 
 *
 * This is pretty common for top-level modules in bigger projects.
 * Notice you don't see any combinational or sequential logic here.
 *
 */

class Top extends Module {

  val io = IO(new Bundle{

    val resetVec = Input(UInt(16.W))

    val intv        = Input(UInt(8.W))
    val intPriority = Input(UInt(3.W))

    val devReady = Input(Bool()) // keyboard has input
    val devData  = Input(UInt(16.W)) // keyboard data

    val uartTxd = Output(Bool())
    val halt    = Output(Bool())
    val intAck  = Output(Bool())

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
    val debuguPC = Output(UInt(6.W))
    val debugMAR = Output(UInt(16.W))
    val debugMDR = Output(UInt(16.W))
    val debugDDR = Output(UInt(16.W))
    val debugDSR = Output(UInt(16.W))
    val debugMCR = Output(UInt(16.W))
    val debugBus = Output(UInt(16.W))
  })

  val ctrlUnit = Module(new Control)    // top-level control unit
  val memCtrl  = Module(new MemCtrl)    // memory controller
  val mem      = Module(new ExternalRAM)
  val intCtrl  = Module(new IntCtrl)    // interrupt controller
  val dataPath = Module(new DataPath)   // datapath

  val serialOut = Module(new BufferedTx(50000000, 115200))

  val ctrl = ctrlUnit.io.ctrlLines

  // wire control unit to datapath
  dataPath.io.ctrlSigs := ctrl
  ctrlUnit.io.IR       := dataPath.io.ir
  ctrlUnit.io.bus      := dataPath.io.bus
  ctrlUnit.io.PSR15    := dataPath.io.psr15
  ctrlUnit.io.N        := dataPath.io.n
  ctrlUnit.io.Z        := dataPath.io.z
  ctrlUnit.io.P        := dataPath.io.p
  ctrlUnit.io.BEN      := dataPath.io.bEn
  ctrlUnit.io.ACV      := dataPath.io.ACV
  ctrlUnit.io.INT      := dataPath.io.irq

  // wire memory controller to datapath
  dataPath.io.mdrVal       := memCtrl.io.mdrOut
  dataPath.io.devIntEnable := memCtrl.io.devIntEnable
  memCtrl.io.bus           := dataPath.io.bus

  // wire up memory to memory controller
  memCtrl.io.memR    := mem.io.R
  memCtrl.io.memData := mem.io.dataOut
  mem.io.en          := memCtrl.io.en
  mem.io.wEn         := memCtrl.io.wEn
  mem.io.dataIn      := memCtrl.io.dataIn
  mem.io.addr        := memCtrl.io.addr

  // since this memory is a black box (external)
  // module, it needs to have our clock connected to it
  mem.io.clk := clock
  
  // wire memory controller up to control unit
  ctrlUnit.io.R    := memCtrl.io.R
  ctrlUnit.io.halt := ~memCtrl.io.mcrOut(15)
  memCtrl.io.LDMDR := ctrl.LDMDR
  memCtrl.io.MIOEN := ctrl.MIOEN
  memCtrl.io.LDMAR := ctrl.LDMAR
  memCtrl.io.RDWR  := ctrl.RW

  // bit 15 of MCR is "clock enable" bit
  io.halt := ~memCtrl.io.mcrOut(15)

  // wire up control unit to interrupt controller
  intCtrl.io.VectorMux := ctrl.VectorMUX
  intCtrl.io.TableMux  := ctrl.TableMUX
  intCtrl.io.LDVector  := ctrl.LDVector

  // wire up datapath to interrupt controller
  intCtrl.io.bus             := dataPath.io.bus
  dataPath.io.intHandlerAddr := intCtrl.io.out

  io.uartTxd := serialOut.io.txd
  serialOut.io.channel <> memCtrl.io.tx

  // wire up the keyboard device
  dataPath.io.intPriority := io.intPriority
  intCtrl.io.INTV         := io.intv
  memCtrl.io.devReady     := io.devReady
  memCtrl.io.devData      := io.devData
  io.intAck               := ctrlUnit.io.intAck

  // This is either the techOS entry point (x02CA) or
  // the .ORIG of a user program
  dataPath.io.resetVec := io.resetVec

  /* DEBUG PORTS */
  io.debugPC  := dataPath.io.debugPC
  io.debugIR  := dataPath.io.debugIR
  io.debugPSR := dataPath.io.debugPSR
  io.debugR0  := dataPath.io.debugR0
  io.debugR1  := dataPath.io.debugR1
  io.debugR2  := dataPath.io.debugR2
  io.debugR3  := dataPath.io.debugR3
  io.debugR4  := dataPath.io.debugR4
  io.debugR5  := dataPath.io.debugR5
  io.debugR6  := dataPath.io.debugR6
  io.debugR7  := dataPath.io.debugR7
  io.debuguPC := ctrlUnit.io.debuguPC
  io.debugMDR := memCtrl.io.debugMDR
  io.debugMAR := memCtrl.io.debugMAR
  io.debugBus := dataPath.io.bus
  io.debugDSR := memCtrl.io.debugDSR
  io.debugDDR := memCtrl.io.debugDDR
  io.debugMCR := memCtrl.io.debugMCR
}

object SimMain extends App {
  (new ChiselStage).execute(args, Seq(ChiselGeneratorAnnotation(() => new Top)))
}
