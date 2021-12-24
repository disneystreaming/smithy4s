import sbt.AutoPlugin
import sbt.Def
import sbt._
import _root_.io.circe
import cats.implicits._

object MergifyPlugin extends AutoPlugin {

  val generateMergifyYml = taskKey[String]("Generate .mergify.yml")

  val writeMergifyYml = taskKey[Unit]("Write .mergify.yml")

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    generateMergifyYml := Def.task {
      val ciJSON =
        circe.yaml.parser
          .parse(IO.read(file(".github") / "workflows" / "ci.yml"))
          .getOrElse(sys.error("Could not parse .github/workflows/ci.yml"))

      val matrix = Matrix.decoder
        .prepare(
          _.downField("jobs")
            .downField("build")
            .downField("strategy")
            .downField("matrix")
        )
        .decodeJson(ciJSON)
        .getOrElse(sys.error("Could not decode .github/workflows/ci.yml"))

      val jobs =
        (matrix.scalaVersion, matrix.ceVersion, matrix.scalaPlatform).tupled
          .filter { triplet =>
            !matrix.exclude.exists(ex => (ex.matches _).tupled(triplet))
          }

      import circe.syntax._

      val mergify = circe.Json.obj(
        "pull_request_rules" := circe.Json.obj(
          "name" := "Automatically merge Scala Steward PRs on CI success",
          "conditions" :=
            "author=scala-steward" +:
              "body~=labels:.*semver-patch.*" +:
              jobs.map { case (sc, ce, sp) =>
                s"""status-success="Test $ce $sc ($sp)""""
              },
          "actions" := circe.Json.obj(
            "merge" := circe.Json.obj(
              "method" := "merge"
            )
          )
        ) :: Nil
      )

      circe.yaml.printer.print(mergify)
    }.value,
    writeMergifyYml := Def.task {
      IO.write(file(".mergify.yml"), generateMergifyYml.value)
    }.value
  )

  case class Exclusion(
      scalaVersion: Option[String],
      ceVersion: Option[String],
      scalaPlatform: Option[String]
  ) {
    def matches(sv: String, ce: String, sp: String): Boolean =
      scalaVersion.forall(sv == _) &&
        ceVersion.forall(ce == _) &&
        scalaPlatform.forall(sp == _)
  }

  object Exclusion {
    implicit val exclusionDecoder: circe.Decoder[Exclusion] =
      circe.generic.semiauto.deriveDecoder
  }

  case class Matrix(
      scalaVersion: List[String],
      scalaPlatform: List[String],
      ceVersion: List[String],
      exclude: List[Exclusion]
  )
  object Matrix {
    val decoder: circe.Decoder[Matrix] =
      circe.generic.semiauto.deriveDecoder[Matrix]
  }
}
