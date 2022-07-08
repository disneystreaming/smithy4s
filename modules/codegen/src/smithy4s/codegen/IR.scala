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

package smithy4s.codegen

import cats.Functor
import cats.data.NonEmptyList
import cats.syntax.all._
import smithy4s.codegen.TypedNode.FieldTN.OptionalNoneTN
import smithy4s.codegen.TypedNode.FieldTN.OptionalSomeTN
import smithy4s.codegen.TypedNode.FieldTN.RequiredTN
import smithy4s.recursion._
import software.amazon.smithy.model.node.Node
import smithy4s.codegen.TypedNode.AltValueTN.ProductAltTN
import smithy4s.codegen.TypedNode.AltValueTN.TypeAltTN
import smithy4s.codegen.UnionMember._

case class CompilationUnit(namespace: String, declarations: List[Decl])

sealed trait Decl {
  def name: String
  def hints: List[Hint]
}

case class Service(
    name: String,
    originalName: String,
    ops: List[Operation],
    hints: List[Hint],
    version: String
) extends Decl

case class Operation(
    name: String,
    originalNamespace: String,
    params: List[Field],
    input: Type,
    errors: List[Type],
    output: Type,
    streamedInput: Option[StreamingField],
    streamedOutput: Option[StreamingField],
    hints: List[Hint] = Nil
)

case class Product(
    name: String,
    originalName: String,
    fields: List[Field],
    recursive: Boolean = false,
    hints: List[Hint] = Nil
) extends Decl

case class Union(
    name: String,
    originalName: String,
    alts: NonEmptyList[Alt],
    recursive: Boolean = false,
    hints: List[Hint] = Nil
) extends Decl

case class TypeAlias(
    name: String,
    originalName: String,
    tpe: Type,
    hints: List[Hint] = Nil
) extends Decl

case class Enumeration(
    name: String,
    originalName: String,
    values: List[EnumValue],
    hints: List[Hint] = Nil
) extends Decl
case class EnumValue(
    value: String,
    ordinal: Int,
    name: Option[String],
    hints: List[Hint] = Nil
)

case class Field(
    name: String,
    realName: String,
    tpe: Type,
    required: Boolean,
    hints: List[Hint]
)

case class StreamingField(
    name: String,
    tpe: Type,
    hints: List[Hint]
)

object Field {

  def apply(
      name: String,
      tpe: Type,
      required: Boolean = true,
      hints: List[Hint] = Nil
  ): Field =
    Field(name, name, tpe, required, hints)

}

sealed trait UnionMember {
  def update(f: Product => Product)(g: Type => Type): UnionMember = this match {
    case TypeCase(tpe)        => TypeCase(g(tpe))
    case ProductCase(product) => ProductCase(f(product))
    case UnitCase             => UnitCase
  }
}
object UnionMember {
  case class ProductCase(product: Product) extends UnionMember
  case object UnitCase extends UnionMember
  case class TypeCase(tpe: Type) extends UnionMember
}

case class Alt(
    name: String,
    realName: String,
    member: UnionMember,
    hints: List[Hint]
)

object Alt {

  def apply(
      name: String,
      member: UnionMember,
      hints: List[Hint] = Nil
  ): Alt = Alt(name, name, member, hints)

}

sealed trait Type {
  def dealiased: Type = this match {
    case Type.Alias(_, _, tpe) => tpe.dealiased
    case other                 => other
  }
}

sealed trait Primitive {
  type T
}
object Primitive {
  type Aux[TT] = Primitive { type T = TT }

  case object Unit extends Primitive { type T = Unit }
  case object ByteArray extends Primitive { type T = Array[Byte] }
  case object Bool extends Primitive { type T = Boolean }
  case object String extends Primitive { type T = String }
  case object Timestamp extends Primitive { type T = java.time.Instant }
  case object Uuid extends Primitive { type T = java.util.UUID }
  case object Byte extends Primitive { type T = Byte }
  case object Int extends Primitive { type T = Int }
  case object Short extends Primitive { type T = Short }
  case object Long extends Primitive { type T = Long }
  case object Float extends Primitive { type T = Float }
  case object Double extends Primitive { type T = Double }
  case object BigDecimal extends Primitive { type T = scala.math.BigDecimal }
  case object BigInteger extends Primitive { type T = scala.math.BigInt }
  case object Document extends Primitive { type T = Node }
  case object Nothing extends Primitive { type T = Nothing }
}

