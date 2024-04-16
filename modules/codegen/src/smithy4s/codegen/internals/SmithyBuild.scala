/*
 *  Copyright 2021-2024 Disney Streaming
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

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.openapi.OpenApiConfig

import scala.collection.Seq
import scala.reflect.ClassTag
import scala.util.Try

private[internals] final case class SmithyBuild(
    version: String,
    imports: Seq[os.FilePath],
    plugins: Seq[SmithyBuildPlugin],
    maven: Option[SmithyBuildMaven]
) {
  def getPlugin[T <: SmithyBuildPlugin](implicit
      classTag: ClassTag[T]
  ): Option[T] =
    plugins.collectFirst { case t: T => t }
}

private[codegen] object SmithyBuild {
  // automatically map absence of value to empty Seq in order to clean up the case class API for later use
  implicit def optionalSeqDecoder[T](implicit base: Decoder[T]): Decoder[Seq[T]] =
    Decoder.decodeOption(Decoder.decodeSeq[T]).map(_.getOrElse(Seq.empty))

  implicit val pathDecoder: Decoder[os.FilePath] =
    Decoder.decodeString.emapTry { raw =>
      Try(os.FilePath(raw))
    }

  implicit val pluginDecoder: Decoder[Seq[SmithyBuildPlugin]] = (c: HCursor) =>
    c.keys match {
      case None => DecodingFailure("Expected JSON object", c.history).asLeft
      case Some(keys) =>
        keys.toList.traverse(key => c.get(key)(SmithyBuildPlugin.decode(key)))
    }

  case class Serializable(
      version: String,
      imports: Seq[String],
      maven: SmithyBuildMaven
  )

  implicit val decoder: Decoder[SmithyBuild] = deriveDecoder

  implicit val serializableEncoder: Encoder[Serializable] = deriveEncoder

  def writeJson(sb: SmithyBuild.Serializable): String = sb.asJson.spaces4

  def readJson(in: String): SmithyBuild = parser
    .decode[SmithyBuild](in)
    .left
    .map(err =>
      throw new IllegalArgumentException(
        s"Input is not a valid smithy-build.json file",
        err
      )
    )
    .merge
}

private[internals] final case class SmithyBuildMaven(
    dependencies: Seq[String],
    repositories: Seq[SmithyBuildMavenRepository]
)
private[codegen] object SmithyBuildMaven {
  implicit val codecs: Codec[SmithyBuildMaven] = deriveCodec
}

private[internals] final case class SmithyBuildMavenRepository(
    url: String
)
private[codegen] object SmithyBuildMavenRepository {
  implicit val codecs: Codec[SmithyBuildMavenRepository] = deriveCodec
}

private[codegen] sealed trait SmithyBuildPlugin

private[codegen] object SmithyBuildPlugin {
  def decode(key: String): Decoder[SmithyBuildPlugin] = key match {
    case "openapi" => Decoder[OpenApi].widen
    case other =>
      Decoder.failedWithMessage(
        s"Plugin $other is not supported by smithy4s. Currently supported plugins: openapi"
      )
  }

  case class OpenApi(config: OpenApiConfig) extends SmithyBuildPlugin

  object OpenApi {
    implicit val decoder: Decoder[OpenApi] = Decoder[JsonObject].emapTry {
      obj =>
        Try {
          val config = OpenApiConfig.fromNode(Node.parse(obj.toJson.noSpaces))
          OpenApi(config)
        }
    }
  }
}
