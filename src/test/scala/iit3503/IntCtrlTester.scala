package iit3503

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IntCtrlTester extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Interrupt Controller"

  it should "accept a device-provided interrupt vector" in {
    test(new IntCtrl()) { c =>
      c.io.TableMux.poke(false.B)
      c.io.VectorMux.poke(0.U)
      c.io.LDVector.poke(true.B)
      c.io.INTV.poke("hFF".U)
      c.clock.step(1)
      c.io.out.expect("h01FF".U)
    }
  }

  it should "load a TRAP vector from the bus" in {
    test(new IntCtrl()) { c =>
      c.io.TableMux.poke(true.B) // TableReg <- x00
      c.io.VectorMux.poke(0.U)
      c.io.LDVector.poke(true.B)
      c.clock.step(1)
      c.io.bus.poke("h00FF".U) // TRAP #255
      c.clock.step(1)
      c.io.out.expect("h00FF".U)
    }
  }

  it should "load ACV vector correctly" in {
    test(new IntCtrl()) { c =>
      c.io.TableMux.poke(false.B) // TableReg <- x01
      c.io.VectorMux.poke(3.U) // x02
      c.io.LDVector.poke(true.B)
      c.clock.step(1)
      c.io.out.expect("h0102".U)
    }
  }

  it should "load UD vector correctly" in {
    test(new IntCtrl()) { c =>
      c.io.TableMux.poke(false.B) // TableReg <- x01
      c.io.VectorMux.poke(2.U) // x01
      c.io.LDVector.poke(true.B)
      c.clock.step(1)
      c.io.out.expect("h0101".U)
    }
  }

  it should "load privilege mode exception vector correctly" in {
    test(new IntCtrl()) { c =>
      c.io.TableMux.poke(false.B) // TableReg <- x01
      c.io.VectorMux.poke(1.U) // x00
      c.io.LDVector.poke(true.B)
      c.clock.step(1)
      c.io.out.expect("h0100".U)
    }
  }

}
