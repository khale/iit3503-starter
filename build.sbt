// See README.md for license details.

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "1.0.0"
ThisBuild / organization     := "com.github.khale"

//unmanagedSources / excludeFilter := HiddenFileFilter || "*ROM*"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.2",
  "edu.berkeley.cs" %% "chiseltest" % "0.2.1"
)




lazy val root = (project in file("."))
  .settings(
    name := "iit3503",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.4.3",
      "edu.berkeley.cs" %% "chiseltest" % "0.3.2" % "test"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.11",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:useBundlePlugin"
    ),
    resolvers ++= Seq(
        Resolver.sonatypeRepo("snapshots"),
        Resolver.sonatypeRepo("releases")
        ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.4.3" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )


