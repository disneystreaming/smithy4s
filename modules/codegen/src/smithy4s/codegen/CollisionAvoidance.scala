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

import cats.~>
import smithy4s.codegen.Type.Alias
import smithy4s.codegen.Type.PrimitiveType
import smithy4s.codegen.TypedNode._
import smithy4s.codegen.Type.ExternalType
import LineSegment._

object CollisionAvoidance {
  def apply(compilationUnit: CompilationUnit): CompilationUnit = {

    val declarations = compilationUnit.declarations.map {
      case Service(serviceId, name, ops, hints, version) =>
        val newOps = ops.map {
          case Operation(
                opId,
                name,
                methodName,
                params,
                input,
                errors,
                output,
                streamedInput,
                streamedOutput,
                hints
              ) =>
            Operation(
              opId,
              protectType(name.capitalize),
              protectKeyword(methodName),
              params.map(modField),
              modType(input),
              errors.map(modType),
              modType(output),
              streamedInput.map(modStreamingField),
              streamedOutput.map(modStreamingField),
              hints.map(modHint)
            )
        }
        Service(
          serviceId,
          protectType(name.capitalize),
          newOps,
          hints.map(modHint),
          version
        )
      case p: Product =>
        modProduct(p)
      case Union(shapeId, name, alts, recursive, hints) =>
        Union(
          shapeId,
          protectType(name.capitalize),
          alts.map(modAlt),
          recursive,
          hints.map(modHint)
        )
      case TypeAlias(shapeId, name, tpe, isUnwrapped, rec, hints) =>
        val protectedName = protectType(name.capitalize)
        // If we had to amend the type
        val unwrapped = isUnwrapped | (protectedName != name.capitalize)
        TypeAlias(
          shapeId,
          protectType(name.capitalize),
          modType(tpe),
          unwrapped,
          rec,
          hints.map(modHint)
        )
      case Enumeration(shapeId, name, values, hints) =>
        val newValues = values.map {
          case EnumValue(value, intValue, name, hints) =>
            EnumValue(value, intValue, protectKeyword(name), hints.map(modHint))
        }
        Enumeration(
          shapeId,
          protectKeyword(name.capitalize),
          newValues,
          hints.map(modHint)
        )
    }
    CompilationUnit(compilationUnit.namespace, declarations)
  }

  private def modType(tpe: Type): Type = tpe match {
    case Type.Collection(collectionType, member) =>
      Type.Collection(collectionType, modType(member))
    case Type.Map(key, value) => Type.Map(modType(key), modType(value))
    case Type.Ref(namespace, name) =>
      Type.Ref(namespace, protectType(name.capitalize))
    case Alias(namespace, name, tpe, isUnwrapped) =>
      val protectedName = protectType(name.capitalize)
      val unwrapped = isUnwrapped | (protectedName != name.capitalize)
      Alias(namespace, protectType(name.capitalize), modType(tpe), unwrapped)
    case PrimitiveType(prim) => PrimitiveType(prim)
    case ExternalType(name, fqn, pFqn, under, refinementHint) =>
      ExternalType(
        protectType(name.capitalize),
        fqn,
        pFqn,
        modType(under),
        modNativeHint(refinementHint)
      )
  }

  private def modField(field: Field): Field = {
    Field(
      protectKeyword(uncapitalise(field.name)),
      field.name,
      modType(field.tpe),
      field.required,
      field.hints.map(modHint)
    )
  }

  private def modStreamingField(
      streamingField: StreamingField
  ): StreamingField = {
    StreamingField(
      streamingField.name,
      modType(streamingField.tpe),
      streamingField.hints.map(modHint)
    )
  }

  private def modAlt(alt: Alt): Alt = {
    Alt(
      protectKeyword(uncapitalise(alt.name)),
      alt.name,
      alt.member.update(modProduct)(modType),
      alt.hints.map(modHint)
    )
  }

  private def modRef(ref: Type.Ref): Type.Ref =
    Type.Ref(ref.namespace, protectType(ref.name.capitalize))

  private def modNativeHint(hint: Hint.Native): Hint.Native =
    Hint.Native(smithy4s.recursion.preprocess(modTypedNode)(hint.typedNode))

  private def modDefaultHint(hint: Hint.Default): Hint.Default =
    Hint.Default(smithy4s.recursion.preprocess(modTypedNode)(hint.typedNode))

  private def modHint(hint: Hint): Hint = hint match {
    case n: Hint.Native => modNativeHint(n)
    case Hint.Constraint(tr, nat) =>
      Hint.Constraint(modRef(tr), modNativeHint(nat))
    case df: Hint.Default => modDefaultHint(df)
    case other            => other
  }

