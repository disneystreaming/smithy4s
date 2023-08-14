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

package smithy4s.xml
package internals

import smithy4s.schema.Schema
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.Reader
import fs2._
import fs2.data.xml._
import fs2.data.xml.dom._
import cats.effect.Concurrent
import cats.syntax.all._
import smithy4s.interopfs2._

private[smithy4s] final case class XmlByteStreamDecoderCompilerImpl[
    F[_]: Concurrent
]() extends CachedSchemaCompiler.Impl[ByteStreamDecoder[F, *]]
    with XmlByteStreamDecoderCompiler[F] {

  type Aux[A] = ByteStreamDecoder[F, A]

  def fromSchema[A](
      schema: Schema[A],
      cache: Cache
  ): ByteStreamDecoder[F, A] =
    new Reader[F, Stream[F, Byte], A] {
      val fa = XmlDocument.Decoder.fromSchema(schema)

      def read(bytes: Stream[F, Byte]): F[A] = {
        bytes
          .through(fs2.text.utf8.decode[F])
          .through(events[F, String]())
          .through(referenceResolver())
          .through(normalize)
          .through(documents[F, XmlDocument])
          .head
          .compile
          .last
          .map(
            _.getOrElse(
              XmlDocument(
                XmlDocument.XmlElem(
                  XmlDocument.XmlQName(None, "Unit"),
                  List.empty,
                  List.empty
                )
              )
            )
          )
          .flatMap(in => Concurrent[F].fromEither(fa.decode(in)))
      }
    }
}
