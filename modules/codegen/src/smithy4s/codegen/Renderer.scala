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
import cats.data.Reader
import cats.syntax.all._
import smithy4s.codegen.Primitive.Nothing
import smithy4s.codegen.TypedNode._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node._

import scala.jdk.CollectionConverters._

import LineSyntax.LineInterpolator
import ToLines.lineToLines

object Renderer {

  case class Result(namespace: String, name: String, content: String)

  def apply(unit: CompilationUnit): List[Result] = {
    val r = new Renderer(unit)

    val pack = Result(
      unit.namespace,
      "package",
      r.renderPackageContents.lines.mkString(
        System.lineSeparator()
      )
    )

    val classes = unit.declarations.map { decl =>
      val renderResult = r.renderDecl(decl)
      val p = s"package ${unit.namespace}"

      val allImports =
        renderResult.imports.filter(
          _.replaceAll(unit.namespace, "")
            .split('.')
            .count(_.nonEmpty) > 1
        )

      // TODO iterate through imports and remove unused ones , based off the current namespace
      val allLines = List(p, "") ++
        allImports.toList.sorted.map("import " + _) ++
        List("") ++
        renderResult.lines

      val content = allLines.mkString(System.lineSeparator())

      Result(unit.namespace, decl.name, content)
    }

    val packageApplicableDecls = unit.declarations.filter {
      case _: TypeAlias | _: Service => true
      case _                         => false
    }

    if (packageApplicableDecls.isEmpty) classes
    else pack :: classes
  }

}

