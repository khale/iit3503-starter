package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RegFileTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Register File"

  it should "read and write regs correctly" in {
    test(new RegFile()) { c =>

      c.io.wEn.poke(true.B)
      c.io.drSel.poke(7.U)
      c.io.drIn.poke("hFFFF".U)
      c.clock.step(1)
      c.io.wEn.poke(false.B)
      c.io.sr1Sel.poke(7.U)
      c.io.sr1Out.expect("hFFFF".U)

    }
  }

  it should "allow concurrent reads on both read ports" in {
    test(new RegFile()) { c =>

      c.io.wEn.poke(true.B)
      c.io.drSel.poke(0.U)
      c.io.drIn.poke("hDEAD".U)
      c.clock.step(1)
      c.io.wEn.poke(true.B)
      c.io.drSel.poke(1.U)
      c.io.drIn.poke("hBEEF".U)
      c.clock.step(1)

      c.io.sr1Sel.poke(0.U)
      c.io.sr2Sel.poke(1.U)
      c.io.sr1Out.expect("hDEAD".U)
      c.io.sr2Out.expect("hBEEF".U)

    }
  }


} 


