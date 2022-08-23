import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin, MimaPlugin.autoImport._

import sbtprojectmatrix._, ProjectMatrixPlugin.autoImport._

import scala.sys.process._
import com.github.sbt.git.GitPlugin
import com.github.sbt.git.SbtGit.git
import scala.util.Try

// Adapted from https://github.com/djspiewak/sbt-spiewak
object MimaVersionPlugin extends AutoPlugin {

  override def requires =
    GitPlugin &&
      MimaPlugin &&
      ProjectMatrixPlugin &&
      plugins.JvmPlugin

  override def trigger = noTrigger

  object autoImport {
    val ReleaseTag = """^v((?:\d+\.){2}\d+(?:-.*)?)$""".r
    lazy val mimaBaseVersion = git.baseVersion
    lazy val mimaReportBinaryIssuesIfRelevant = taskKey[Unit](
      "A wrapper around the mima task which ensures publishArtifact is set to true"
    )
  }
  import autoImport._

  private val Description = """^.*-(\d+)-[a-zA-Z0-9]+$""".r

  private def filterTaskWhereRelevant(delegate: TaskKey[Unit]) = Def.taskDyn {
    if (publishArtifact.value) {
      Def.task(delegate.value)
    } else {
      Def.task(
        streams.value.log.warn(
          s"skipping `${delegate.key.label}` in ${name.value}: publishArtifact is set to false."
        )
      )
    }
  }

  override def buildSettings: Seq[Setting[_]] =
    GitPlugin.autoImport.versionWithGit ++ Seq(
      git.gitTagToVersionNumber := {
        case ReleaseTag(version) => Some(version)
        case _                   => None
      },
      git.formattedShaVersion := {
        val suffix = git.makeUncommittedSignifierSuffix(
          git.gitUncommittedChanges.value,
          git.uncommittedSignifier.value
        )

        val description = Try("git describe --tags --match v*".!!.trim).toOption
        val optDistance = description collect { case Description(distance) =>
          distance + "-"
        }

        val distance = optDistance.getOrElse("")

        git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
          autoImport.mimaBaseVersion.value + "-" + distance + sha + suffix
        }
      },
      git.gitUncommittedChanges := Try("git status -s".!!.trim.length > 0)
        .getOrElse(true),
      git.gitHeadCommit := Try("git rev-parse HEAD".!!.trim).toOption,
      git.gitCurrentTags := Try(
        "git tag --contains HEAD".!!.trim.split("\\s+").toList.filter(_ != "")
      ).toOption.toList.flatten
    )

  override def projectSettings: Seq[Setting[_]] = Seq(
    mimaReportBinaryIssuesIfRelevant := filterTaskWhereRelevant(
      mimaReportBinaryIssues
    ).value,
    mimaPreviousArtifacts := {
      val current = version.value
      val org = organization.value
      val n = moduleName.value

      val TagBase = """^(\d+)\.(\d+).*""" r
      val TagBase(major, minor) = mimaBaseVersion.value

      val isPre = major.toInt == 0

      val isJvm = virtualAxes.?.value
        .getOrElse(List.empty)
        .contains(
          VirtualAxis.jvm
        )

      if (sbtPlugin.value || !isJvm) {
        Set.empty
      } else {
        val tags = scala.util
          .Try("git tag --list".!!.split("\n").map(_.trim))
          .getOrElse(new Array[String](0))

        // in semver, we allow breakage in minor releases if major is 0, otherwise not
        val Pattern =
          if (isPre)
            s"^v($major\\.$minor\\.\\d+)$$".r
          else
            s"^v($major\\.\\d+\\.\\d+)$$".r

        val versions = tags collect { case Pattern(version) =>
          version
        }

        val notCurrent = versions.filterNot(current ==)

        notCurrent
          .map(v =>
            org % s"${n}_${CrossVersion.binaryScalaVersion(scalaVersion.value)}" % v
          )
          .toSet
      }
    }
  )
}
