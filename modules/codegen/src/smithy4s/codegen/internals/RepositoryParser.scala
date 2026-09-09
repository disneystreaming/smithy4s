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

package smithy4s.codegen
package internals

import cats.data.ValidatedNel
import cats.syntax.all._
import coursierapi.Credentials
import coursierapi.IvyRepository
import coursierapi.MavenRepository
import coursierapi.Repository

import java.io.File
import java.net.MalformedURLException
import java.net.URL

// Adapted from coursier's own repository parsing, Copyright the coursier
// contributors, licensed under the Apache License, Version 2.0
// (http://www.apache.org/licenses/LICENSE-2.0):
//   - https://github.com/coursier/coursier/blob/main/modules/coursier/shared/src/main/scala/coursier/parse/RepositoryParser.scala
//   - https://github.com/coursier/coursier/blob/main/modules/coursier/jvm/src/main/scala/coursier/internal/PlatformRepositoryParser.scala
//     (inlined below as this object's `repository`/`LocalRepositories`)
//   - https://github.com/coursier/coursier/blob/main/modules/coursier/shared/src/main/scala/coursier/internal/SharedRepositoryParser.scala
//     (inlined below as `SharedRepositoryParser`)
// Adapted to build `coursierapi` types instead of coursier-core's own `Repository`
// model, since coursierapi has no public equivalent of this alias/pattern parsing
// (see ModelLoader.scala for why). To be replaced once equivalents land in
// coursier-interface upstream.
private[internals] object RepositoryParser {

  def repositories(inputs: Seq[String]): ValidatedNel[String, Seq[Repository]] =
    inputs.toVector
      .traverse(s => repository(s).toValidatedNel)
      .map(_.toSeq)

  def repository(input: String): Either[String, Repository] =
    repository(input, maybeFile = false)

  def repository(
      input: String,
      maybeFile: Boolean
  ): Either[String, Repository] =
    if (input == "ivy2local" || input == "ivy2Local")
      Right(LocalRepositories.ivy2Local)
    else if (input == "ivy2cache" || input == "ivy2Cache")
      Right(LocalRepositories.Dangerous.ivy2Cache)
    else if (input == "m2Local" || input == "m2local")
      Right(LocalRepositories.Dangerous.maven2Local)
    else {
      val repo = SharedRepositoryParser.repository(input)

      val url = repo.map {
        case m: MavenRepository =>
          m.getBase()
        case i: IvyRepository =>
          // FIXME We're not handling metadataPattern here
          constantPrefixOf(i.getPattern())
        case r =>
          sys.error(s"Unrecognized repository: $r")
      }

      val validatedUrl = url.flatMap { url0 =>
        try Right(new URL(url0))
        catch {
          case e: MalformedURLException =>
            val urlErrorMsg =
              "Error parsing URL " + url0 + Option(e.getMessage).fold("")(
                " (" + _ + ")"
              )

            if (url0.contains(File.separatorChar)) {
              val f = new File(url0)
              if (f.exists() && !f.isDirectory)
                Left(s"$urlErrorMsg, and $url0 not a directory")
              else
                Right(f.toURI.toURL)
            } else
              Left(urlErrorMsg)
        }
      }

      validatedUrl.flatMap { url =>
        Option(url.getUserInfo) match {
          case None =>
            repo
          case Some(userInfo) =>
            val (user, password) = userInfo.split(":", 2) match {
              case Array(user, password) => (user, password)
              case Array(user)           => (user, "")
            }

            val auth = Credentials
              .of(user, password)
              .withHttpsOnly(url.getProtocol != "http")

            val baseUrl = new java.net.URL(
              url.getProtocol,
              url.getHost,
              url.getPort,
              url.getFile
            ).toString

            repo.map {
              case _: MavenRepository =>
                MavenRepository.of(baseUrl).withCredentials(auth)
              case i: IvyRepository =>
                val prefix = constantPrefixOf(i.getPattern())
                val rest = i.getPattern().substring(prefix.length())
                IvyRepository
                  .of(baseUrl + rest, i.getMetadataPattern())
                  .withCredentials(auth)
                  .withDropInfoAttributes(i.getDropInfoAttributes())
              case r =>
                sys.error(s"Unrecognized repository: $r")
            }
        }
      }
    }

  // coursierapi's Repository model has no structured Pattern/Chunk type like
  // coursier-core's IvyRepository.pattern.chunks - patterns are plain strings.
  // This extracts the literal (non-placeholder, non-optional) prefix, e.g.
  // "https://host/repo/" out of "https://host/repo/[organisation]/[module]/...",
  // standing in for "the constant chunks preceding the first Var/Opt chunk".
  private def constantPrefixOf(pattern: String): String = {
    val idx = pattern.indexWhere(c => c == '[' || c == '(')
    if (idx < 0) pattern else pattern.substring(0, idx)
  }

  // Corresponds to coursier's default Ivy pattern:
  // [organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]
  private val ivyDefaultPattern =
    "[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]"

  // A JVM-only reimplementation of coursier-core's `coursier.LocalRepositories`,
  // built on `coursierapi` types instead of coursier-core's own `Repository` model.
  private object LocalRepositories {

    private def ivy2HomeUri: String = {
      val path = sys.props
        .get("coursier.ivy.home")
        .orElse(sys.props.get("ivy.home"))
        .getOrElse(sys.props("user.home") + "/.ivy2/")
      val str = new File(path).toURI().toString()
      if (str.endsWith("/")) str else str + "/"
    }

    def ivy2Local: IvyRepository =
      IvyRepository
        .of(ivy2HomeUri + "local/" + ivyDefaultPattern)
        .withDropInfoAttributes(true)

    object Dangerous {
      def ivy2Cache: IvyRepository =
        IvyRepository
          .of(
            ivy2HomeUri + "cache/" +
              "(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/[type]s/[artifact]-[revision](-[classifier]).[ext]",
            ivy2HomeUri + "cache/" +
              "(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/[type]-[revision](-[classifier]).[ext]"
          )
          .withDropInfoAttributes(true)

      def maven2Local: MavenRepository = {
        val str = new File(sys.props("user.home")).toURI().toString()
        val homeUri = if (str.endsWith("/")) str else str + "/"
        MavenRepository.of(homeUri + ".m2/repository")
      }
    }
  }

  // A JVM-only reimplementation of coursier-core's `coursier.internal.SharedRepositoryParser`,
  // built on `coursierapi` types instead of coursier-core's own `Repository` model.
  private object SharedRepositoryParser {

    def repository(s: String): Either[String, Repository] =
      if (s == "central")
        Right(Repositories.central)
      else if (s.startsWith("sonatype:"))
        Right(Repositories.sonatype(s.stripPrefix("sonatype:")))
      else if (s.startsWith("sonatype-s01:"))
        Right(Repositories.sonatypeS01(s.stripPrefix("sonatype-s01:")))
      else if (s.startsWith("bintray:")) {
        val s0 = s.stripPrefix("bintray:")
        val id =
          if (s.contains("/")) s0
          else s0 + "/maven"

        Right(Repositories.bintray(id))
      } else if (s.startsWith("bintray-ivy:"))
        Right(Repositories.bintrayIvy(s.stripPrefix("bintray-ivy:")))
      else if (s.startsWith("typesafe:ivy-"))
        Right(Repositories.typesafeIvy(s.stripPrefix("typesafe:ivy-")))
      else if (s.startsWith("typesafe:"))
        Right(Repositories.typesafe(s.stripPrefix("typesafe:")))
      else if (s.startsWith("sbt-maven:"))
        Right(Repositories.sbtMaven(s.stripPrefix("sbt-maven:")))
      else if (s.startsWith("sbt-plugin:"))
        Right(Repositories.sbtPlugin(s.stripPrefix("sbt-plugin:")))
      else if (s == "scala-integration" || s == "scala-nightlies")
        Right(Repositories.scalaIntegration)
      else if (s.startsWith("ivy:")) {
        val s0 = s.stripPrefix("ivy:")
        val sepIdx = s0.indexOf('|')
        if (sepIdx < 0)
          Right(IvyRepository.of(s0))
        else {
          val mainPart = s0.substring(0, sepIdx)
          val metadataPart = s0.substring(sepIdx + 1)
          Right(IvyRepository.of(mainPart, metadataPart))
        }
      } else if (s == "jitpack")
        Right(Repositories.jitpack)
      else if (s == "clojars")
        Right(Repositories.clojars)
      else if (s == "jcenter")
        Right(Repositories.jcenter)
      else if (s == "google")
        Right(Repositories.google)
      else if (s == "gcs")
        Right(Repositories.centralGcs)
      else if (s == "gcs-eu")
        Right(Repositories.centralGcsEu)
      else if (s == "gcs-asia")
        Right(Repositories.centralGcsAsia)
      else if (s.startsWith("apache:"))
        Right(Repositories.apache(s.stripPrefix("apache:")))
      else
        Right(MavenRepository.of(s))

  }

}
