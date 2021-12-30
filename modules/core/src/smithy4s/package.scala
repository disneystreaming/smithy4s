/*
 *  Copyright 2021 Disney Streaming
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

package object smithy4s extends TypeAliases {

  type Hint = Hints.Binding[_]
  type Static[A] = schematic.Static[A]
  type Schema[A] = schematic.Schema[Schematic, A]
  type StaticSchema[A] = Static[Schema[A]]
  type UnionSchematic[F[_]] = schematic.union.Schematic[F]
  type UnionSchema[A] = schematic.union.Schema[UnionSchematic, A]

  val errorTypeHeader = "X-Error-Type"

  def segment(s: Any): String = URIEncoderDecoder.encodeOthers(s.toString())
  def greedySegment(s: String) = s.split("/").map(segment).mkString("/")

  // Allows to "inject" F[_] types in places that require F[_,_,_,_,_]
  type GenLift[F[_]] = {
    type λ[I, E, O, SI, SO] = F[O]
  }

  def checkProtocol[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      service: Service[Alg, Op],
      protocolKey: Hints.Key[_]
  ): Either[UnsupportedProtocolError, Unit] =
    service.hints
      .get(protocolKey)
      .toRight(UnsupportedProtocolError(service, protocolKey))
      .map(_ => ())

}
