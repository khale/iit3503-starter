import mill._, scalalib._
import coursier.maven.MavenRepository

/**
 * Scala 2.12 module that is source-compatible with 2.11.
 * This is due to Chisel's use of structural types. See
 * https://github.com/freechipsproject/chisel3/issues/606
 */
trait HasXsource211 extends ScalaModule {
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-deprecation",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-unchecked",
      "-Xsource:2.11"
    )
  }
  override def repositories = super.repositories ++ Seq(
          MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
          MavenRepository("https://oss.sonatype.org/content/repositories/releases")
          )
}

trait HasChisel3 extends ScalaModule {
  override def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.4.3",
 )
}

trait HasChiselTests extends CrossSbtModule  {
  object test extends Tests {
    override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.8", ivy"edu.berkeley.cs::chisel-iotesters:1.2+")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}


object iit3503_lab extends HasChisel3 with CrossSbtModule with HasXsource211 {
  def crossScalaVersion = "2.12.12"
  object test extends Tests {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest:3.0.4",
      ivy"edu.berkeley.cs::chisel-iotesters:1.4.1+",
      ivy"edu.berkeley.cs::chiseltest:0.3.2+"
    )

    def testFrameworks = T {
      Seq(
        "org.scalatest.tools.Framework",
        "utest.runner.Framework"
      )
    }

    def testOnly(args: String*) = T.command {
      super.runMain("org.scalatest.tools.Runner", args: _*)
    }
  }
}
