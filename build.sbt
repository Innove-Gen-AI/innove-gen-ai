import play.sbt.PlayImport.PlayKeys.playDefaultPort

name := """innove-gen-ai"""
organization := "innove"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test

playDefaultPort := 80