  private def modProduct(p: Product): Product = {
    import p._
    Product(
      p.shapeId,
      protectKeyword(name.capitalize),
      fields.map(modField),
      mixins.map(modType),
      recursive,
      hints.map(modHint),
      isMixin
    )
  }

  private def modTypedNode: TypedNode ~> TypedNode =
    new (TypedNode ~> TypedNode) {

      def apply[A](fa: TypedNode[A]): TypedNode[A] = fa match {
        case EnumerationTN(ref, value, intValue, name) =>
          EnumerationTN(modRef(ref), value, intValue, name)
        case StructureTN(ref, fields) =>
          StructureTN(modRef(ref), fields)
        case NewTypeTN(ref, target) =>
          NewTypeTN(modRef(ref), target)
        case AltTN(ref, altName, alt) =>
          AltTN(modRef(ref), altName, alt)
        case MapTN(values) =>
          MapTN(values)
        case CollectionTN(collectionType, values) =>
          CollectionTN(collectionType, values)
        case PrimitiveTN(prim, value) =>
          PrimitiveTN(prim, value)
      }
    }

  private def protectKeyword(str: String): String =
    if (reservedKeywords(str)) s"_$str" else str

  private val names = new Names()
  private def protectType(str: String): String =
    if (names.getReservedNames(str)) "_" + str else protectKeyword(str)

  private val reservedKeywords: Set[String] = Set(
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "forSome",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "new",
    "null",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "this",
    "throw",
    "trait",
    "true",
    "try",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield"
  )

  class Names() {

    // Using mutation to avoid repetition.
    private var reservedNames: Set[String] = Set.empty
    private def reserved(pkg: String, name: String) = {
      reservedNames = reservedNames + name
      NameRef(pkg, name)
    }

    def getReservedNames: Set[String] = reservedNames

    val Transformation = NameRef("smithy4s.capability", "Transformation")
    val PolyFunction5_ = NameRef("smithy4s.kinds", "PolyFunction5")
    val Service_ = NameRef("smithy4s", "Service")
    val Endpoint_ = NameRef("smithy4s", "Endpoint")
    val NoInput_ = NameRef("smithy4s", "NoInput")
    val ShapeId_ = NameRef("smithy4s", "ShapeId")
    val Schema_ = NameRef("smithy4s", "Schema")
    val FunctorAlgebra_ = NameRef("smithy4s.kinds", "FunctorAlgebra")
    val StreamingSchema_ = NameRef("smithy4s", "StreamingSchema")
    val Enumeration_ = NameRef("smithy4s", "Enumeration")
    val EnumValue_ = NameRef("smithy4s.schema", "EnumValue")
    val Newtype_ = NameRef("smithy4s", "Newtype")
    val Hints_ = NameRef("smithy4s", "Hints")
    val ShapeTag_ = NameRef("smithy4s", "ShapeTag")
    val Errorable_ = NameRef("smithy4s", "Errorable")
    val unionSchema_ = NameRef("smithy4s.schema.Schema", "UnionSchema")
    val union_ = NameRef("smithy4s.schema.Schema", "union")
    val recursive_ = NameRef("smithy4s.schema.Schema", "recursive")
    val enumeration_ = NameRef("smithy4s.schema.Schema", "enumeration")
    val constant_ = NameRef("smithy4s.schema.Schema", "constant")
    val struct_ = NameRef("smithy4s.schema.Schema", "struct")
    val bijection_ = NameRef("smithy4s.schema.Schema", "bijection")
    val Transformed_ = NameDef("Transformed")

    // We reserve these keywords as they collide with types that the
    // users are bound to manipulate when using Smithy4s .
    val short_ = reserved("scala", "Short")
    val int_ = reserved("scala", "Int")
    val javaInt_ = reserved("java.lang", "Integer")
    val long_ = reserved("scala", "Long")
    val double_ = reserved("scala", "Double")
    val float_ = reserved("scala", "Float")
    val bigint_ = reserved("scala.math", "BigInteger")
    val bigdecimal_ = reserved("scala.math", "BigDecimal")
    val string_ = reserved("scala.Predef", "String")
    val boolean_ = reserved("scala", "Boolean")
    val byte_ = reserved("scala", "Byte")
    val unit_ = reserved("scala", "Unit")
    val timestamp_ = reserved("smithy4s", "Timestamp")
    val document_ = reserved("smithy4s", "Document")
    val uuid_ = reserved("smithy4s", "UUID")
    val list = reserved("scala", "List")
    val set = reserved("scala.collection.immutable", "Set")
    val map = reserved("scala.collection.immutable", "Map")
    val option = reserved("scala", "Option")
    val none = reserved("scala", "None")
    val some = reserved("scala", "Some")

  }

}
