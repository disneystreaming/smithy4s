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

package object smithy4s {

  type Hint = Hints.Binding
  type Schema[A] = schema.Schema[A]
  val Schema: schema.Schema.type = schema.Schema

  type ~>[F[_], G[_]] = kinds.PolyFunction[F, G]

  type PayloadReader[A] = Reader[Either[PayloadError, *], Blob, A]
  type PayloadWriter[A] = Writer[Unit, Blob, A]
  type PayloadCodec[A] = kinds.TupleK[PayloadReader, PayloadWriter, A]

  def checkProtocol[Alg[_[_, _, _, _, _]]](
      service: Service[Alg],
      protocolTag: ShapeTag[_]
  ): Either[UnsupportedProtocolError, Unit] =
    service.hints
      .get(protocolTag)
      .toRight(UnsupportedProtocolError(service.id, protocolTag.id))
      .map(_ => ())

}
