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

import cats.effect.Concurrent
import cats.syntax.all._
import fs2.io.file.Files
import smithy4s.aws.kernel.AWS_ACCESS_KEY_ID
import smithy4s.aws.kernel.AWS_SECRET_ACCESS_KEY
import smithy4s.aws.kernel.AWS_SESSION_TOKEN
import smithy4s.aws.kernel.AwsCredentials

final case class AwsCredentialsFileException(
    message: String,
    cause: Throwable = null
) extends RuntimeException(message, cause)

final case class AwsCredentialsFile(
    default: Option[AwsCredentials],
    profiles: Map[String, AwsCredentials]
)

object AwsCredentialsFile {
  private type LoadCredentials[A] = Either[AwsCredentialsFileException, A]

  def fromDisk[F[_]](path: fs2.io.file.Path, profile: Option[String])(implicit
      F: Concurrent[F],
      Files: Files[F]
  ): F[AwsCredentials] = {
    for {
      lines <- Files.readUtf8Lines(path).compile.toList
      creds <- processFileLines(lines).liftTo[F]

      defaultProfile = creds.default
      requestedProfile <- profile.traverse { p =>
        creds.profiles
          .get(p)
          .toRight(
            AwsCredentialsFileException(
              s"Profile `$p` was not found in the credentials file."
            )
          )
          .liftTo[F]
      }

      finalProfile <- requestedProfile
        .orElse(defaultProfile)
        .toRight(
          AwsCredentialsFileException(
            "No default profile is available in the credentials file."
          )
        )
        .liftTo[F]

    } yield finalProfile
  }

  private[aws] def processFileLines(
      lines: List[String]
  ): LoadCredentials[AwsCredentialsFile] = {
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
    Either
      .catchNonFatal(
        lookingForProfile(lines.filter(_.trim.nonEmpty), Map.empty)
      )
      .leftMap(ex =>
        AwsCredentialsFileException("Unable to parse credentials file.", ex)
      )
      .flatMap { data =>
        AwsCredentialsFile.profilesFromMap(data)
      }
  }

  private def credentialsFromMap(
      data: Map[String, String]
  ): LoadCredentials[AwsCredentials] = {
    def required(key: String): LoadCredentials[String] =
      data
        .get(key.toLowerCase())
        .toRight(
          AwsCredentialsFileException(
            s"'$key' is missing from the profile data."
          )
        )
    def optional(key: String): Option[String] =
      data.get(key.toLowerCase())
    (
      required(AWS_ACCESS_KEY_ID),
      required(AWS_SECRET_ACCESS_KEY),
      Right(optional(AWS_SESSION_TOKEN))
    ).mapN { case (key, secret, sessionToken) =>
      AwsCredentials.Default(key, secret, sessionToken)
    }
  }

  private def profilesFromMap(
      dataPerProfile: Map[String, Map[String, String]]
  ): LoadCredentials[AwsCredentialsFile] = {
    val defaultF: LoadCredentials[Option[AwsCredentials]] =
      dataPerProfile
        .get("default")
        .traverse(credentialsFromMap)
    val othersF: LoadCredentials[Map[String, AwsCredentials]] = dataPerProfile
      .filterNot(_._1 == "default")
      .toList
      .traverse { case (profile, data) =>
        credentialsFromMap(data).tupleLeft(profile)
      }
      .map(_.toMap)

    (defaultF, othersF).mapN(AwsCredentialsFile.apply)
  }
}

private object Profile {
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
