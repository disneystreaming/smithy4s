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
) {

  /**
    * Selects the credentials of the given profile, or the default profile if
    * none is given. The default profile can also be requested by name.
    */
  private[aws] def select(
      profile: Option[String]
  ): Either[AwsCredentialsFileException, AwsCredentials] =
    profile match {
      case Some("default") | None =>
        default.toRight(
          AwsCredentialsFileException(
            "No default profile is available in the credentials file."
          )
        )
      case Some(p) =>
        profiles
          .get(p)
          .toRight(
            AwsCredentialsFileException(
              s"Profile `$p` was not found in the credentials file."
            )
          )
    }
}

object AwsCredentialsFile {
  private type LoadCredentials[A] = Either[AwsCredentialsFileException, A]

  def fromDisk[F[_]](path: fs2.io.file.Path, profile: Option[String])(implicit
      F: Concurrent[F],
      Files: Files[F]
  ): F[AwsCredentials] = {
    for {
      lines <- Files.readUtf8Lines(path).compile.toList
      creds <- processFileLines(lines).liftTo[F]
      finalProfile <- creds.select(profile).liftTo[F]
    } yield finalProfile
  }

  private[aws] def processFileLines(
      lines: List[String]
  ): LoadCredentials[AwsCredentialsFile] =
    parseProfiles(lines).flatMap(profilesFromMap)

  private def withoutComments(line: String): String =
    line.replaceAll("#.+", "")

  private def parseProfiles(
      lines: List[String]
  ): LoadCredentials[Map[String, Map[String, String]]] = {
    // The line's content is deliberately left out: it may well be a secret.
    def parseError(lineNumber: Int, reason: String) =
      AwsCredentialsFileException(
        s"Unable to parse credentials file at line $lineNumber: $reason."
      )

    type State = (Option[String], Map[String, Map[String, String]])

    lines.zipWithIndex
      .map { case (line, i) => (withoutComments(line).trim(), i + 1) }
      .filter { case (content, _) => content.nonEmpty }
      .foldLeftM[LoadCredentials, State]((None, Map.empty)) {
        case ((current, data), (content, lineNumber)) =>
          content match {
            case Profile.Header(profile) =>
              val existing = data.getOrElse(profile, Map.empty[String, String])
              Right((Some(profile), data.updated(profile, existing)))
            case _ =>
              current match {
                case None =>
                  Left(
                    parseError(
                      lineNumber,
                      "expected a profile header like `[default]`"
                    )
                  )
                case Some(profile) =>
                  content.split("=", 2) match {
                    case Array(key, value) =>
                      val updated =
                        data(profile) + (key.trim().toLowerCase -> value.trim())
                      Right((current, data.updated(profile, updated)))
                    case _ =>
                      Left(
                        parseError(
                          lineNumber,
                          "expected a `key = value` pair"
                        )
                      )
                  }
              }
          }
      }
      .map(_._2)
  }

  private def credentialsFromMap(
      profile: String,
      data: Map[String, String]
  ): LoadCredentials[AwsCredentials] = {
    def required(key: String): LoadCredentials[String] =
      data
        .get(key.toLowerCase())
        .toRight(
          AwsCredentialsFileException(
            s"'${key.toLowerCase()}' is missing from the data of profile `$profile`."
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
        .traverse(credentialsFromMap("default", _))
    val othersF: LoadCredentials[Map[String, AwsCredentials]] = dataPerProfile
      .filterNot(_._1 == "default")
      .toList
      .traverse { case (profile, data) =>
        credentialsFromMap(profile, data).tupleLeft(profile)
      }
      .map(_.toMap)

    (defaultF, othersF).mapN(AwsCredentialsFile.apply)
  }
}

private object Profile {
  private val profileMatch = "(.*)"

  object Header {
    def unapply(s: String): Option[String] =
      Default
        .unapply(s)
        .orElse(Prefixed.unapply(s))
        .orElse(NonPrefixed.unapply(s))
  }

  object Default {
    def unapply(s: String): Option[String] =
      if (s == "[default]") Some("default") else None
  }

  object Prefixed {
    private val reg = s"^\\[profile $profileMatch\\]$$".r
    def unapply(s: String): Option[String] = s match {
      case reg(first) => Some(first.trim())
      case _          => None
    }
  }

  object NonPrefixed {
    private val reg = s"^\\[$profileMatch\\]$$".r
    def unapply(s: String): Option[String] = s match {
      case reg(first) => Some(first.trim())
      case _          => None
    }
  }
}
