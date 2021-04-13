package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3.experimental.BundleLiterals._

class ControlStoreTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Control Store"

  it should "handle an untaken branch correctly" in {
    test(new ControlStore()) { c =>

      c.io.addr.poke(0.U)
      c.io.out.feed.COND.expect("b010".U)
      c.io.out.feed.J.expect(18.U)
    }
  }

}
