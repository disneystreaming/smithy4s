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

package smithy4s
package dynamic
package internals

import model._

private[internals] trait ShapeVisitor[T] extends ((ShapeId, Shape) => T) {

  def apply(id: ShapeId, shape: Shape): T = shape match {
    case Shape.BlobCase(s)       => blobShape(id, s)
    case Shape.BooleanCase(s)    => booleanShape(id, s)
    case Shape.ListCase(s)       => listShape(id, s)
    case Shape.SetCase(s)        => setShape(id, s)
    case Shape.MapCase(s)        => mapShape(id, s)
    case Shape.ByteCase(s)       => byteShape(id, s)
    case Shape.ShortCase(s)      => shortShape(id, s)
    case Shape.IntegerCase(s)    => integerShape(id, s)
    case Shape.LongCase(s)       => longShape(id, s)
    case Shape.FloatCase(s)      => floatShape(id, s)
    case Shape.DocumentCase(s)   => documentShape(id, s)
    case Shape.DoubleCase(s)     => doubleShape(id, s)
    case Shape.BigIntegerCase(s) => bigIntegerShape(id, s)
    case Shape.BigDecimalCase(s) => bigDecimalShape(id, s)
    case Shape.OperationCase(s)  => operationShape(id, s)
    case Shape.ResourceCase(s)   => resourceShape(id, s)
    case Shape.ServiceCase(s)    => serviceShape(id, s)
    case Shape.StringCase(s)     => stringShape(id, s)
    case Shape.StructureCase(s)  => structureShape(id, s)
    case Shape.UnionCase(s)      => unionShape(id, s)
    case Shape.TimestampCase(s)  => timestampShape(id, s)
  }

  def blobShape(id: ShapeId, x: BlobShape): T
  def booleanShape(id: ShapeId, x: BooleanShape): T
  def listShape(id: ShapeId, x: ListShape): T
  def setShape(id: ShapeId, x: SetShape): T
  def mapShape(id: ShapeId, x: MapShape): T
  def byteShape(id: ShapeId, x: ByteShape): T
  def shortShape(id: ShapeId, x: ShortShape): T
  def integerShape(id: ShapeId, x: IntegerShape): T
  def longShape(id: ShapeId, x: LongShape): T
  def floatShape(id: ShapeId, x: FloatShape): T
  def documentShape(id: ShapeId, x: DocumentShape): T
  def doubleShape(id: ShapeId, x: DoubleShape): T
  def bigIntegerShape(id: ShapeId, x: BigIntegerShape): T
  def bigDecimalShape(id: ShapeId, x: BigDecimalShape): T
  def operationShape(id: ShapeId, x: OperationShape): T
  def resourceShape(id: ShapeId, x: ResourceShape): T
  def serviceShape(id: ShapeId, x: ServiceShape): T
  def stringShape(id: ShapeId, x: StringShape): T
  def structureShape(id: ShapeId, x: StructureShape): T
  def unionShape(id: ShapeId, x: UnionShape): T
  def timestampShape(id: ShapeId, x: TimestampShape): T
}

private[internals] object ShapeVisitor {

  trait Default[T] extends ShapeVisitor[T] {
    def default: T

    def blobShape(id: ShapeId, x: BlobShape): T = default
    def booleanShape(id: ShapeId, x: BooleanShape): T = default
    def listShape(id: ShapeId, x: ListShape): T = default
    def setShape(id: ShapeId, x: SetShape): T = default
    def mapShape(id: ShapeId, x: MapShape): T = default
    def byteShape(id: ShapeId, x: ByteShape): T = default
    def shortShape(id: ShapeId, x: ShortShape): T = default
    def integerShape(id: ShapeId, x: IntegerShape): T = default
    def longShape(id: ShapeId, x: LongShape): T = default
    def floatShape(id: ShapeId, x: FloatShape): T = default
    def documentShape(id: ShapeId, x: DocumentShape): T = default
    def doubleShape(id: ShapeId, x: DoubleShape): T = default
    def bigIntegerShape(id: ShapeId, x: BigIntegerShape): T = default
    def bigDecimalShape(id: ShapeId, x: BigDecimalShape): T = default
    def operationShape(id: ShapeId, x: OperationShape): T = default
    def resourceShape(id: ShapeId, x: ResourceShape): T = default
    def serviceShape(id: ShapeId, x: ServiceShape): T = default
    def stringShape(id: ShapeId, x: StringShape): T = default
    def structureShape(id: ShapeId, x: StructureShape): T = default
    def unionShape(id: ShapeId, x: UnionShape): T = default
    def timestampShape(id: ShapeId, x: TimestampShape): T = default
  }

}
