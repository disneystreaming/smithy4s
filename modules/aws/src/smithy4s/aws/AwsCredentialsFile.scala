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

package smithy4s.aws

import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.all._
import java.nio.file.Paths
import smithy4s.aws.kernel.AWS_ACCESS_KEY_ID
import smithy4s.aws.kernel.AWS_SECRET_ACCESS_KEY
import smithy4s.aws.kernel.AWS_SESSION_TOKEN
import smithy4s.aws.kernel.AwsCredentials

final case class AwsCredentialsFile(
    default: Option[AwsCredentials],
    profiles: Map[String, AwsCredentials]
)

object AwsCredentialsFile {
  def loadFromDisk[F[_]](home: String, profile: Option[String])(implicit
      F: Sync[F]
  ): F[AwsCredentials] = {
    loadFile(home)
      .use(lines => processFileLines(lines).pure[F])
      .flatMap(creds =>
        profile
          .map(p =>
            creds.profiles
              .get(p)
              .toRight(
                new RuntimeException(
                  s"Profile `$p` was not found in the credentials file."
                )
              )
              .liftTo[F]
          )
          .getOrElse(
            creds.default
              .toRight(
                new RuntimeException(
                  s"No default profile is available in the credentials file."
                )
              )
              .liftTo[F]
          )
      )
  }

  private[aws] def processFileLines(lines: List[String]): AwsCredentialsFile = {
    def inProfile(
        rest: List[String],
        currentProfile: String,
        data: Map[String, String]
    ): (Map[String, String], List[String]) = {
      rest match {
        case Nil =>
          (data, Nil)
        case head :: _ if head.trim().startsWith("[") =>
          (data, rest)
        case head :: next =>
          val parts = head.split("=")
          val key = parts(0).trim().toLowerCase
          val value = parts(1).trim().toLowerCase()
          val updated = data + (key -> value)
          inProfile(next, currentProfile, updated)
      }
    }

    def lookingForProfile(
        rest: List[String],
        data: Map[String, Map[String, String]]
    ): Map[String, Map[String, String]] = {
      rest match {
        case head :: next =>
          val profile = head.trim() match {
            case Profile.Prefixed(profile)    => profile
            case Profile.Default(profile)     => profile
            case Profile.NonPrefixed(profile) => profile
          }

          val (profileData, rest) = inProfile(next, profile, Map.empty)
          lookingForProfile(rest, data + (profile -> profileData))
        case Nil =>
          data
      }
    }

    val data = lookingForProfile(lines.filter(_.trim.nonEmpty), Map.empty)
    AwsCredentialsFile.profilesFromMap(data)
  }

  private def loadFile[F[_]: Sync](home: String): Resource[F, List[String]] =
    Resource
      .fromAutoCloseable(
        Sync[F].delay(
          scala.io.Source
            .fromFile(Paths.get(home, ".aws", "credentials").toUri())
        )
      )
      .map(_.getLines().toList)

  private def credentialsFromMap(data: Map[String, String]): AwsCredentials =
    AwsCredentials.Default(
      data(AWS_ACCESS_KEY_ID.toLowerCase()),
      data(AWS_SECRET_ACCESS_KEY.toLowerCase()),
      data.get(AWS_SESSION_TOKEN.toLowerCase())
    )

  private def profilesFromMap(
      dataPerProfile: Map[String, Map[String, String]]
  ): AwsCredentialsFile = {
    val default = dataPerProfile.get("default").map(credentialsFromMap)
    val others = dataPerProfile
      .filterNot(_._1 == "default")
      .map { case (profile, data) =>
        profile -> credentialsFromMap(data)
      }
    AwsCredentialsFile(default, others)
  }
}

object Profile {
  private val profileMatch = "([\\w_-]*)"
  object Default {
    def unapply(s: String): Option[String] =
      if (s == "[default]") Some("default") else None
  }

  object Prefixed {
    private val reg = s"^\\[profile $profileMatch\\]$$".r
    def unapply(s: String): Option[String] = s match {
      case reg(first) => Some(first)
      case _          => None
    }
  }

  object NonPrefixed {
    private val reg = s"^\\[$profileMatch\\]$$".r
    def unapply(s: String): Option[String] = s match {
      case reg(first) => Some(first)
      case _          => None
    }
  }
}
