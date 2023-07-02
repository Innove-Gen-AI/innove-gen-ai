/*
 * Copyright 2023 Innové Gen AI
 *
 */

import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "com.google.cloud" % "google-cloud-aiplatform" % "3.13.0",
    "io.kontainers" % "purecsv_2.13" % "1.3.10",
    "org.mongodb.scala" %% "mongo-scala-driver" % "4.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalamock" %% "scalamock" % "5.2.0" % Test
  )

}
