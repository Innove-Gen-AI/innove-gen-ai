/*
 * Copyright 2023 Innové Gen AI
 *
 */

import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "com.google.cloud" % "google-cloud-aiplatform" % "3.13.0"
  )

}
