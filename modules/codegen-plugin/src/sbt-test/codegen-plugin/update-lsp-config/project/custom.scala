package demo

import sbt.plugins.JvmPlugin

import _root_.sbt.Keys._
import _root_.sbt._

object MyPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    resolvers ++= Resolver.sonatypeOssRepos("snapshots")
  )

}
