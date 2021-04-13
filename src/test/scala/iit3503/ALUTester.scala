package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ALUTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "ALU"

  it should "ADD properly" in {
    test(new ALU()) { c =>
      for (i <- 0 to 200) {
        for (j <- 0 to 200) {
          c.io.opSel.poke(0.U)
          c.io.in_a.poke(i.U)
          c.io.in_b.poke(j.U)
          c.io.out.expect((i+j).U)
        } 
      } 
    }
  }

  it should "wrap around on overflow addition" in {
    test(new ALU()) { c =>
      c.io.opSel.poke(0.U)
      c.io.in_a.poke(((1<<16) - 1).asUInt)
      c.io.in_b.poke(2.U)
      c.io.out.expect(1.U)
    } 
  }

  it should "compute AND properly" in {
    test(new ALU()) { c =>
      for (i <- 0 to 200) {
        for (j <- 0 to 200) {
          c.io.opSel.poke(1.U)
          c.io.in_a.poke(i.U)
          c.io.in_b.poke(j.U)
          c.io.out.expect((i&j).U)
        } 
      } 
    }
  }

  it should "negate (NOT) properly on A input" in {
    test(new ALU()) { c =>
      c.io.opSel.poke(2.U)
      c.io.in_a.poke(0.U)
      c.io.in_b.poke("hFFFF".U)
      c.io.out.expect("hFFFF".U)
    } 
  }

  it should "PASS A correctly" in {
    test(new ALU()) { c =>
      for (i <- 0 to 200) {
        for (j <- 0 to 200) {
          c.io.opSel.poke(3.U)
          c.io.in_a.poke(i.U)
          c.io.in_b.poke(j.U)
          c.io.out.expect(i.U)
        } 
      } 
    } 
  }

} 


