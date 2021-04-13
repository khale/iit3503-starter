package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataPathTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Data Path"

  it should "init PC at the provided reset vector" in {
    test(new DataPath()) { c =>
      c.reset.poke(true.B)
      c.io.ctrlSigs.LDPC.poke(false.B)
      c.io.resetVec.poke("h03FF".U)
      c.clock.step(1)
      c.reset.poke(false.B)
      c.io.debugPC.expect("h03FF".U)
    }
  }

  it should "load IR correctly" in {
    test(new DataPath()) { c =>
      c.io.ctrlSigs.GateMDR.poke(true.B)
      c.io.mdrVal.poke("hF00D".U)
      c.io.ctrlSigs.LDIR.poke(true.B)
      c.clock.step(1)
      c.io.debugIR.expect("hF00D".U)
    }
  }

}
