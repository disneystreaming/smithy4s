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
import fs2._
import fs2.data.xml._
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.Writer

private[smithy4s] final case class XmlByteStreamEncoderCompilerImpl[F[_]]()
    extends CachedSchemaCompiler.Impl[Xml.XmlByteStreamEncoder[F, *]]
    with XmlByteStreamEncoderCompiler[F] {

  type Aux[A] = Xml.XmlByteStreamEncoder[F, A]

  def fromSchema[A](
      schema: Schema[A],
      cache: Cache
  ): Xml.XmlByteStreamEncoder[F, A] = new Writer[Any, Stream[F, Byte], A] {
    // TODO: Does cache need to be passed here
    val fa = XmlDocument.Encoder.fromSchema(schema)

    def write(input: Any, a: A): Stream[F, Byte] = {
      XmlDocument.documentEventifier
        .eventify(fa.encode(a))
        .through(render(collapseEmpty = false))
        .through(fs2.text.utf8.encode[F])
    }
  }
}
