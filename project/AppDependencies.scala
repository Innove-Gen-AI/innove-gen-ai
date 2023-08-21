/*
 * Copyright 2023 Innové Gen AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "com.google.cloud" % "google-cloud-aiplatform" % "3.21.0",
    "io.kontainers" % "purecsv_2.13" % "1.3.10",
    "org.mongodb.scala" %% "mongo-scala-driver" % "4.10.0",
    "com.google.guava" % "guava" % "32.1.2-jre"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalamock" %% "scalamock" % "5.2.0" % Test
  )

}
