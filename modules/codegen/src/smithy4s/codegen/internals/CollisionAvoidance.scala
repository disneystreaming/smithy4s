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

package smithy4s.codegen.internals

import cats.~>

import Type.Alias
import Type.PrimitiveType
import TypedNode._
import Type.ExternalType
import LineSegment._
import smithy4s.codegen.internals.Type.Sparse

private[internals] object CollisionAvoidance {
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
      case Union(shapeId, name, alts, mixins, recursive, hints) =>
        Union(
          shapeId,
          protectType(name.capitalize),
          alts.map(modAlt),
          mixins.map(modType),
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
      case Enumeration(shapeId, name, tag, values, hints) =>
        val newValues = values.map {
          case EnumValue(value, intValue, name, hints) =>
            EnumValue(value, intValue, protectKeyword(name), hints.map(modHint))
        }
        Enumeration(
          shapeId,
          protectKeyword(name.capitalize),
          tag,
          newValues,
          hints.map(modHint)
        )
    }
    compilationUnit.copy(declarations = declarations)
  }

  private def modType(tpe: Type): Type = tpe match {
    case Type.Collection(collectionType, member, memberHints) =>
      Type.Collection(
        collectionType = collectionType,
        member = modType(member),
        memberHints = memberHints.map(modHint(_))
      )
    case Type.Map(key, keyHints, value, valueHints) =>
      Type.Map(
        key = modType(key),
        keyHints = keyHints.map(modHint(_)),
        value = modType(value),
        valueHints = valueHints.map(modHint(_))
      )
    case Type.Ref(namespace, name) =>
      Type.Ref(namespace, protectType(name.capitalize))
    case Alias(namespace, name, tpe, isUnwrapped) =>
      val protectedName = protectType(name.capitalize)
      val unwrapped = isUnwrapped | (protectedName != name.capitalize)
      Alias(namespace, protectType(name.capitalize), modType(tpe), unwrapped)
    case PrimitiveType(prim) => PrimitiveType(prim)
    case ExternalType(name, fqn, typeParams, pFqn, under, refinementHint) =>
      ExternalType(
        protectType(name.capitalize),
        fqn,
        typeParams,
        pFqn,
        modType(under),
        modNativeHint(refinementHint)
      )
    case Sparse(underlying) => Sparse(modType(underlying))
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
    Hint.Native(recursion.preprocess(modTypedNode)(hint.typedNode))

  private def modDefaultHint(hint: Hint.Default): Hint.Default =
    Hint.Default(recursion.preprocess(modTypedNode)(hint.typedNode))

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

    val Transformation = NameRef("smithy4s", "Transformation")
    val PolyFunction5_ = NameRef("smithy4s.kinds", "PolyFunction5")
    val Service_ = NameRef("smithy4s", "Service")
    val Endpoint_ = NameRef("smithy4s", "Endpoint")
    val NoInput_ = NameRef("smithy4s", "NoInput")
    val ShapeId_ = NameRef("smithy4s", "ShapeId")
    val Schema_ = NameRef("smithy4s", "Schema")
    val FunctorAlgebra_ = NameRef("smithy4s.kinds", "FunctorAlgebra")
    val BiFunctorAlgebra_ = NameRef("smithy4s.kinds", "BiFunctorAlgebra")
    val StreamingSchema_ = NameRef("smithy4s", "StreamingSchema")
    val Enumeration_ = NameRef("smithy4s", "Enumeration")
    val EnumValue_ = NameRef("smithy4s.schema", "EnumValue")
    val EnumTag_ = NameRef("smithy4s.schema", "EnumTag")
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
    val endpoint_ = NameDef("endpoint")
    val mapK5_ = NameDef("mapK5")
    val fromPolyFunction_ = NameDef("fromPolyFunction")
    val toPolyFunction_ = NameDef("toPolyFunction")
    val transform_ = NameDef("transform")
    val apply_ = NameDef("apply")
    val Impl_ = NameDef("Impl")
    val ErrorAware_ = NameDef("ErrorAware")
    val Constant_ = NameDef("Constant")
    val Default_ = NameDef("Default")
    val const5_ = NameRef("smithy4s.kinds.toPolyFunction5", "const5")

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