object Type {

  val unit = PrimitiveType(Primitive.Unit)

  case class List(member: Type) extends Type
  case class Set(member: Type) extends Type
  case class Map(key: Type, value: Type) extends Type
  case class Ref(namespace: String, name: String) extends Type {
    def show = namespace + "." + name
  }
  case class Alias(namespace: String, name: String, tpe: Type) extends Type
  case class PrimitiveType(prim: Primitive) extends Type
}

sealed trait Hint

object Hint {
  case object Trait extends Hint
  case object Error extends Hint
  case object PackedInputs extends Hint
  case class Constraint(tr: Type.Ref) extends Hint
  case class Protocol(traits: List[Type.Ref]) extends Hint
  // traits that get rendered generically
  case class Native(typedNode: Fix[TypedNode]) extends Hint
}

sealed trait Segment extends scala.Product with Serializable
object Segment {
  case class Label(value: String) extends Segment
  case class GreedyLabel(value: String) extends Segment
  case class Static(value: String) extends Segment
}

sealed trait NodeF[+A]

object NodeF {

  case class ArrayF[A](tpe: Type, values: List[A]) extends NodeF[A]
  case class ObjectF[A](tpe: Type, values: Vector[(String, A)]) extends NodeF[A]
  case class BooleanF(tpe: Type, bool: Boolean) extends NodeF[Nothing]
  case object NullF extends NodeF[Nothing]
  case class NumberF(tpe: Type, number: Number) extends NodeF[Nothing]
  case class StringF(tpe: Type, string: String) extends NodeF[Nothing]

}

sealed trait TypedNode[+A]
object TypedNode {
  sealed trait FieldTN[+A] {
    def map[B](f: A => B): FieldTN[B] = this match {
      case RequiredTN(value)     => RequiredTN(f(value))
      case OptionalSomeTN(value) => OptionalSomeTN(f(value))
      case OptionalNoneTN        => OptionalNoneTN
    }
  }
  object FieldTN {
    case class RequiredTN[A](value: A) extends FieldTN[A]
    case class OptionalSomeTN[A](value: A) extends FieldTN[A]
    case object OptionalNoneTN extends FieldTN[Nothing]
  }
  sealed trait AltValueTN[+A] {
    def map[B](f: A => B): AltValueTN[B] = this match {
      case ProductAltTN(value) => ProductAltTN(f(value))
      case TypeAltTN(value)    => TypeAltTN(f(value))
    }
  }
  object AltValueTN {
    case class ProductAltTN[A](value: A) extends AltValueTN[A]
    case class TypeAltTN[A](value: A) extends AltValueTN[A]
  }

  implicit val typedNodeFunctor: Functor[TypedNode] = new Functor[TypedNode] {
    def map[A, B](fa: TypedNode[A])(f: A => B): TypedNode[B] = fa match {
      case EnumerationTN(ref, value, ordinal, name) =>
        EnumerationTN(ref, value, ordinal, name)
      case StructureTN(ref, fields) =>
        StructureTN(ref, fields.map(_.map(_.map(f))))
      case NewTypeTN(ref, target) =>
        NewTypeTN(ref, f(target))
      case AltTN(ref, altName, alt) =>
        AltTN(ref, altName, alt.map(f))
      case MapTN(values) =>
        MapTN(values.map(_.leftMap(f).map(f)))
      case ListTN(values) =>
        ListTN(values.map(f))
      case SetTN(values) =>
        SetTN(values.map(f))
      case PrimitiveTN(prim, value) =>
        PrimitiveTN(prim, value)
    }
  }

  case class EnumerationTN(
      ref: Type.Ref,
      value: String,
      ordinal: Int,
      name: Option[String]
  ) extends TypedNode[Nothing]
  case class StructureTN[A](
      ref: Type.Ref,
      fields: List[(String, FieldTN[A])]
  ) extends TypedNode[A]
  case class NewTypeTN[A](ref: Type.Ref, target: A) extends TypedNode[A]
  case class AltTN[A](ref: Type.Ref, altName: String, alt: AltValueTN[A])
      extends TypedNode[A]
  case class MapTN[A](values: List[(A, A)]) extends TypedNode[A]
  case class ListTN[A](values: List[A]) extends TypedNode[A]
  case class SetTN[A](values: List[A]) extends TypedNode[A]
  case class PrimitiveTN[T](prim: Primitive.Aux[T], value: T)
      extends TypedNode[Nothing]

}
