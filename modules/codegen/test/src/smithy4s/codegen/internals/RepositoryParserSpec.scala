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

// Adapted from coursier's own repository parser tests, Copyright the coursier
// contributors, licensed under the Apache License, Version 2.0
// (http://www.apache.org/licenses/LICENSE-2.0):
// https://github.com/coursier/coursier/blob/main/modules/coursier/shared/src/test/scala/coursier/tests/parse/RepositoryParserTests.scala
// Ported from utest to munit (this module's test framework), and from
// coursier-core's `Repository` model to `coursierapi` types, to match
// `RepositoryParser.scala`. This whole file is a stopgap: it should be deleted
// once `RepositoryParser.scala` itself is deleted, i.e. once coursier-interface
// exposes an equivalent of coursier-core's alias/pattern repository parsing.
import coursierapi.IvyRepository
import coursierapi.MavenRepository
import coursierapi.Repository
import munit._

class RepositoryParserSpec extends FunSuite {

  private def isMavenRepo(repo: Repository): Boolean =
    repo match {
      case _: MavenRepository => true
      case _                  => false
    }

  private def isIvyRepo(repo: Repository): Boolean =
    repo match {
      case _: IvyRepository => true
      case _                => false
    }

  test("bintray-ivy:") {
    val obtained = RepositoryParser.repository("bintray-ivy:scalameta/maven")
    assert(obtained.exists(isIvyRepo))
  }

  test("bintray:") {
    val obtained = RepositoryParser.repository("bintray:scalameta/maven")
    assert(obtained.exists(isMavenRepo))
  }

  test("sbt-plugin:") {
    val res = RepositoryParser.repository("sbt-plugin:releases")
    assert(res.exists(isIvyRepo))
  }

  test("typesafe:ivy-") {
    val res = RepositoryParser.repository("typesafe:ivy-releases")
    assert(res.exists(isIvyRepo))
  }

  test("typesafe:") {
    val res = RepositoryParser.repository("typesafe:releases")
    assert(res.exists(isMavenRepo))
  }

  test("scala-nightlies") {
    val res = RepositoryParser.repository("scala-nightlies")
    assert(res.exists(isMavenRepo))
  }

  test("scala-integration") {
    val res = RepositoryParser.repository("scala-integration")
    assert(res.exists(isMavenRepo))
  }

  test("jitpack") {
    val res = RepositoryParser.repository("jitpack")
    assert(res.exists(isMavenRepo))
  }

  test("clojars") {
    val res = RepositoryParser.repository("clojars")
    assert(res.exists(isMavenRepo))
  }

  test("jcenter") {
    val res = RepositoryParser.repository("jcenter")
    assert(res.exists(isMavenRepo))
  }

  test("google") {
    val res = RepositoryParser.repository("google")
    assert(res.exists(isMavenRepo))
  }

  test("gcs") {
    val res = RepositoryParser.repository("gcs")
    assert(res.exists(isMavenRepo))
  }

  test("gcs-eu") {
    val res = RepositoryParser.repository("gcs-eu")
    assert(res.exists(isMavenRepo))
  }

  test("gcs-asia") {
    val res = RepositoryParser.repository("gcs-asia")
    assert(res.exists(isMavenRepo))
  }

  test("ivy with metadata") {
    val mainPattern =
      "http://repo/cache/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/[type]s/[artifact]-[revision](-[classifier]).[ext]"
    val metadataPattern =
      "http://repo/cache/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/[type]-[revision](-[classifier]).[ext]"

    val repo = s"ivy:$mainPattern|$metadataPattern"

    // Unlike coursier-core's `IvyRepository.parse` (which validates the pattern
    // and returns an `Either`), `coursierapi.IvyRepository.of` is a dumb
    // constructor that can't fail - see RepositoryParser.scala's own note on
    // why that matters here.
    val expected = IvyRepository.of(mainPattern, metadataPattern)

    val res = RepositoryParser.repository(repo)
    assertEquals(res, Right(expected))
  }

  test("apache: snapshots") {
    val res = RepositoryParser.repository("apache:snapshots")
    assert(res.exists(isMavenRepo))
  }

  test("apache: releases") {
    val res = RepositoryParser.repository("apache:releases")
    assert(res.exists(isMavenRepo))
  }

}