private[codegen] class Renderer(compilationUnit: CompilationUnit) { self =>

  val names = new CollisionAvoidance.Names(compilationUnit)
  import compilationUnit.namespace
  import names._

  def renderDecl(decl: Decl): Lines = decl match {
    case Service(name, originalName, ops, hints, version) =>
      renderService(name, originalName, ops, hints, version)
    case Product(name, originalName, fields, recursive, hints) =>
      renderProduct(name, originalName, fields, recursive, hints)
    case Union(name, originalName, alts, recursive, hints) =>
      renderUnion(name, originalName, alts, recursive, hints)
    case TypeAlias(name, originalName, tpe, _, hints) =>
      renderTypeAlias(name, originalName, tpe, hints)
    case Enumeration(name, originalName, values, hints) =>
      renderEnum(name, originalName, values, hints)
    case _ => Lines.empty
  }

  def renderPackageContents: Lines = {
    val typeAliases = compilationUnit.declarations.collect {
      case TypeAlias(name, _, _, _, _) =>
        s"type $name = ${compilationUnit.namespace}.${name}.Type"
    }

    val blk =
      block(
        s"package object ${compilationUnit.namespace.split('.').last}"
      )(
        compilationUnit.declarations.map(renderDeclPackageContents),
        newline,
        typeAliases,
        newline
      )
    val parts = compilationUnit.namespace.split('.').filter(_.nonEmpty)
    if (parts.size > 1) {
      lines(
        s"package ${parts.dropRight(1).mkString(".")}",
        newline,
        blk
      )
    } else blk
  }

  private def renderDeclPackageContents(decl: Decl): Lines = decl match {
    case s: Service =>
      val name = s.name
      lines(
        s"type $name[F[_]] = smithy4s.Monadic[${name}Gen, F]",
        block(
          s"object $name extends $Service_.Provider[${name}Gen, ${name}Operation]"
        )(
          s"def apply[F[_]](implicit F: $name[F]): F.type = F",
          s"def service: $Service_[${name}Gen, ${name}Operation] = ${name}Gen",
          s"val id: $ShapeId_ = service.id"
        )
      )
    case _ => empty
  }

  private def renderService(
      name: String,
      originalName: String,
      ops: List[Operation],
      hints: List[Hint],
      version: String
  ): Lines = {

    val genName = name + "Gen"
    val opTraitName = name + "Operation"

    lines(
      block(s"trait $genName[F[_, _, _, _, _]]")(
        line"self =>",
        newline,
        ops.map { op =>
          line"def ${op.methodName}(${op.renderArgs}) : F[${op.renderAlgParams}]"

        },
        newline,
        line"def transform[G[_, _, _, _, _]](transformation : $Transformation_[F, G]) : $genName[G] = new Transformed(transformation)",
        block(
          line"class Transformed[G[_, _, _, _, _]](transformation : $Transformation_[F, G]) extends $genName[G]"
        ) {
          ops.map { op =>
            val opName = op.methodName
            line"def $opName(${op.renderArgs}) = transformation[${op.renderAlgParams}](self.$opName(${op.renderParams}))"
          }
        }
      ),
      newline,
      obj(
        genName,
        ext = line"$Service_[$genName, $opTraitName]"
      )(
        newline,
        line"def apply[F[_]](implicit F: smithy4s.Monadic[$genName, F]): F.type = F",
        newline,
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        newline,
        line"val endpoints: List[$Endpoint_[$opTraitName, _, _, _, _, _]] = List"
          .args(ops.map(_.name)),
        newline,
        line"""val version: String = "$version"""",
        newline,
        if (ops.isEmpty) {
          line"""def endpoint[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) = sys.error("impossible")"""

        } else {
          block(
            s"def endpoint[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) = op match"
          ) {
            ops.map {
              case op if op.input != Type.unit =>
                s"case ${op.name}(input) => (input, ${op.name})"
              case op =>
                s"case ${op.name}() => ((), ${op.name})"
            }
          }
        },
        newline,
        block(s"object reified extends $genName[$opTraitName]") {
          ops.map {
            case op if op.input == Type.unit =>
              line"def ${op.methodName}(${op.renderArgs}) = ${op.name}()"
            case op if op.hints.contains(Hint.PackedInputs) =>
              line"def ${op.methodName}(${op.renderArgs}) = ${op.name}(input)"
            case op =>
              line"def ${op.methodName}(${op.renderArgs}) = ${op.name}(${op.input}(${op.renderParams}))"
          }
        },
        newline,
        line"def transform[P[_, _, _, _, _]](transformation: $Transformation_[$opTraitName, P]): $genName[P] = reified.transform(transformation)",
        newline,
        line"def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: $genName[P], transformation: $Transformation_[P, P1]): $genName[P1] = alg.transform(transformation)",
        newline,
        block(
          line"def asTransformation[P[_, _, _, _, _]](impl : $genName[P]): $Transformation_[$opTraitName, P] = new $Transformation_[$opTraitName, P]"
        ) {
          if (ops.isEmpty) {
            line"""def apply[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = sys.error("impossible")""".toLines
          } else {
            block(
              line"def apply[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match "
            ) {
              ops.map {
                case op if op.input == Type.unit =>
                  line"case ${op.name}() => impl.${op.methodName}(${op.renderParams})"
                case op if op.hints.contains(Hint.PackedInputs) =>
                  line"case ${op.name}(input) => impl.${op.methodName}(${op.renderParams})"
                case op =>
                  line"case ${op.name}(${op.input}(${op.renderParams})) => impl.${op.methodName}(${op.renderParams})"
              }
            }
          }
        },
        ops.map(renderOperation(name, _))
      ),
      newline,
      line"sealed trait $opTraitName[Input, Err, Output, StreamedInput, StreamedOutput]",
      newline
    )
  }

  private def renderOperation(
      serviceName: String,
      op: Operation
  ): Lines = {
    val params = if (op.input != Type.unit) {
      line"input: ${op.input}"
    } else Line.empty
    val opName = op.name
    val traitName = s"${serviceName}Operation"
    val input =
      if (op.input == Type.unit) "" else "input"
    val errorName = if (op.errors.isEmpty) "Nothing" else s"${op.name}Error"

    val errorable = if (op.errors.nonEmpty) {
      s" with $Errorable_[$errorName]"
    } else ""

    val errorUnion: Option[Union] = for {
      errorNel <- NonEmptyList.fromList(op.errors)
      alts <- errorNel.traverse { t =>
        t.name.map(n => Alt(n, UnionMember.TypeCase(t)))
      }
      name = opName + "Error"
    } yield Union(name, name, alts)

    val renderedErrorUnion = errorUnion.foldMap {
      case Union(name, originalName, alts, recursive, hints) =>
        renderUnion(name, originalName, alts, recursive, hints, error = true)
    }

    val additionalImports =
      if (op.input == Type.unit || op.output == Type.unit) syntaxImport
      else Set.empty[String]

    lines(
      line"case class $opName($params) extends $traitName[${op.renderAlgParams}]",
      obj(
        opName,
        ext = line"$Endpoint_[${traitName}, ${op.renderAlgParams}]$errorable"
      )(
        renderId(op.name, op.originalNamespace),
        line"val input: $Schema_[${op.input}] = ${op.input.schemaRef}.addHints(smithy4s.internals.InputOutput.Input.widen)",
        line"val output: $Schema_[${op.output}] = ${op.output.schemaRef}.addHints(smithy4s.internals.InputOutput.Output.widen)",
        renderStreamingSchemaVal("streamedInput", op.streamedInput),
        renderStreamingSchemaVal("streamedOutput", op.streamedOutput),
        renderHintsVal(op.hints),
        line"def wrap(input: ${op.input}) = ${opName}($input)",
        renderErrorable(op)
      ),
      renderedErrorUnion
    ).addImports(additionalImports ++ {
      if (op.errors.isEmpty) Set.empty[String]
      else Set(s"${serviceName}Gen.$errorName")
    })
  }

  private def renderStreamingSchemaVal(
      valName: String,
      sField: Option[StreamingField]
  ): Line = sField match {
    case Some(StreamingField(name, tpe, hints)) =>
      val mh =
        if (hints.isEmpty) Line.empty
        else line".addHints(${memberHints(hints)})"
      line"""val $valName : $StreamingSchema_[${tpe}] = $StreamingSchema_("$name", ${tpe.schemaRef}$mh)"""
    case None =>
      line"""val $valName : $StreamingSchema_[Nothing] = $StreamingSchema_.nothing"""
  }

  private def renderProtocol(name: String, hints: List[Hint]): Lines = {
    hints.collectFirst({ case p: Hint.Protocol => p }).foldMap { protocol =>
      val protocolTraits = protocol.traits
        .map(t => line"""$ShapeId_("${t.namespace}", "${t.name}")""")
        .intercalate(Line.comma)
      lines(
        newline,
        block(
          line"implicit val protocol: smithy4s.Protocol[$name] = new smithy4s.Protocol[$name]"
        ) {
          line"def traits: Set[$ShapeId_] = Set($protocolTraits)"
        }
      )
    }
  }

  private def renderProduct(
      name: String,
      originalName: String,
      fields: List[Field],
      recursive: Boolean,
      hints: List[Hint],
      adtParent: Option[String] = None,
      additionalLines: Lines = Lines.empty
  ): Lines = {

    val decl = line"case class $name(${renderArgs(fields)})"
    val imports = syntaxImport
    val schemaImplicit = if (adtParent.isEmpty) "implicit " else ""

    lines(
      if (hints.contains(Hint.Error)) {
        block(line"${decl} extends Throwable") {
          fields
            .find { f =>
              f.hints.contains_(Hint.ErrorMessage) ||
              f.name === "message"
            }
            .filter {
              _.tpe.dealiased == Type.PrimitiveType(Primitive.String)
            }
            .foldMap(renderGetMessage)
        }
      } else {
        val extend = adtParent.map(t => s" extends $t").getOrElse("")
        line"$decl$extend"
      },
      obj(name, line"${shapeTag(name)}")(
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        renderProtocol(name, hints),
        newline,
        if (fields.nonEmpty) {
          val renderedFields =
            fields.map { case Field(fieldName, realName, tpe, required, hints) =>
              val req = if (required) "required" else "optional"
              if (hints.isEmpty) {
                line"""${tpe.schemaRef}.$req[$name]("$realName", _.$fieldName)"""
              } else {
                val mh = memberHints(hints)
                  // format: off
                  line"""${tpe.schemaRef}${renderConstraintValidation(hints)}.$req[$name]("$realName", _.$fieldName).addHints($mh)"""
                  // format: on
              }
            }
          if (fields.size <= 22) {
            val definition = if (recursive) "recursive(struct" else "struct"
            line"${schemaImplicit}val schema: $Schema_[$name] = $definition"
              .args(renderedFields)
              .block(s"$name.apply")
              .appendToLast(".withId(id).addHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          } else {
            val definition =
              if (recursive) "recursive(struct.genericArity"
              else "struct.genericArity"
            line"${schemaImplicit}val schema: $Schema_[$name] = $definition"
              .args(renderedFields)
              .block(
                line"arr => new $name".args(
                  fields.zipWithIndex.map {
                    case (Field(_, _, tpe, required, _), idx) =>
                      val scalaTpe = line"${tpe}"
                        .modify(line => {
                          if (required) line else s"Option[$line]"
                        })
                      line"arr($idx).asInstanceOf[$scalaTpe]"
                  }
                )
              )
              .appendToLast(".withId(id).addHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          }
        } else {
          line"implicit val schema: $Schema_[$name] = constant($name()).withId(id).addHints(hints)"
        },
        additionalLines
      )
    ).addImports(imports)
  }

  private def renderGetMessage(field: Field) = field match {
    case field if field.tpe.isResolved && field.required =>
      line"override def getMessage(): String = ${field.name}"
    case field if field.tpe.isResolved =>
      line"override def getMessage(): String = ${field.name}.orNull"
    case field if field.required =>
      line"override def getMessage(): String = ${field.name}.value"
    case field =>
      line"override def getMessage(): String = ${field.name}.map(_.value).orNull"
  }

  private def renderErrorable(op: Operation): Lines = {
    val errorName = op.name + "Error"

    if (op.errors.isEmpty) Lines.empty
    else
      lines(
        s"override val errorable: Option[$Errorable_[$errorName]] = Some(this)",
        s"val error: $unionSchema_[$errorName] = $errorName.schema",
        block(
          s"def liftError(throwable: Throwable) : Option[$errorName] = throwable match"
        ) {
          op.errors.collect { case Type.Ref(_, name) =>
            s"case e: ${name} => Some($errorName.${name}Case(e))"
          } ++ List("case _ => None")
        },
        block(
          s"def unliftError(e: $errorName) : Throwable = e match"
        ) {
          op.errors.collect { case Type.Ref(_, name) =>
            line"case $errorName.${name}Case(e) => e"
          }
        }
      )
  }

  private def renderUnion(
      name: String,
      originalName: String,
      alts: NonEmptyList[Alt],
      recursive: Boolean,
      hints: List[Hint],
      error: Boolean = false
  ): Lines = {
    def caseName(alt: Alt) = alt.member match {
      case UnionMember.ProductCase(product) => product.name
      case UnionMember.TypeCase(_) | UnionMember.UnitCase =>
        alt.name.dropWhile(_ == '_').capitalize + "Case"
    }
    val caseNames = alts.map(caseName)
    val caseNamesAndIsUnit =
      caseNames.zip(alts.map(_.member == UnionMember.UnitCase))
    val imports = /*alts.foldMap(_.tpe.imports) ++*/ syntaxImport

    lines(
      block(
        s"sealed trait $name extends scala.Product with scala.Serializable"
      )(
        line"@inline final def widen: $name = this"
      ),
      obj(name, line"${shapeTag(name)}")(
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        newline,
        alts.map {
          case a @ Alt(_, realName, UnionMember.UnitCase, altHints) =>
            val cn = caseName(a)
            // format: off
            lines(
              line"case object $cn extends $name",
              line"""private val ${cn}Alt = $Schema_.constant($cn)${renderConstraintValidation(altHints)}.oneOf[$name]("$realName").addHints(hints)""",
              line"private val ${cn}AltWithValue = ${cn}Alt($cn)"
            )
            // format: on
          case a @ Alt(altName, _, UnionMember.TypeCase(tpe), _) =>
            val cn = caseName(a)
            lines(
              line"case class $cn(${uncapitalise(altName)}: ${tpe}) extends $name"
            )
          case Alt(_, realName, UnionMember.ProductCase(struct), _) =>
            val additionalLines = lines(
              newline,
              line"""val alt = schema.oneOf[$name]("$realName")"""
            )
            renderProduct(
              struct.name,
              struct.originalName,
              struct.fields,
              struct.recursive,
              struct.hints,
              adtParent = Some(name),
              additionalLines
            )
        },
        newline,
        alts.collect {
          case a @ Alt(
                altName,
                realName,
                UnionMember.TypeCase(tpe),
                altHints
              ) =>
            val cn = caseName(a)
            block(s"object $cn")(
              renderHintsVal(altHints),
            // format: off
            line"val schema: $Schema_[$cn] = bijection(${tpe.schemaRef}.addHints(hints)${renderConstraintValidation(altHints)}, $cn(_), _.${uncapitalise(altName)})",
            line"""val alt = schema.oneOf[$name]("$realName")""",
            // format: on
            )
        },
        newline, {
          val union =
            if (error)
              line"implicit val schema: $unionSchema_[$name] = union"
            else if (recursive)
              line"implicit val schema: $Schema_[$name] = recursive(union"
            else
              line"implicit val schema: $Schema_[$name] = union"
          union
            .args {
              caseNamesAndIsUnit.map {
                case (caseName, false) => caseName + ".alt"
                case (caseName, true)  => caseName + "Alt"
              }
            }
            .block {
              caseNamesAndIsUnit.map {
                case (caseName, true) =>
                  s"case $caseName => ${caseName}AltWithValue"
                case (caseName, false) =>
                  s"case c : $caseName => $caseName.alt(c)"
              }
            }
            .appendToLast(
              if (error) "" else ".withId(id).addHints(hints)"
            )
            .appendToLast(if (recursive) ")" else "")
        }
      )
    ).addImports(imports)
  }

  private def fieldToRenderLine(field: Field): Line = {
    field match {
      case Field(name, _, tpe, required, _) =>
        line"$tpe"
          .modify(line =>
            name + ": " + { if (required) line else s"Option[$line] = None" }
          )
    }
  }
  private def renderArgs(fields: List[Field]): Line = fields
    .map(fieldToRenderLine)
    .intercalate(Line.comma)

  private def renderEnum(
      name: String,
      originalName: String,
      values: List[EnumValue],
      hints: List[Hint]
  ): Lines = lines(
    block(
      line"sealed abstract class $name(_value: String, _name: String, _ordinal: Int) extends $Enumeration_.Value"
    )(
      line"override val value: String = _value",
      line"override val name: String = _name",
      line"override val ordinal: Int = _ordinal",
      line"override val hints: $Hints_ = $Hints_.empty",
      line"@inline final def widen: $name = this"
    ),
    obj(name, ext = line"$Enumeration_[$name]", w = line"${shapeTag(name)}")(
      renderId(originalName),
      newline,
      renderHintsVal(hints),
      newline,
      values.map { case e @ EnumValue(value, ordinal, _, _) =>
        line"""case object ${e.name} extends $name("$value", "${e.name}", $ordinal)"""
      },
      newline,
      line"val values: List[$name] = List".args(
        values.map(_.name)
      ),
      line"implicit val schema: $Schema_[$name] = enumeration(values).withId(id).addHints(hints)"
    )
  ).addImports(syntaxImport)

  private def renderTypeAlias(
      name: String,
      originalName: String,
      tpe: Type,
      hints: List[Hint]
  ): Lines = {
    val imports = Set("smithy4s.Newtype") ++ syntaxImport

    val trailingCalls =
      line".withId(id).addHints(hints)${renderConstraintValidation(hints)}"
    lines(
      obj(name, line"Newtype[$tpe]")(
        renderId(originalName),
        renderHintsVal(hints),
        line"val underlyingSchema : $Schema_[$tpe] = ${tpe.schemaRef}$trailingCalls",
        lines(
          s"implicit val schema : $Schema_[$name] = bijection(underlyingSchema, asBijection)"
        )
      )
    ).addImports(imports)
  }

  private implicit class OperationExt(op: Operation) {
    def renderArgs =
      if (op.input == Type.unit) Line.empty
      else if (op.hints.contains(Hint.PackedInputs)) {
        line"input: ${op.input}"
      } else self.renderArgs(op.params)

    def renderParams: Line =
      if (op.input == Type.unit) Line.empty
      else if (op.hints.contains(Hint.PackedInputs)) {
        line"input"
      } else op.params.map(f => Line(f.name)).intercalate(Line.comma)

    def methodName = uncapitalise(op.name)

    def renderAlgParams = {
      line"${op.input}, ${if (op.errors.isEmpty) "Nothing"
      else op.name + "Error"}, ${op.output}, ${op.streamedInput.map(_.tpe).getOrElse(Type.PrimitiveType(Nothing))}, ${op.streamedOutput
        .map(_.tpe)
        .getOrElse(Type.PrimitiveType(Nothing))}"
    }
  }

  implicit class TypeRefExt(tpe: Type.Ref) {
    def renderFull: String = s"${tpe.namespace}.${tpe.name}"
  }

  implicit class TypeExt(tpe: Type) {
    def schemaRef: Line = tpe match {
      case Type.PrimitiveType(p) => Line(schemaRefP(p))
      case Type.Collection(collectionType, member) =>
        val col = collectionType match {
          case CollectionType.List       => "list"
          case CollectionType.Set        => "set"
          case CollectionType.Vector     => "vector"
          case CollectionType.IndexedSeq => "indexedSeq"
        }
        line"$col(${member.schemaRef})"
      case Type.Map(key, value) =>
        line"map(${key.schemaRef}, ${value.schemaRef})"
      case Type.Alias(
            ns,
            name,
            _,
            false
          ) =>
        line"$name.schema".addImport(ns + "." + name)
      case Type.Alias(ns, name, _, _) =>
        line"$name.underlyingSchema".addImport(ns + "." + name)
      case Type.Ref(ns, name) => line"$name.schema".addImport(ns + "." + name)
      case Type.ExternalType(
            _,
            fqn,
            maybeProviderImport,
            underlyingTpe,
            hint
          ) =>
        line"${underlyingTpe.schemaRef}.refined[$fqn](${renderNativeHint(hint)})"
          .addImport(maybeProviderImport.getOrElse(""))
    }

    private def schemaRefP(primitive: Primitive): String = primitive match {
      case Primitive.Unit       => "unit"
      case Primitive.ByteArray  => "bytes"
      case Primitive.Bool       => "boolean"
      case Primitive.String     => "string"
      case Primitive.Timestamp  => "timestamp"
      case Primitive.Byte       => "byte"
      case Primitive.Int        => "int"
      case Primitive.Short      => "short"
      case Primitive.Long       => "long"
      case Primitive.Float      => "float"
      case Primitive.Double     => "double"
      case Primitive.BigDecimal => "bigdecimal"
      case Primitive.BigInteger => "bigint"
      case Primitive.Uuid       => "uuid"
      case Primitive.Document   => "document"
      case Primitive.Nothing    => "???"
    }

    def name: Option[String] = tpe match {
      case Type.Alias(_, name, _, _) => Some(name)
      case Type.Ref(_, name)         => Some(name)
      case _                         => None
    }
  }

  private def renderNativeHint(hint: Hint.Native): Line =
    Line(
      smithy4s.recursion
        .cata(renderTypedNode)(hint.typedNode)
        .run(true)
        ._2
    )

  private def renderHint(hint: Hint): Option[Line] = hint match {
    case h: Hint.Native => renderNativeHint(h).some
    case Hint.IntEnum   => line"smithy4s.IntEnum()".some
    case _              => None
  }

  def renderId(name: String, ns: String = namespace): Line =
    line"""val id: $ShapeId_ = $ShapeId_("$ns", "$name")"""

  def renderHintsVal(hints: List[Hint]): Lines = if (hints.isEmpty) {
    lines(line"val hints : $Hints_ = $Hints_.empty")
  } else {
    line"val hints : $Hints_ = $Hints_".args {
      hints.flatMap(renderHint(_).toList)
    }
  }

  def memberHints(hints: List[Hint]): Line = {
    val h = hints.map(renderHint).collect { case Some(v) => v }
    if (h.isEmpty) Line.empty else h.intercalate(Line.comma)
  }

  def renderConstraintValidation(hints: List[Hint]): Line = {
    val tags = hints.collect { case t: Hint.Constraint => t }
    if (tags.isEmpty) Line.empty
    else {
      tags
        .map { tag =>
          line".validated(${renderNativeHint(tag.native)})"
        }
        .intercalate(Line.empty)
    }
  }

  private def shapeTag(name: String): String =
    s"$ShapeTag_.Companion[$name]"

  type TopLevel = Boolean
  type InCollection = Boolean

  type Contextual[A] = cats.data.Reader[TopLevel, A]
  type CString = Contextual[(InCollection, String)]

  implicit class ContextualOps(val str: String) {
    def write: CString = (false, str).pure[Contextual]
    def writeCollection: CString = (true, str).pure[Contextual]
  }

  implicit class CStringOps(val str: CString) {
    def runDefault = str.run(false)._2
  }

  private def renderTypedNode(tn: TypedNode[CString]): CString = tn match {
    case EnumerationTN(ref, _, _, name) =>
      (ref.show + "." + name + ".widen").write
    case StructureTN(ref, fields) =>
      val fieldStrings = fields.map {
        case (_, FieldTN.RequiredTN(value))     => value.runDefault
        case (_, FieldTN.OptionalSomeTN(value)) => s"Some(${value.runDefault})"
        case (_, FieldTN.OptionalNoneTN)        => "None"
      }
      s"${ref.show}(${fieldStrings.mkString(", ")})".write
    case NewTypeTN(ref, target) =>
      Reader(topLevel => {
        val (wroteCollection, text) = target.run(topLevel)
        if (wroteCollection && !topLevel)
          false -> text
        else
          false -> s"${ref.show}($text)"
      })

    case AltTN(ref, altName, AltValueTN.TypeAltTN(alt)) =>
      (s"${ref.show}" + "." + s"${altName.capitalize}Case(${alt.runDefault}).widen").write

    case AltTN(_, _, AltValueTN.ProductAltTN(alt)) =>
      alt.runDefault.write

    case CollectionTN(collectionType, values) =>
      val col = collectionType.tpe
      s"$col(${values.map(_.runDefault).mkString(", ")})".writeCollection
    case MapTN(values) =>
      s"Map(${values
        .map { case (k, v) => k.runDefault + " -> " + v.runDefault }
        .mkString(", ")})".writeCollection
    case PrimitiveTN(prim, value) =>
      renderPrimitive(prim)(value).write
  }

  private def renderPrimitive[T](prim: Primitive.Aux[T]): T => String =
    prim match {
      case Primitive.BigDecimal =>
        (bd: BigDecimal) => s"scala.math.BigDecimal($bd)"
      case Primitive.BigInteger => (bi: BigInt) => s"scala.math.BigInt($bi)"
      case Primitive.Unit       => _ => "()"
      case Primitive.Double     => _.toString
      case Primitive.Float      => _.toString
      case Primitive.Long       => _.toString
      case Primitive.Int        => _.toString
      case Primitive.Short      => _.toString
      case Primitive.Bool       => _.toString
      case Primitive.Uuid       => (uuid => s"java.util.UUID.fromString($uuid)")
      case Primitive.String => { raw =>
        import scala.reflect.runtime.universe._
        Literal(Constant(raw)).toString
      }
      case Primitive.Document => { (node: Node) =>
        node.accept(new NodeVisitor[String] {
          def arrayNode(x: ArrayNode): String = {
            val innerValues = x.getElements().asScala.map(_.accept(this))
            innerValues.mkString("smithy4s.Document.array(", ",", ")")
          }
          def booleanNode(x: BooleanNode): String =
            s"smithy4s.Document.fromBoolean(${x.getValue})"
          def nullNode(x: NullNode): String =
            "smithy4s.Document.nullDoc"
          def numberNode(x: NumberNode): String =
            s"smithy4s.Document.fromDouble(${x.getValue.doubleValue()})"
          def objectNode(x: ObjectNode): String = {
            val members = x.getMembers.asScala.map { member =>
              val key = s""""${member._1.getValue()}""""
              val value = member._2.accept(this)
              s"$key -> $value"
            }
            members.mkString("smithy4s.Document.obj(", ",", ")")
          }
          def stringNode(x: StringNode): String =
            s"""smithy4s.Document.fromString("${x.getValue}")"""
        })
      }
      case _ => _ => "null"
    }

  val syntaxImport = Set("smithy4s.schema.Schema._")

}
