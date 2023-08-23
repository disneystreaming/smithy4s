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
package internals

import cats.effect.Concurrent
import fs2.compression.Compression
import org.http4s.Entity
import smithy4s.Endpoint
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.xml.internals.XmlStartingPath
import smithy4s.interopcats._

// scalafmt: {maxColumn = 120}

private[aws] object AwsRestXmlCodecs {

  def make[F[_]: Concurrent: Compression](): HttpUnaryClientCodecs.Make[F, Entity[F]] = {

    val mediaWriters = CachedSchemaCompiler.getOrElse(stringAndBlobRequestWriters[F], xmlRequestWriters[F])
    val mediaReaders = CachedSchemaCompiler.getOrElse(stringAndBlobResponseReaders[F], xmlResponseReaders[F])

    val requestWriters = HttpRequest.Encoder.restSchemaCompiler[Entity[F]](
      Metadata.AwsEncoder,
      mediaWriters
    )

    val responseReaders = HttpResponse.Decoder.restSchemaCompilerAux[F, Entity[F]](
      Metadata.AwsDecoder,
      mediaReaders
    )

    new HttpUnaryClientCodecs.Make[F, Entity[F]] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): HttpUnaryClientCodecs[F, Entity[F], I, E, O] = {
        val addCompression = applyCompression[F](endpoint.hints)
        val finalRequestWriters = addCompression(requestWriters)

        val errorResponseReaders = responseReaders.contramapSchema(
          smithy4s.schema.Schema.transformHintsLocallyK(
            _.addMemberHints(XmlStartingPath(List("ErrorResponse", "Error")))
          )
        )
        val discriminator = AwsErrorTypeDecoder.fromResponse(errorResponseReaders)

        val make = HttpUnaryClientCodecs.Make[F, Entity[F]](
          finalRequestWriters,
          responseReaders,
          errorResponseReaders,
          discriminator,
          toStrict
        )
        make.apply(endpoint)
      }
    }
  }

}
