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
import smithy4s.codegen.LineSegment.{NameDef, NameRef}
import cats.kernel.Eq
import cats.Traverse
import cats.Applicative
import cats.Eval
import software.amazon.smithy.model.shapes.ShapeId

case class CompilationUnit(namespace: String, declarations: List[Decl])

sealed trait Decl {
  def shapeId: ShapeId
  def name: String
  def hints: List[Hint]
  def nameDef: NameDef = NameDef(name)
  def nameRef: NameRef = NameRef(List.empty, name)
}

case class Service(
    shapeId: ShapeId,
    name: String,
    ops: List[Operation],
    hints: List[Hint],
    version: String
) extends Decl

case class Operation(
    shapeId: ShapeId,
    name: String,
    methodName: String,
    params: List[Field],
    input: Type,
    errors: List[Type],
    output: Type,
    streamedInput: Option[StreamingField],
    streamedOutput: Option[StreamingField],
    hints: List[Hint] = Nil
)

case class Product(
    shapeId: ShapeId,
    name: String,
    fields: List[Field],
    mixins: List[Type],
    recursive: Boolean = false,
    hints: List[Hint] = Nil,
    isMixin: Boolean = false
) extends Decl

case class Union(
    shapeId: ShapeId,
    name: String,
    alts: NonEmptyList[Alt],
    recursive: Boolean = false,
    hints: List[Hint] = Nil
) extends Decl

case class TypeAlias(
    shapeId: ShapeId,
    name: String,
    tpe: Type,
    isUnwrapped: Boolean,
    recursive: Boolean = false,
    hints: List[Hint] = Nil
) extends Decl

case class Enumeration(
    shapeId: ShapeId,
    name: String,
    values: List[EnumValue],
    hints: List[Hint]
) extends Decl
case class EnumValue(
    value: String,
    intValue: Int,
    name: String,
    hints: List[Hint]
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
    case Type.Alias(_, _, tpe, _) => tpe.dealiased
    case other                    => other
  }

  def isResolved: Boolean = {
    val isUnwrapped = this match {
      case Type.Alias(_, _, _, unwrapped) => unwrapped
      case _                              => false
    }
    val isDealiased = dealiased == this
    isUnwrapped || isDealiased
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

  case class Collection(collectionType: CollectionType, member: Type)
      extends Type
  case class Map(key: Type, value: Type) extends Type
  case class Ref(namespace: String, name: String) extends Type {
    def show = namespace + "." + name
  }
  case class Alias(
      namespace: String,
      name: String,
      tpe: Type,
      isUnwrapped: Boolean
  ) extends Type
  case class PrimitiveType(prim: Primitive) extends Type
  case class ExternalType(
      name: String,
      fullyQualifiedName: String,
      providerImport: Option[String],
      underlyingTpe: Type,
      refinementHint: Hint.Native
  ) extends Type
}

sealed abstract class CollectionType(val tpe: NameRef)
object CollectionType {
  case object List extends CollectionType(NameRef("scala.List"))
  case object Set
      extends CollectionType(NameRef("scala.collection.immutable.Set"))
  case object Vector extends CollectionType(NameRef("scala.Vector"))
  case object IndexedSeq extends CollectionType(NameRef("scala.IndexedSeq"))
}

sealed trait Hint

object Hint {
  case object Trait extends Hint
  case object Error extends Hint
  case object PackedInputs extends Hint
  case object NoDefault extends Hint
  case object ErrorMessage extends Hint
  case class Constraint(tr: Type.Ref, native: Native) extends Hint
  case class Protocol(traits: List[Type.Ref]) extends Hint
  case class Default(typedNode: Fix[TypedNode]) extends Hint
  case class Deprecated(message: Option[String], since: Option[String])
      extends Hint
  // traits that get rendered generically
  case class Native(typedNode: Fix[TypedNode]) extends Hint
  case object IntEnum extends Hint

