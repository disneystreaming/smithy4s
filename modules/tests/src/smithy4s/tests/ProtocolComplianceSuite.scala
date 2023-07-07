/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.tests

import cats.effect.IO
import cats.effect.std.Env
import cats.syntax.all._
import fs2.io.file.Path
import smithy4s.{Blob, Document, Schema, ShapeId}
import smithy4s.compliancetests._
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.dynamic.model.Model
import smithy4s.dynamic.DynamicSchemaIndex.load
import smithy4s.codecs._
import weaver._
import fs2.Stream
import java.util.regex.Pattern

abstract class ProtocolComplianceSuite
    extends EffectSuite[IO]
    with BaseCatsSuite {

  implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun

  def getSuite: EffectSuite[IO] = this

  def allRules(dsi: DynamicSchemaIndex): IO[ComplianceTest[IO] => ShouldRun]
  def allTests(dsi: DynamicSchemaIndex): List[ComplianceTest[IO]]

  def spec(args: List[String]): fs2.Stream[IO, TestOutcome] = {
    val includeTest = Filters.filterTests(this.name)(args)
    fs2.Stream
      .eval(dynamicSchemaIndexLoader)
      .evalMap(index => allRules(index).map(_ -> allTests(index)))
      .flatMap { case (rules, tests) => Stream(tests: _*).map(rules -> _) }
      .flatMap { case (rules, test) =>
        if (includeTest(test.id)) Stream.emit((rules, test)) else Stream.empty
      }
      .flatMap { case (rules, test) =>
        runInWeaver(rules, test)
      }
  }

  def dynamicSchemaIndexLoader: IO[DynamicSchemaIndex]

  def genClientTests(
      impl: ReverseRouter[IO],
      shapeIds: ShapeId*
  )(dsi: DynamicSchemaIndex): List[ComplianceTest[IO]] =
    shapeIds.toList.flatMap(shapeId =>
      dsi
        .getService(shapeId)
        .toList
        .flatMap(wrapper => {
          HttpProtocolCompliance
            .clientTests(
              impl,
              wrapper.service
            )
        })
    )

  def genServerTests(
      impl: Router[IO],
      shapeIds: ShapeId*
  )(dsi: DynamicSchemaIndex): List[ComplianceTest[IO]] =
    shapeIds.toList.flatMap(shapeId =>
      dsi
        .getService(shapeId)
        .toList
        .flatMap(wrapper => {
          HttpProtocolCompliance
            .serverTests(
              impl,
              wrapper.service
            )
        })
    )

  def genClientAndServerTests(
      impl: ReverseRouter[IO] with Router[IO],
      shapeIds: ShapeId*
  )(dsi: DynamicSchemaIndex): List[ComplianceTest[IO]] =
    shapeIds.toList.flatMap(shapeId =>
      dsi
        .getService(shapeId)
        .toList
        .flatMap(wrapper => {
          HttpProtocolCompliance
            .clientAndServerTests(
              impl,
              wrapper.service
            )
        })
    )

  def loadDynamic(
      doc: Document
  ): Either[PayloadError, DynamicSchemaIndex] = {
    Document.decode[Model](doc).map(load)
  }
  private[smithy4s] def fileFromEnv(key: String): IO[Path] = Env
    .make[IO]
    .get(key)
    .flatMap(
      _.liftTo[IO](sys.error("MODEL_DUMP env var not set"))
        .map(fs2.io.file.Path(_))
    )

  def decodeDocument(
      bytes: Array[Byte],
      codecApi: PayloadCodec.CachedCompiler
  ): Document = {
    val codec: PayloadCodec[Document] = codecApi.fromSchema(Schema.document)
    codec.reader
      .decode(Blob(bytes))
      .getOrElse(sys.error("unable to decode smithy model into document"))

  }

  private def runInWeaver(
      rule: ComplianceTest[IO] => ShouldRun,
      tc: ComplianceTest[IO]
  ): Stream[IO, TestOutcome] = {
    val shouldRun = rule(tc)
    val runner: fs2.Stream[IO, IO[Expectations]] = {
      if (shouldRun == ShouldRun.Yes) {
        Stream {
          tc.run
            .map(res => expectSuccess(res))
            .attempt
            .map {
              case Right(expectations) => expectations
              case Left(throwable) =>
                Expectations.Helpers.failure(
                  s"unexpected error when running test ${throwable.getMessage} \n $throwable"
                )
            }
        }
      } else if (shouldRun == ShouldRun.No) { Stream.empty }
      else
        Stream {
          tc.run.attempt
            .map(_.fold(t => t.toString.invalidNel[Unit], identity))
            .map(res => unsureWhetherShouldSucceed(tc, res))
        }
    }

    runner.evalMap { runTest =>
      Test(
        tc.show,
        (log: Log[IO]) => tc.documentation.foldMap(log.info(_)) *> runTest
      )
    }
  }

  def expectSuccess(
      res: ComplianceTest.ComplianceResult
  ): Expectations = {
    res.toEither match {
      case Left(failures) => failures.foldMap(Expectations.Helpers.failure(_))
      case Right(_)       => Expectations.Helpers.success
    }
  }

  def unsureWhetherShouldSucceed(
      test: ComplianceTest[IO],
      res: ComplianceTest.ComplianceResult
  ): Expectations = {
    res.toEither match {
      case Left(failures) =>
        throw new weaver.CanceledException(
          Some(failures.head),
          weaver.SourceLocation.fromContext
        )
      case Right(_) =>
        throw new weaver.IgnoredException(
          Some("Passing unknown spec"),
          weaver.SourceLocation.fromContext
        )
    }
  }

}

// brought over from weaver https://github.com/disneystreaming/weaver-test/blob/d5489c994ecbe84f267550fb84c25c9fba473d70/modules/core/src/weaver/Filters.scala#L5
object Filters {

  def toPattern(filter: String): Pattern = {
    val parts = filter
      .split("\\*", -1)
      .map { // Don't discard trailing empty string, if any.
        case ""  => ""
        case str => Pattern.quote(str)
      }
    Pattern.compile(parts.mkString(".*"))
  }

  private type Predicate = TestName => Boolean

  private object atLine {
    def unapply(testPath: String): Option[(String, Int)] = {
      // Can't use string interpolation in pattern (2.12)
      val members = testPath.split(".line://")
      if (members.size == 2) {
        val suiteName = members(0)
        // Can't use .toIntOption (2.12)
        val maybeLine = scala.util.Try(members(1).toInt).toOption
        maybeLine.map(suiteName -> _)
      } else None
    }
  }

  def filterTests(
      suiteName: String
  )(args: List[String]): TestName => Boolean = {

    def toPredicate(filter: String): Predicate = {
      filter match {

        case atLine(`suiteName`, line) => { case TestName(_, indicator, _) =>
          indicator.line == line
        }
        case regexStr => { case TestName(name, _, _) =>
          val fullName = suiteName + "." + name
          toPattern(regexStr).matcher(fullName).matches()
        }
      }
    }

    import scala.util.Try
    val maybePattern = for {
      index <- Option(args.indexOf("-o"))
        .orElse(Option(args.indexOf("--only")))
        .filter(_ >= 0)
      filter <- Try(args(index + 1)).toOption
    } yield toPredicate(filter)
    testId => maybePattern.forall(_.apply(testId))
  }

}
