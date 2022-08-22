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

import cats.syntax.all._
import cats.~>
import smithy4s.codegen.Type.Alias
import smithy4s.codegen.Type.PrimitiveType
import smithy4s.codegen.TypedNode._
import smithy4s.codegen.Type.ExternalType
import LineSegment._

object CollisionAvoidance {
  def apply(compilationUnit: CompilationUnit): CompilationUnit = {

    val declarations = compilationUnit.declarations.map {
      case Service(name, originalName, ops, hints, version) =>
        val newOps = ops.map {
          case Operation(
                name,
                ns,
                params,
                input,
                errors,
                output,
                streamedInput,
                streamedOutput,
                hints
              ) =>
            Operation(
              protect(name.capitalize),
              ns,
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
          protect(name.capitalize),
          originalName,
          newOps,
          hints.map(modHint),
          version
        )
      case p: Product =>
        modProduct(p)
      case Union(name, originalName, alts, recursive, hints) =>
        Union(
          protect(name.capitalize),
          originalName,
          alts.map(modAlt),
          recursive,
          hints.map(modHint)
        )
      case TypeAlias(name, originalName, tpe, isUnwrapped, rec, hints) =>
        TypeAlias(
          protect(name.capitalize),
          originalName,
          modType(tpe),
          isUnwrapped,
          rec,
          hints.map(modHint)
        )
      case Enumeration(name, originalName, values, hints) =>
        val newValues = values.map {
          case EnumValue(value, intValue, name, hints) =>
            EnumValue(value, intValue, protect(name), hints.map(modHint))
        }
        Enumeration(
          protect(name.capitalize),
          originalName,
          newValues,
          hints.map(modHint)
        )
    }
    CompilationUnit(compilationUnit.namespace, declarations)
  }

  private def modType(tpe: Type): Type = tpe match {
    case Type.Collection(collectionType, member) =>
      Type.Collection(collectionType, modType(member))
    case Type.Map(key, value)      => Type.Map(modType(key), modType(value))
    case Type.Ref(namespace, name) => Type.Ref(namespace, name.capitalize)
    case Alias(namespace, name, tpe, isUnwrapped) =>
      Alias(namespace, protect(name.capitalize), modType(tpe), isUnwrapped)
    case PrimitiveType(prim) => PrimitiveType(prim)
    case ExternalType(name, fqn, pFqn, under, refinementHint) =>
      ExternalType(
        protect(name),
        fqn,
        pFqn,
        modType(under),
        modNativeHint(refinementHint)
      )
  }

  private def modField(field: Field): Field = {
    Field(
      protect(uncapitalise(field.name)),
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
      protect(uncapitalise(alt.name)),
      alt.name,
      alt.member.update(modProduct)(modType),
      alt.hints.map(modHint)
    )
  }

  private def modRef(ref: Type.Ref): Type.Ref =
    Type.Ref(ref.namespace, protect(ref.name.capitalize))

  private def modNativeHint(hint: Hint.Native): Hint.Native =
    Hint.Native(smithy4s.recursion.preprocess(modTypedNode)(hint.typedNode))

  private def modHint(hint: Hint): Hint = hint match {
    case n: Hint.Native => modNativeHint(n)
    case Hint.Constraint(tr, nat) =>
      Hint.Constraint(modRef(tr), modNativeHint(nat))
    case df: Hint.Default => modDefault(df)
    case other            => other
  }

  private def modDefault(hint: Hint.Default): Hint.Default = {
    Hint.Default(smithy4s.recursion.preprocess(modTypedNode)(hint.typedNode))
  }

  private def modProduct(p: Product): Product = {
    import p._
    Product(
      protect(name.capitalize),
      originalName,
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

  private def protect(str: String) =
    if (reservedNames(str)) s"_${str}" else str

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

  private val reservedTypes: Set[String] = Set(
    "String",
    "Byte",
    "Bytes",
    "Int",
    "Short",
    "Boolean",
    "Long",
    "Double",
    "Float",
    "BigInt",
    "BigDecimal",
    "Array",
    "Vector",
    "Seq",
    "Map",
    "List",
    "Nil",
    "Stream",
    "LazyList",
    "StringBuilder",
    "Range",
    "Either",
    "Left",
    "Right",
    "Set",
    "Option",
    "Some",
    "None",
    "Nothing",
  )

  private val reservedNames = reservedKeywords ++ reservedTypes

  class Names(compilationUnit: CompilationUnit) {

    val definitions = compilationUnit.declarations.foldMap { d => Set(d.name) }

    val Transformation_ = NameRef("smithy4s", "Transformation")
    val Service_ = NameRef("smithy4s", "Service")
    val Endpoint_ = NameRef("smithy4s", "Endpoint")
    val NoInput_ = NameRef("smithy4s", "NoInput")
    val ShapeId_ = NameRef("smithy4s", "ShapeId")
    val Schema_ = NameRef("smithy4s", "Schema")
    val Monadic_ = NameRef("smithy4s", "Monadic")
    val StreamingSchema_ = NameRef("smithy4s", "StreamingSchema")
    val Enumeration_ = NameRef("smithy4s", "Enumeration")
    val EnumValue_ = NameRef("smithy4s", "schema.EnumValue")
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
    val short_ = NameRef("smithy4s.schema.Schema", "Short")
    val int_ = NameRef("smithy4s.schema.Schema", "Integer")
    val long_ = NameRef("smithy4s.schema.Schema", "Long")
    val double_ = NameRef("smithy4s.schema.Schema", "Double")
    val float_ = NameRef("smithy4s.schema.Schema", "Float")
    val bigint_ = NameRef("smithy4s.schema.Schema", "BigInteger")
    val bigdecimal_ = NameRef("smithy4s.schema.Schema", "BigDecimal")
    val string_ = NameRef("smithy4s.schema.Schema", "String")
    val boolean_ = NameRef("smithy4s.schema.Schema", "Boolean")
    val byte_ = NameRef("smithy4s.schema.Schema", "Byte")
    val bytes_ = NameRef("smithy4s.schema.Schema", "Blob")
    val unit_ = NameRef("smithy4s.schema.Schema", "Unit")
    val timestamp_ = NameRef("smithy4s.schema.Schema", "Timestamp")
    val document_ = NameRef("smithy4s.schema.Schema", "Document")
    val uuid_ = NameRef("smithy4s.schema.Schema", "UUID")
    val Transformed_ = NameDef("Transformed")

    def reconcile(str: String): String = {
      val last = str.split('.').last
      if (definitions.contains(last)) str else last
    }

  }

}
