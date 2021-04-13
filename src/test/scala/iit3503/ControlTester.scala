package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ControlTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Control Unit"

  it should "start in IFETCH state (18)" in {
    test(new Control()) { c =>
      c.io.debuguPC.expect(18.U)
    }
  }

  it should "go through unexceptional IFETCH sequence properly" in {
    test(new Control()) { c =>
      // note ACV and INT are not asserted
      val states = List(18, 33, 28, 30, 32)
      c.io.R.poke(true.B) // assume the fetch read is complete
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "initiate the INT sequence properly" in {
    test(new Control()) { c =>
      // note ACV and INT are not asserted
      val states = List(18, 49)
      c.io.INT.poke(true.B) // IRQ requested
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "initiate the ACV sequence properly in an IFETCH" in {
    test(new Control()) { c =>
      // note ACV and INT are not asserted
      val states = List(18, 33, 60)
      c.io.ACV.poke(true.B) // IRQ requested
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "actually wait for memory in an IFETCH" in {
    test(new Control()) { c =>
      // note ACV and INT are not asserted
      val states = List(18, 33, 28, 28, 28, 28)
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }

      c.io.R.poke(true.B) // mem ready
      c.io.debuguPC.expect(28.U)
      c.clock.step(1)
      c.io.debuguPC.expect(30.U)

    }

    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 1, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h1000".U) // simulated ADD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through ADD states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 1, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h1000".U) // simulated ADD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through AND states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 5, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h5000".U) // simulated AND
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through NOT states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 9, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h9000".U) // simulated NOT
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through LEA states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 14, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("he000".U) // simulated LEA
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through LD states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 2, 35, 25, 27, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h2000".U) // simulated LD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through LDR states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 6, 35, 25, 27, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h6000".U) // simulated LDR
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through LDI states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 10, 17, 24, 26, 35, 25, 27, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("ha000".U) // simulated LDI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through STI states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 11, 19, 29, 31, 23, 16, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("hb000".U) // simulated STI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through STR states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 7, 23, 16, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h7000".U) // simulated STR
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through ST states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 3, 23, 16, 18)
      c.io.R.poke(true.B) // assume memory is ready
      c.io.IR.poke("h3000".U) // simulated ST
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through JSR states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 4, 21, 18)
      c.io.R.poke(true.B)
      c.io.IR.poke("h4800".U) // simulated JSR 
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through JSRR states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 4, 20, 18)
      c.io.R.poke(true.B)
      c.io.IR.poke("h4000".U) // simulated JSRR
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through JMP states properly " in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 12, 18)
      c.io.R.poke(true.B)
      c.io.IR.poke("hc000".U) // simulated JMP
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through BR states properly when branch not taken" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 0, 18)
      c.io.R.poke(true.B)
      c.io.IR.poke("h0000".U) // simulated BR
      c.io.BEN.poke(false.B) // branch not taken
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through BR states properly when branch taken" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 0, 22, 18)
      c.io.R.poke(true.B)
      c.io.IR.poke("h0000".U) // simulated BR
      c.io.BEN.poke(true.B) // branch taken
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "handle ACV properly on an LD" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 2)
      c.io.R.poke(true.B)
      c.io.IR.poke("h2000".U) // simulated LD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }

      c.io.ACV.poke(true.B)

      val states2 = List(35, 57, 45, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      for (s <- states2) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "handle ACV properly on LDI indirect access" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 10)
      c.io.R.poke(true.B)
      c.io.IR.poke("ha000".U) // simulated LDI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }

      c.io.ACV.poke(true.B)

      val states2 = List(17, 56, 45, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      for (s <- states2) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }


  it should "go through undefined opcode states properly in supervisor mode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 13, 62, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      c.io.PSR15.poke(false.B) // supervisor mode
      c.io.R.poke(true.B)
      c.io.IR.poke("hd000".U) // simulated UD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through undefined opcode states properly in user mode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 13, 62, 45, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      c.io.PSR15.poke(true.B) // user mode
      c.io.R.poke(true.B)
      c.io.IR.poke("hd000".U) // simulated UD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "respect the old gods and the new" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 13, 63, 18)
      c.io.PSR15.poke(true.B) // user mode
      c.io.R.poke(true.B)
      c.io.IR.poke("hd800".U) // simulated UD
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
      c.io.PSR15.expect(true.B)
    }
  }

  it should "raise an exception for RTI in user mode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 8, 44, 45, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      c.io.PSR15.poke(true.B) // user mode
      c.io.R.poke(true.B) // reads always ready
      c.io.IR.poke("h8000".U) // simulated RTI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through RTI states properly in supervisor mode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 8, 36, 38, 39, 40, 42, 34, 51, 18)
      c.io.R.poke(true.B) // reads always ready
      c.io.IR.poke("h8000".U) // simulated RTI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through RTI states properly in supervisor mode when the INT happened in usermode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 8, 36, 38, 39, 40, 42)
      c.io.R.poke(true.B) // reads always ready
      c.io.IR.poke("h8000".U) // simulated RTI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }

      c.io.PSR15.poke(true.B) // popped PSR is usermode (INT happened in ring 1)
      val states2 = List(34, 59, 18)
      for (s <- states2) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through TRAP states properly in usermode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 15, 47, 45, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      c.io.R.poke(true.B) // reads always ready
      c.io.PSR15.poke(true.B) // in usermode
      c.io.IR.poke("hf000".U) // simulated RTI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

  it should "go through TRAP states properly in supervisor mode" in {
    test(new Control()) { c =>
      val states = List(18, 33, 28, 30, 32, 15, 47, 37, 41, 43, 46, 52, 54, 53, 55, 18)
      c.io.R.poke(true.B) // reads always ready
      c.io.IR.poke("hf000".U) // simulated RTI
      for (s <- states) {
        c.io.debuguPC.expect(s.U)
        c.clock.step(1)
      }
    }
  }

} 


