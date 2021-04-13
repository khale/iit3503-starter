package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MicroSeqTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "MicroSequencer"

  it should "give back {00 ++ IR[15..12]} when IRD ctrl line is low" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(true.B) // next uPC should come from IR
      c.io.cFeed.COND.poke(0.U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b01010".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000101".U)

      c.io.INT.poke(true.B) // shouldn't matter
      c.io.ctrlAddr.expect("b000101".U)

      c.io.IR15_11.poke("b00010".U) // should change
      c.io.ctrlAddr.expect("b000001".U)

      c.io.IR15_11.poke("b00011".U) // flipping addr mode bit shouldn't matter
      c.io.ctrlAddr.expect("b000001".U)
    }
  }

  it should "handle ACV condition properly" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(false.B) // next uPC should go to exception state
      c.io.cFeed.COND.poke("b110".U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b00000".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000000".U)

      c.io.cFeed.J.poke("b100000".U)

      c.io.ctrlAddr.expect("b100000".U)

      c.io.INT.poke(true.B) // shouldn't change

      c.io.ctrlAddr.expect("b100000".U)

      c.io.cFeed.J.poke("b000000".U)
      c.io.ACV.poke(true.B)

      c.io.ctrlAddr.expect("b100000".U)

    }
  }

  it should "handle INT condition properly" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(false.B) // next uPC should go to exception state
      c.io.cFeed.COND.poke("b101".U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b00000".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000000".U)

      c.io.cFeed.J.poke("b100000".U)

      c.io.ctrlAddr.expect("b100000".U)

      c.io.R.poke(true.B) // shouldn't change

      c.io.ctrlAddr.expect("b100000".U)

      c.io.cFeed.J.poke("b000000".U)
      c.io.INT.poke(true.B)

      c.io.ctrlAddr.expect("b010000".U)

    }
  }

  it should "handle PSR[15] condition properly" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(false.B) // next uPC should go to exception state
      c.io.cFeed.COND.poke("b100".U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b00000".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000000".U)

      c.io.cFeed.J.poke("b100000".U)

      c.io.ctrlAddr.expect("b100000".U)

      c.io.INT.poke(true.B) // shouldn't change

      c.io.ctrlAddr.expect("b100000".U)

      c.io.cFeed.J.poke("b000000".U)
      c.io.PSR15.poke(true.B)

      c.io.ctrlAddr.expect("b001000".U)

    }
  }

  it should "handle BEN condition properly" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(false.B) // next uPC should go to exception state
      c.io.cFeed.COND.poke("b010".U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b00000".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000000".U)

      c.io.cFeed.J.poke("b100000".U)

      c.io.ctrlAddr.expect("b100000".U)

      c.io.INT.poke(true.B) // shouldn't change

      c.io.ctrlAddr.expect("b100000".U)

      c.io.cFeed.J.poke("b000000".U)
      c.io.BEN.poke(true.B)

      c.io.ctrlAddr.expect("b000100".U)

    }
  }

  it should "handle R condition properly" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(false.B) // next uPC should go to exception state
      c.io.cFeed.COND.poke("b001".U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b00000".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000000".U)

      c.io.cFeed.J.poke("b100000".U)

      c.io.ctrlAddr.expect("b100000".U)

      c.io.INT.poke(true.B) // shouldn't change

      c.io.ctrlAddr.expect("b100000".U)

      c.io.cFeed.J.poke("b000000".U)
      c.io.R.poke(true.B)

      c.io.ctrlAddr.expect("b000010".U)

    }
  }

  it should "handle IR[11] condition properly" in {
    test(new MicroSequencer()) { c =>
      
      c.io.cFeed.IRD.poke(false.B) // next uPC should go to exception state
      c.io.cFeed.COND.poke("b011".U)
      c.io.cFeed.J.poke(0.U)
      c.io.INT.poke(false.B)
      c.io.R.poke(false.B)
      c.io.IR15_11.poke("b00000".U)
      c.io.BEN.poke(false.B)
      c.io.PSR15.poke(false.B)
      c.io.ACV.poke(false.B)

      c.io.ctrlAddr.expect("b000000".U)

      c.io.cFeed.J.poke("b100000".U)

      c.io.ctrlAddr.expect("b100000".U)

      c.io.BEN.poke(true.B) // shouldn't change

      c.io.ctrlAddr.expect("b100000".U)

      c.io.cFeed.J.poke("b000000".U)
      c.io.IR15_11.poke("b00001".U)

      c.io.ctrlAddr.expect("b000001".U)

    }
  }

} 


