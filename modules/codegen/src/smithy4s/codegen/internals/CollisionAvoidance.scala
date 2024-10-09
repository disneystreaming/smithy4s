/*
 *  Copyright 2021-2024 Disney Streaming
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
import Type.Nullable
import Type.PrimitiveType
import Type.ValidatedAlias
import TypedNode._
import Type.ExternalType
import LineSegment._

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
              protectKeyword(name.capitalize),
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
          protectKeyword(name.capitalize),
          newOps,
          hints.map(modHint),
          version
        )
      case p: Product =>
        modProduct(p)
      case Union(shapeId, name, alts, mixins, recursive, hints) =>
        Union(
          shapeId,
          protectKeyword(name.capitalize),
          alts.map(modAlt),
          mixins.map(modType),
          recursive,
          hints.map(modHint)
        )
      case TypeAlias(shapeId, name, tpe, isUnwrapped, rec, hints) =>
        val protectedName = protectKeyword(name.capitalize)
        // If we had to amend the type
        val unwrapped = isUnwrapped | (protectedName != name.capitalize)
        TypeAlias(
          shapeId,
          protectKeyword(name.capitalize),
          modType(tpe),
          unwrapped,
          rec,
          hints.map(modHint)
        )
      case ValidatedTypeAlias(shapeId, name, tpe, recursive, hints) =>
        ValidatedTypeAlias(
          shapeId,
          protectKeyword(name.capitalize),
          modType(tpe),
          recursive,
          hints.map(modHint)
        )
      case Enumeration(shapeId, name, tag, values, hints) =>
        val newValues = values.map {
          case EnumValue(value, intValue, name, realName, hints) =>
            EnumValue(
              value = value,
              intValue = intValue,
              name = protectKeyword(name),
              realName = realName,
              hints.map(modHint)
            )
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
      Type.Ref(namespace, protectKeyword(name.capitalize))
    case Alias(namespace, name, tpe, isUnwrapped) =>
      val protectedName = protectKeyword(name.capitalize)
      val unwrapped = isUnwrapped | (protectedName != name.capitalize)
      Alias(namespace, protectKeyword(name.capitalize), modType(tpe), unwrapped)
    case ValidatedAlias(namespace, name, tpe) =>
      ValidatedAlias(namespace, protectKeyword(name.capitalize), modType(tpe))
    case PrimitiveType(prim) => PrimitiveType(prim)
    case ExternalType(name, fqn, typeParams, pFqn, under, refinementHint) =>
      ExternalType(
        protectKeyword(name.capitalize),
        fqn,
        typeParams,
        pFqn,
        modType(under),
        modNativeHint(refinementHint)
      )
    case Nullable(underlying) => Nullable(modType(underlying))
  }

  private def modField(field: Field): Field = {
    Field(
      name = protectKeyword(uncapitalise(field.name)),
      realName = field.name,
      tpe = modType(field.tpe),
      modifier = modModifier(field.modifier),
      originalIndex = field.originalIndex,
      hints = field.hints.map(modHint)
    )
  }

  private def modModifier(modifier: Field.Modifier): Field.Modifier =
    Field.Modifier(
      required = modifier.required,
      nullable = modifier.nullable,
      default = modifier.default.map(modFieldDefault)
    )

  private def modFieldDefault(default: Field.Default): Field.Default =
    Field.Default(
      node = default.node,
      typedNode = default.typedNode.map(recursion.preprocess(modTypedNode))
    )

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
    Type.Ref(ref.namespace, protectKeyword(ref.name.capitalize))

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
          StructureTN(
            ref = modRef(ref),
            fields = fields.map { case (k, v) =>
              protectKeyword(k) -> v
            }
          )
        case NewTypeTN(ref, target) =>
          NewTypeTN(modRef(ref), target)
        case ValidatedNewTypeTN(ref, target) =>
          ValidatedNewTypeTN(modRef(ref), target)
        case AltTN(ref, altName, alt) =>
          // note: technically we should probably escape altName here
          // but it'd only really break if it matched a capitalized keyword,
          // and Scala has none of those, so it's impossible to write a failing test.
          // Alt names in this context are always capitalized before being printed
          // (Renderer.scala:1614 at the time of writing).
          AltTN(modRef(ref), altName, alt)
        case MapTN(values) =>
          MapTN(values)
        case CollectionTN(collectionType, values) =>
          CollectionTN(collectionType, values)
        case PrimitiveTN(prim, value) =>
          PrimitiveTN(prim, value)
      }
    }

  private[internals] def protectKeyword(str: String): String =
    if (reservedKeywords(str)) s"_$str" else str

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
    "yield",
    // For below, see https://github.com/disneystreaming/smithy4s/issues/1238
    // Treating all methods from java.lang.Object as reserved
    "clone",
    "equals",
    "finalize",
    "getClass",
    "hashCode",
    "notify",
    "notifyAll",
    "toString",
    "wait",

    // scala 3 "regular" keywords
    // https://docs.scala-lang.org/scala3/guides/migration/incompat-syntactic.html#restricted-keywords
    "enum",
    "export",
    "given",
    "then"
  )

  class Names() {

    val Transformation = NameRef("smithy4s", "Transformation")
    val PolyFunction5_ = NameRef("smithy4s.kinds", "PolyFunction5")
    val Service_ = NameRef("smithy4s", "Service")
    val ServiceProduct = NameRef("smithy4s", "ServiceProduct")
    val ServiceProductMirror = NameRef("smithy4s", "ServiceProduct.Mirror")
    val Endpoint_ = NameRef("smithy4s", "Endpoint")
    val NoInput_ = NameRef("smithy4s", "NoInput")
    val ShapeId_ = NameRef("smithy4s", "ShapeId")
    val Schema_ = NameRef("smithy4s", "Schema")
    val Validator_ = NameRef("smithy4s", "Validator")
    val OperationSchema_ = NameRef("smithy4s.schema", "OperationSchema")
    val FunctorAlgebra_ = NameRef("smithy4s.kinds", "FunctorAlgebra")
    val BiFunctorAlgebra_ = NameRef("smithy4s.kinds", "BiFunctorAlgebra")
    val Bijection_ = NameRef("smithy4s", "Bijection")
    val StreamingSchema_ = NameRef("smithy4s.schema", "StreamingSchema")
    val Enumeration_ = NameRef("smithy4s", "Enumeration")
    val EnumValue_ = NameRef("smithy4s.schema", "EnumValue")
    val EnumTag_ = NameRef("smithy4s.schema", "EnumTag")
    val Newtype_ = NameRef("smithy4s", "Newtype")
    val ValidatedNewtype_ = NameRef("smithy4s", "ValidatedNewtype")
    val Hints_ = NameRef("smithy4s", "Hints")
    val ShapeTag_ = NameRef("smithy4s", "ShapeTag")
    val ErrorSchema_ = NameRef("smithy4s.schema", "ErrorSchema")
    val union_ = NameRef("smithy4s.schema.Schema", "union")
    val recursive_ = NameRef("smithy4s.schema.Schema", "recursive")
    val enumeration_ = NameRef("smithy4s.schema.Schema", "enumeration")
    val constant_ = NameRef("smithy4s.schema.Schema", "constant")
    val struct_ = NameRef("smithy4s.schema.Schema", "struct")
    val bijection_ = NameRef("smithy4s.schema.Schema", "bijection")
    val Transformed_ = NameDef("Transformed")
    val endpoint_ = NameDef("endpoint")
    val input_ = NameDef("input")
    val ordinal_ = NameDef("ordinal")
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
    val smithy4sThrowable = NameRef("smithy4s", "Smithy4sThrowable")

    // We reserve these keywords as they collide with types that the
    // users are bound to manipulate when using Smithy4s .
    val short_ = NameRef("scala", "Short")
    val int_ = NameRef("scala", "Int")
    val javaInt_ = NameRef("java.lang", "Integer")
    val long_ = NameRef("scala", "Long")
    val double_ = NameRef("scala", "Double")
    val float_ = NameRef("scala", "Float")
    val bigint_ = NameRef("scala.math", "BigInteger")
    val bigdecimal_ = NameRef("scala.math", "BigDecimal")
    val string_ = NameRef("java.lang", "String")
    val boolean_ = NameRef("scala", "Boolean")
    val byte_ = NameRef("scala", "Byte")
    val unit_ = NameRef("scala", "Unit")
    val timestamp_ = NameRef("smithy4s", "Timestamp")
    val document_ = NameRef("smithy4s", "Document")
    val uuid_ = NameRef("smithy4s", "UUID")
    val list = NameRef("scala", "List")
    val indexedSeq = NameRef("scala.collection.immutable", "IndexedSeq")
    val set = NameRef("scala.collection.immutable", "Set")
    val map = NameRef("scala.collection.immutable", "Map")
    val vector = NameRef("scala", "Vector")
    val option = NameRef("scala", "Option")
    val none = NameRef("scala", "None")
    val some = NameRef("scala", "Some")
    val noStackTrace = NameRef("scala.util.control", "NoStackTrace")
    val throwable = NameRef("java.lang", "Throwable")

  }

}
