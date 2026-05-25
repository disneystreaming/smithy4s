/*
 *  Copyright 2021-2026 Disney Streaming
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

private[internals] object PackageRemapper {

  def apply(compilationUnit: CompilationUnit): CompilationUnit = {
    val config = compilationUnit.packageConfig
    if (config == PackageConfig.empty) compilationUnit
    else {
      val remapper = new PackageRemapper(config)
      compilationUnit.copy(
        declarations = compilationUnit.declarations.map(remapper.modDecl)
      )
    }
  }
}

private class PackageRemapper(config: PackageConfig) {

  def modDecl(decl: Decl): Decl = decl match {
    case Service(shapeId, name, ops, hints, version) =>
      Service(
        shapeId,
        name,
        ops.map(modOperation),
        hints.map(modHint),
        version
      )
    case p: Product => modProduct(p)
    case Union(shapeId, name, alts, mixins, recursive, hints) =>
      Union(
        shapeId,
        name,
        alts.map(modAlt),
        mixins.map(modType),
        recursive,
        hints.map(modHint)
      )
    case TypeAlias(shapeId, name, tpe, isUnwrapped, rec, hints) =>
      TypeAlias(
        shapeId,
        name,
        modType(tpe),
        isUnwrapped,
        rec,
        hints.map(modHint)
      )
    case ValidatedTypeAlias(shapeId, name, tpe, recursive, hints) =>
      ValidatedTypeAlias(
        shapeId,
        name,
        modType(tpe),
        recursive,
        hints.map(modHint)
      )
    case Enumeration(shapeId, name, tag, values, hints) =>
      Enumeration(shapeId, name, tag, values, hints.map(modHint))
  }

  private def modOperation(op: Operation): Operation =
    Operation(
      op.shapeId,
      op.name,
      op.methodName,
      op.params.map(modField),
      modType(op.input),
      op.errors.map(modType),
      modType(op.output),
      op.streamedInput.map(modStreamingField),
      op.streamedOutput.map(modStreamingField),
      op.hints.map(modHint)
    )

  private def modProduct(p: Product): Product =
    Product(
      p.shapeId,
      p.name,
      p.fields.map(modField),
      p.mixins.map(modType),
      p.recursive,
      p.hints.map(modHint),
      p.isMixin
    )

  private def modField(field: Field): Field =
    field.copy(
      tpe = modType(field.tpe),
      modifier = modModifier(field.modifier),
      hints = field.hints.map(modHint)
    )

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

  private def modStreamingField(sf: StreamingField): StreamingField =
    StreamingField(sf.name, modType(sf.tpe), sf.hints.map(modHint))

  private def modAlt(alt: Alt): Alt =
    Alt(
      alt.name,
      alt.realName,
      alt.member.update(modProduct)(modType),
      alt.hints.map(modHint)
    )

  private def modRef(ref: Type.Ref): Type.Ref =
    Type.Ref(config.remap(ref.namespace), ref.name)

  private def modType(tpe: Type): Type = tpe match {
    case Type.Ref(namespace, name) =>
      Type.Ref(config.remap(namespace), name)
    case Alias(namespace, name, tpe, isUnwrapped) =>
      Alias(config.remap(namespace), name, modType(tpe), isUnwrapped)
    case ValidatedAlias(namespace, name, tpe) =>
      ValidatedAlias(config.remap(namespace), name, modType(tpe))
    case Type.Collection(collectionType, member, memberHints) =>
      Type.Collection(collectionType, modType(member), memberHints.map(modHint))
    case Type.Map(mapTpe, key, keyHints, value, valueHints) =>
      Type.Map(
        mapTpe,
        modType(key),
        keyHints.map(modHint),
        modType(value),
        valueHints.map(modHint)
      )
    case PrimitiveType(_) => tpe
    case Nullable(under)  => Nullable(modType(under))
    case Type.ExternalType(
          name,
          fqn,
          typeParams,
          pFqn,
          under,
          refinementHint
        ) =>
      Type.ExternalType(
        name,
        fqn,
        typeParams.map(modType),
        pFqn,
        modType(under),
        modNativeHint(refinementHint)
      )
  }

  private def modNativeHint(hint: Hint.Native): Hint.Native =
    Hint.Native(
      hint.shapeId,
      hint.typedNode.map(recursion.preprocess(modTypedNode))
    )

  private def modHint(hint: Hint): Hint = hint match {
    case n: Hint.Native => modNativeHint(n)
    case Hint.Constraint(tr, nat) =>
      Hint.Constraint(modRef(tr), modNativeHint(nat))
    case df: Hint.Default =>
      Hint.Default(recursion.preprocess(modTypedNode)(df.typedNode))
    case other => other
  }

  private val modTypedNode: TypedNode ~> TypedNode =
    new (TypedNode ~> TypedNode) {
      def apply[A](fa: TypedNode[A]): TypedNode[A] = fa match {
        case EnumerationTN(ref, value, intValue, name) =>
          EnumerationTN(modRef(ref), value, intValue, name)
        case StructureTN(ref, fields) =>
          StructureTN(modRef(ref), fields)
        case NewTypeTN(ref, target) =>
          NewTypeTN(modRef(ref), target)
        case ValidatedNewTypeTN(ref, target) =>
          ValidatedNewTypeTN(modRef(ref), target)
        case AltTN(ref, altName, alt) =>
          AltTN(modRef(ref), altName, alt)
        case MapTN(mapTpe, values)    => MapTN(mapTpe, values)
        case CollectionTN(ct, values) => CollectionTN(ct, values)
        case PrimitiveTN(prim, value) => PrimitiveTN(prim, value)
      }
    }
}
