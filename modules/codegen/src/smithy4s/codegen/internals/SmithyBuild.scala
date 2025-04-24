/*
 *  Copyright 2021-2025 Disney Streaming
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
import io.circe.generic.semiauto._
import io.circe.syntax._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.openapi.OpenApiConfig

import scala.collection.Set
import scala.reflect.ClassTag
import scala.util.Try

private[internals] final case class SmithyBuild(
    version: String,
    sources: Set[os.FilePath],
    plugins: Set[SmithyBuildPlugin],
    maven: Option[SmithyBuildMaven]
) {
  def getPlugin[T <: SmithyBuildPlugin](implicit
      classTag: ClassTag[T]
  ): Option[T] =
    plugins.collectFirst { case t: T => t }
}

private[codegen] object SmithyBuild {
  // automatically map absence of value to empty Seq for ease of use
  implicit def optionalSetDecoder[T](implicit
      base: Decoder[T]
  ): Decoder[Set[T]] =
    Decoder.decodeOption(Decoder.decodeSet[T]).map(_.getOrElse(Set.empty))

  implicit val pathDecoder: Decoder[os.FilePath] =
    Decoder.decodeString.emapTry { raw =>
      Try(os.FilePath(raw))
    }

  implicit val pluginDecoder: Decoder[Set[SmithyBuildPlugin]] = Decoder
    .decodeOption { (c: HCursor) =>
      c.keys match {
        case None => DecodingFailure("Expected JSON object", c.history).asLeft
        case Some(keys) =>
          keys.toList
            .traverse(key => c.get(key)(SmithyBuildPlugin.decode(key)))
            .map(_.toSet)
      }
    }
    .map(_.getOrElse(Set.empty))

  /* Class containing only the subset of the smithy-build.json properties that need
   * to be serialized when creating a smithy-build.json file. Allows us to skip
   * things that are both unnecessary and very complicated to serialize,
   * such as OpenApiConfig.
   */
  case class Serializable(
      version: String,
      sources: Set[String],
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
        s"Input is not a valid smithy-build.json file: ${err.getMessage}",
        err
      )
    )
    .merge
}

private[internals] final case class SmithyBuildMaven(
    dependencies: Set[String],
    repositories: Set[SmithyBuildMavenRepository]
)
private[codegen] object SmithyBuildMaven {
  import SmithyBuild.optionalSetDecoder

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
    private val nodeFolder: Json.Folder[Node] = new Json.Folder[Node] {
      import scala.jdk.CollectionConverters._
      override def onNull: Node = Node.nullNode()

      override def onBoolean(value: Boolean): Node = Node.from(value)

      override def onNumber(value: JsonNumber): Node =
        // try to avoid rounding errors from double conversion if we possibly can
        value.toInt
          .map(Node.from(_))
          .orElse(value.toLong.map(Node.from(_)))
          .getOrElse(Node.from(value.toDouble))

      override def onString(value: String): Node = Node.from(value)

      override def onArray(value: Vector[Json]): Node =
        Node.arrayNode(value.map(_.foldWith(this)): _*)

      override def onObject(value: JsonObject): Node =
        Node.objectNode(value.toMap.map { case (name, json) =>
          Node.from(name) -> json.foldWith(this)
        }.asJava)
    }

    implicit val decoder: Decoder[OpenApi] = Decoder[Json].emapTry { obj =>
      Try {
        val config = OpenApiConfig.fromNode(obj.foldWith(nodeFolder))
        OpenApi(config)
      }
    }
  }
}