  sealed trait SpecializedList extends Hint
  object SpecializedList {
    case object Vector extends SpecializedList
    case object IndexedSeq extends SpecializedList
  }
  case object UniqueItems extends Hint

  implicit val eq: Eq[Hint] = Eq.fromUniversalEquals
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
    implicit val fieldTNTraverse: Traverse[FieldTN] = new Traverse[FieldTN] {
      def traverse[G[_]: Applicative, A, B](
          fa: FieldTN[A]
      )(f: A => G[B]): G[FieldTN[B]] =
        fa match {
          case RequiredTN(value)     => f(value).map(RequiredTN(_))
          case OptionalSomeTN(value) => f(value).map(OptionalSomeTN(_))
          case OptionalNoneTN        => Applicative[G].pure(OptionalNoneTN)
        }
      def foldLeft[A, B](fa: FieldTN[A], b: B)(f: (B, A) => B): B = ???
      def foldRight[A, B](fa: FieldTN[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] = ???
    }

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
    implicit val altValueTNTraverse: Traverse[AltValueTN] =
      new Traverse[AltValueTN] {
        def traverse[G[_]: Applicative, A, B](
            fa: AltValueTN[A]
        )(f: A => G[B]): G[AltValueTN[B]] =
          fa match {
            case ProductAltTN(value) => f(value).map(ProductAltTN(_))
            case TypeAltTN(value)    => f(value).map(TypeAltTN(_))
          }
        def foldLeft[A, B](fa: AltValueTN[A], b: B)(f: (B, A) => B): B = ???
        def foldRight[A, B](fa: AltValueTN[A], lb: Eval[B])(
            f: (A, Eval[B]) => Eval[B]
        ): Eval[B] = ???
      }

    case class ProductAltTN[A](value: A) extends AltValueTN[A]
    case class TypeAltTN[A](value: A) extends AltValueTN[A]
  }

  implicit val typedNodeTraverse: Traverse[TypedNode] =
    new Traverse[TypedNode] {
      def traverse[G[_], A, B](
          fa: TypedNode[A]
      )(f: A => G[B])(implicit F: Applicative[G]): G[TypedNode[B]] = fa match {
        case EnumerationTN(ref, value, intValue, name) =>
          F.pure(EnumerationTN(ref, value, intValue, name))
        case StructureTN(ref, fields) =>
          fields.traverse(_.traverse(_.traverse(f))).map(StructureTN(ref, _))
        case NewTypeTN(ref, target) =>
          f(target).map(NewTypeTN(ref, _))
        case AltTN(ref, altName, alt) =>
          alt.traverse(f).map(AltTN(ref, altName, _))
        case MapTN(values) =>
          values
            .traverse { case (k, v) =>
              (f(k), f(v)).tupled
            }
            .map(MapTN(_))
        case CollectionTN(collectionType, values) =>
          values.traverse(f).map(CollectionTN(collectionType, _))
        case PrimitiveTN(prim, value) =>
          F.pure(PrimitiveTN(prim, value))
      }
      def foldLeft[A, B](fa: TypedNode[A], b: B)(f: (B, A) => B): B = ???
      def foldRight[A, B](fa: TypedNode[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] = ???
    }

  case class EnumerationTN(
      ref: Type.Ref,
      value: String,
      intValue: Int,
      name: String
  ) extends TypedNode[Nothing]
  case class StructureTN[A](
      ref: Type.Ref,
      fields: List[(String, FieldTN[A])]
  ) extends TypedNode[A]
  case class NewTypeTN[A](ref: Type.Ref, target: A) extends TypedNode[A]
  case class AltTN[A](ref: Type.Ref, altName: String, alt: AltValueTN[A])
      extends TypedNode[A]
  case class MapTN[A](values: List[(A, A)]) extends TypedNode[A]
  case class CollectionTN[A](collectionType: CollectionType, values: List[A])
      extends TypedNode[A]
  case class PrimitiveTN[T](prim: Primitive.Aux[T], value: T)
      extends TypedNode[Nothing]

}
