/*
 *  Copyright 2021-2026 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.codegen.internals

import coursierapi.IvyRepository
import coursierapi.MavenRepository

// Adapted from coursier's own `Repositories` object, Copyright the coursier
// contributors, licensed under the Apache License, Version 2.0
// (http://www.apache.org/licenses/LICENSE-2.0):
// https://github.com/coursier/coursier/blob/main/modules/coursier/shared/src/main/scala/coursier/Repositories.scala
// Adapted to build `coursierapi` types (`MavenRepository.of`/`IvyRepository.of`,
// literal Ivy pattern strings) instead of coursier-core's own `Repository` model
// (`MavenRepository(...)` apply syntax, `IvyRepository.fromPattern`, structured
// `coursier.ivy.Pattern`). To be replaced once equivalents land in coursier-interface.
private[internals] object Repositories {

  // Corresponds to coursier's default Ivy pattern:
  // [organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]
  private val ivyDefaultPattern =
    "[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]"

  def central: MavenRepository =
    MavenRepository.of("https://repo1.maven.org/maven2")
  def sonatype(name: String): MavenRepository =
    MavenRepository.of(s"https://oss.sonatype.org/content/repositories/$name")
  def sonatypeS01(name: String): MavenRepository =
    MavenRepository.of(
      s"https://s01.oss.sonatype.org/content/repositories/$name"
    )
  def bintray(id: String): MavenRepository =
    MavenRepository.of(s"https://dl.bintray.com/$id")
  def bintray(owner: String, repo: String): MavenRepository =
    bintray(s"$owner/$repo")
  def bintrayIvy(id: String): IvyRepository =
    IvyRepository.of(
      s"https://dl.bintray.com/${id.stripSuffix("/")}/$ivyDefaultPattern"
    )
  def typesafe(id: String): MavenRepository =
    MavenRepository.of(s"https://repo.typesafe.com/typesafe/$id")
  def typesafeIvy(id: String): IvyRepository =
    IvyRepository.of(
      s"https://repo.typesafe.com/typesafe/ivy-$id/$ivyDefaultPattern"
    )
  def sbtPlugin(id: String): IvyRepository =
    IvyRepository.of(
      s"https://repo.scala-sbt.org/scalasbt/sbt-plugin-$id/$ivyDefaultPattern"
    )
  def sbtMaven(id: String): MavenRepository =
    MavenRepository.of(s"https://repo.scala-sbt.org/scalasbt/maven-$id")
  def scalaIntegration: MavenRepository =
    MavenRepository.of(
      "https://scala-ci.typesafe.com/artifactory/scala-integration"
    )
  def jitpack: MavenRepository =
    MavenRepository.of("https://jitpack.io")
  def clojars: MavenRepository =
    MavenRepository.of("https://repo.clojars.org")
  def jcenter: MavenRepository =
    MavenRepository.of("https://jcenter.bintray.com")
  def google: MavenRepository =
    MavenRepository.of("https://maven.google.com")

  // https://storage-download.googleapis.com/maven-central/index.html
  def centralGcs: MavenRepository =
    MavenRepository.of(
      "https://maven-central.storage-download.googleapis.com/maven2"
    )
  def centralGcsEu: MavenRepository =
    MavenRepository.of(
      "https://maven-central-eu.storage-download.googleapis.com/maven2"
    )
  def centralGcsAsia: MavenRepository =
    MavenRepository.of(
      "https://maven-central-asia.storage-download.googleapis.com/maven2"
    )

  def apache(id: String): MavenRepository =
    MavenRepository.of(
      s"https://repository.apache.org/content/repositories/$id"
    )
}
