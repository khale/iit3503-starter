package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MemCtrlTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Memory Controller"

  it should "load MAR correctly" in {
    test(new MemCtrl()) { c =>
      c.io.LDMAR.poke(true.B)
      c.io.bus.poke("hFEED".U)
      c.clock.step(1)
      c.io.debugMAR.expect("hFEED".U)
    }
  }

  it should "load MDR correctly" in {
    test(new MemCtrl()) { c =>
      c.io.LDMDR.poke(true.B)
      c.io.bus.poke("hF00D".U)
      c.clock.step(1)
      c.io.mdrOut.expect("hF00D".U)
    }
  }
}
