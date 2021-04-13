package iit3503

import chisel3._

/*
 * Control Unit for the 3503
 *
 * This control unit drives the datapath. It does
 * so using a microcoded, multi-cycle approach. One
 * instruction can take several cycles to execute,
 * and each of those cycles will involve different parts
 * of the datapath. The control unit drives the appropriate control
 * signals for *each* of those cycles. If we view the control
 * as a state machine, this part is in charge of progressing through
 * states on each cycle, and directing to the appropriate states
 * based on components of the datapath and exceptional conditions
 *
 * Each cycle of execution corresponds to a particular state in the
 * state machine (specified in Figs C.2 and C.7 of P&P). Each
 * of these states has a unique number. During each cycle/state
 * the control unit needs to activate particular control signals
 * that drive the datapath. To do that, we use a microcode ROM,
 * which is just a memory that holds values for *all* the control signals.
 * We lookup into that memory based on our state number at the current 
 * cycle, and output the signals we find there. For example, if we're in
 * state 18, we output the signal values stored at rom(18). This is
 * in contrast to hard-coded control, where the signals output on each
 * cycle are computed using a logic circuit.
 *
 * See P&P C.4
 *
 */

class Control extends Module {

  // see Fig C.4 (pp. 707)
  val io = IO(new Bundle {
    val IR    = Input(UInt(16.W))
    val bus   = Input(UInt(16.W))
    val PSR15 = Input(Bool())
    val N     = Input(Bool())
    val Z     = Input(Bool())
    val P     = Input(Bool())
    val INT   = Input(Bool())
    val R     = Input(Bool())
    val BEN   = Input(Bool())
    val ACV   = Input(Bool())

    val halt = Input(Bool())

    val ctrlLines = Output(new CtrlSigs)
    val intAck    = Output(Bool())
    val debuguPC  = Output(UInt(6.W))
  })

  val ir = io.IR

  // our uPC is the "microcode program counter"
  // It directly corresponds to what state we are
  // in the machine's FSM.
  // start in ifetch state (State #18)
  val uPC = RegInit(18.U(6.W))

  // the uIR consists of all control signals
  // corresponding to our *current* state 
  // (which we get from microcode ROM)
  val uIR = 0.U.asTypeOf(new MicroInstr)

  // the microsequencer is an address generator for
  // the control store (the microcode ROM). Given external inputs and
  // our current state (captured in uIR), it will
  // give us the *next* state (the next address in the control
  // store that we'll fetch our control signals from)
  val uSeq = Module(new MicroSequencer)

  // The control store is a "microcode ROM" that
  // implements the FSM for our 3503. It is essentially
  // a lookup table that gives us all the control signals
  // that need to go out to the data path for a given
  // state. A single entry in the ROM corresponds to a
  // single state in the FSM for the control unit (see
  // Figures C.2 and C.7 in P&P). A given state is comprised
  // of all the control signals needed to control the elements
  // on the datapath. The *next* states of this FSM
  // are determined by the microsequencer and external
  // signals coming as input to the control.
  val ctrlStore = Module(new ControlStore)

  // our current state is comprised of all
  // control signals coming from the control store
  // at our *current* uPC. uPC will be updated 
  // in the next clock by our microsequencer.
  // Only updates when the machine is not halted.
  when (io.halt === false.B) {
    uIR := ctrlStore.io.out
  }

  // here we just break up our micro instruction into the
  // part that has to feed back to the microsequencer (ctrlFeed) and
  // the part that will go out to drive the datapath (ctrlSigs)
  val ctrlFeed = uIR.feed
  val ctrlSigs = uIR.sigs

  // tell the ctrl store to give us a micro instruction
  // at current uPC
  ctrlStore.io.addr := uPC

  // send appropriate control signals out to data path
  io.ctrlLines := ctrlSigs

  // get the address to fetch from on the next clock from the ctrl store
  // the microsequencer computes this address, which is why it is 
  // also sometimes called an "address generator"
  // Only updates when the machine is not halted
  when (io.halt === false.B) {
    uPC := uSeq.io.ctrlAddr
  }

  // this is INT ACK behavior (interrupt acknowledge)
  io.intAck := uPC === 49.U

  // connect the feedback lines back to the 
  // input side of the microsequencer 
  uSeq.io.cFeed := ctrlFeed

  // wire up external inputs to the microsequencer
  uSeq.io.INT     := io.INT      // is an interrupt requested by a device?
  uSeq.io.R       := io.R        // when we're waiting for a memory access, has it completed?
  uSeq.io.IR15_11 := ir(15,11)   // the IR from the datapath
  uSeq.io.BEN     := io.BEN      // was a branch taken?
  uSeq.io.PSR15   := io.PSR15    // are we in supervisor mode or user mode?
  uSeq.io.ACV     := io.ACV      // did an access violation occur?

  // DEBUGGING OUTPUTS
  io.debuguPC := uPC 
}
