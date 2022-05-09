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

package smithy4s.codegen

import cats.data.NonEmptyList
import cats.data.Reader
import cats.syntax.all._
import smithy4s.codegen.TypedNode._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node._
import LineSyntax.LineInterpolator
import ToLines.lineToLines
import smithy4s.codegen.Primitive.Nothing

import scala.jdk.CollectionConverters._

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
    case TypeAlias(name, originalName, tpe, hints) =>
      renderTypeAlias(name, originalName, tpe, hints)
    case Enumeration(name, originalName, values, hints) =>
      renderEnum(name, originalName, values, hints)
    case _ => Lines.empty
  }

  def renderPackageContents: Lines = {
    val typeAliases = compilationUnit.declarations.collect {
      case TypeAlias(name, _, _, _) =>
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
          s"object $name extends smithy4s.Service.Provider[${name}Gen, ${name}Operation]"
        )(
          s"def apply[F[_]](implicit F: $name[F]): F.type = F",
          s"def service : smithy4s.Service[${name}Gen, ${name}Operation] = ${name}Gen",
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
        renderHintsValWithId(hints),
        newline,
        Lines("val endpoints = List").args(ops.map(_.name)),
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
        line"def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[$opTraitName, P]): $genName[P] = reified.transform(transformation)",
        newline,
        line"def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: $genName[P], transformation: smithy4s.Transformation[P, P1]): $genName[P1] = alg.transform(transformation)",
        newline,
        block(
          line"def asTransformation[P[_, _, _, _, _]](impl : $genName[P]): smithy4s.Transformation[$opTraitName, P] = new smithy4s.Transformation[$opTraitName, P]"
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
    ).addImports(syntaxImport)
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
        t.name.map(n => Alt(n, t))
      }
      name = opName + "Error"
    } yield Union(name, name, alts)

    val renderedErrorUnion = errorUnion.foldMap {
      case Union(name, originalName, alts, recursive, hints) =>
        renderUnion(name, originalName, alts, recursive, hints, error = true)
    }

    lines(
      line"case class $opName($params) extends $traitName[${op.renderAlgParams}]",
      obj(
        opName,
        ext = line"$Endpoint_[${traitName}, ${op.renderAlgParams}]$errorable"
      )(
        renderId(op.name, op.originalNamespace),
        line"val input: $Schema_[${op.input}] = ${op.input.schemaRef}.withHints(smithy4s.internals.InputOutput.Input)",
        line"val output: $Schema_[${op.output}] = ${op.output.schemaRef}.withHints(smithy4s.internals.InputOutput.Output)",
        renderStreamingSchemaVal("streamedInput", op.streamedInput),
        renderStreamingSchemaVal("streamedOutput", op.streamedOutput),
        renderHintsValWithId(op.hints),
        line"def wrap(input: ${op.input}) = ${opName}($input)",
        renderErrorable(op)
      ),
      renderedErrorUnion
    ).addImports(syntaxImport ++ {
      if (op.errors.isEmpty) Set.empty[String]
      else Set(s"${serviceName}Gen.$errorName")
    })
  }

  private def renderStreamingSchemaVal(
      valName: String,
      sField: Option[StreamingField]
  ): Line = sField match {
    case Some(StreamingField(name, tpe, hints)) =>
      val mh = if (hints.isEmpty) "" else s".withHints(${memberHints(hints)})"
      line"""val $valName : $StreamingSchema_[${tpe}] = $StreamingSchema_("$name", ${tpe.schemaRef}$mh)"""
    case None =>
      line"""val $valName : $StreamingSchema_[Nothing] = $StreamingSchema_.nothing"""
  }

  private def renderProtocol(name: String, hints: List[Hint]): Lines = {
    hints.collectFirst({ case p: Hint.Protocol => p }).foldMap { protocol =>
      val protocolTraits = protocol.traits
        .map(t => s"${t.namespace}.${t.name.capitalize}")
        .mkString(", ")
      lines(
        newline,
        block(
          s"implicit val protocol: smithy4s.Protocol[$name] = new smithy4s.Protocol[$name]"
        ) {
          s"def schemas: smithy4s.SchemaIndex = smithy4s.SchemaIndex($protocolTraits)"
        }
      )
    }
  }

  private def renderProduct(
      name: String,
      originalName: String,
      fields: List[Field],
      recursive: Boolean,
      hints: List[Hint]
  ): Lines = {
    val decl = line"case class $name(${renderArgs(fields)})"
    val imports = syntaxImport
    lines(
      if (hints.contains(Hint.Error)) {
        block(line"${decl} extends Throwable") {
          fields
            .find(_.name == "message")
            .map(field => (field.tpe, field.required))
            .filter { case (tpe, _) =>
              tpe.dealiased == Type.PrimitiveType(Primitive.String)
            } match {
            case Some((tpe, true)) if (tpe.dealiased == tpe) =>
              line"override def getMessage() : String = message"
            case Some((tpe, false)) if (tpe.dealiased == tpe) =>
              line"override def getMessage() : String = message.orNull"
            case Some((_, true)) =>
              line"override def getMessage() : String = message.value"
            case Some((_, false)) =>
              line"override def getMessage() : String = message.map(_.value).orNull"

            case None => Line.empty
          }
        }
      } else decl,
      obj(name, line"${shapeTag(name)}")(
        renderId(originalName),
        newline,
        renderHintsValWithId(hints),
        renderProtocol(name, hints),
        newline,
        if (fields.nonEmpty) {
          val renderedFields =
            fields.map {
              case Field(fieldName, realName, tpe, required, hints) =>
                val req = if (required) "required" else "optional"
                if (hints.isEmpty) {
                  s"""${tpe.schemaRef}.$req[$name]("$realName", _.$fieldName)"""
                } else {
                  val mh = memberHints(hints)
                  s"""${tpe.schemaRef}.$req[$name]("$realName", _.$fieldName).withHints($mh)"""
                }
            }
          if (fields.size <= 22) {
            val definition = if (recursive) "recursive(struct" else "struct"
            Lines(s"val schema: $Schema_[$name] = $definition")
              .args(renderedFields)
              .block(s"$name.apply")
              .appendToLast(".withHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          } else {
            val definition =
              if (recursive) "recursive(bigStruct" else "bigStruct"
            Lines(s"val schema: $Schema_[$name] = $definition")
              .args(renderedFields)
              .args(renderedFields)
              .block(
                Lines(s"arr => new $name").args(
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
              .appendToLast(".withHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          }
        } else {
          line"val schema: $Schema_[$name] = constant($name()).withHints(hints)"
        },
        s"implicit val staticSchema : $Static_[$Schema_[$name]] = $Static_(schema)"
      )
    ).addImports(imports)
  }

  private def renderErrorable(op: Operation): Lines = {
    val errorName = op.name + "Error"

    if (op.errors.isEmpty) Lines.empty
    else
      lines(
        s"override val errorable: Option[$Errorable_[$errorName]] = Some(this)",
        s"val error: $errorUnion_.Schema[$errorName] = $errorName.schema",
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
    def caseName(altName: String) =
      altName.dropWhile(_ == '_').capitalize + "Case"
    val caseNames = alts.map(_.name).map(caseName)
    val imports = /*alts.foldMap(_.tpe.imports) ++*/ syntaxImport

    lines(
      s"sealed trait $name extends scala.Product with scala.Serializable",
      obj(name, line"${shapeTag(name)}")(
        renderId(originalName),
        newline,
        renderHintsValWithId(hints),
        newline,
        alts.map { case Alt(altName, _, tpe, _) =>
          val cn = caseName(altName)
          line"case class $cn(${uncapitalise(altName)}: ${tpe}) extends $name"
        },
        newline,
        alts.map { case Alt(altName, realName, tpe, altHints) =>
          val cn = caseName(altName)
          block(s"object $cn")(
            renderHintsVal(altHints),
            s"val schema: $Schema_[$cn] = bijection(${tpe.schemaRef}.withHints(hints), $cn(_), _.${uncapitalise(altName)})",
            s"""val alt = schema.oneOf[$name]("$realName")"""
          )
        },
        newline, {
          val union =
            if (error)
              s"val schema: $errorUnion_.Schema[$name] = errors"
            else if (recursive)
              s"val schema: $Schema_[$name] = recursive(union"
            else
              s"val schema: $Schema_[$name] = union"
          Lines(union)
            .args {
              caseNames.map(_ + ".alt")
            }
            .block {
              caseNames.map { caseName =>
                s"case c : $caseName => $caseName.alt(c)"
              }
            }
            .appendToLast(
              if (error) "" else ".withHints(hints)"
            )
            .appendToLast(if (recursive) ")" else "")
        },
        s"implicit val staticSchema : $Static_[$Schema_[$name]] = $Static_(schema)"
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
    .intercalate(Line(", "))

  private def renderEnum(
      name: String,
      originalName: String,
      values: List[EnumValue],
      hints: List[Hint]
  ): Lines = lines(
    s"sealed abstract class $name(val value: String, val ordinal: Int) extends scala.Product with scala.Serializable",
    obj(name, ext = line"$Enumeration_[$name]", w = line"${shapeTag(name)}")(
      renderId(originalName),
      newline,
      renderHintsValWithId(hints),
      newline,
      values.map { case e @ EnumValue(value, ordinal, _, _) =>
        line"""case object ${e.className} extends $name("$value", $ordinal)"""
      },
      newline,
      Lines(s"val values: List[$name] = List").args(
        values.map(_.className)
      ),
      newline,
      line"def to(e: $name) : (String, Int) = (e.value, e.ordinal)",
      lines(
        s"val schema: $Schema_[$name] = enumeration(to, valueMap, ordinalMap).withHints(hints)",
        s"implicit val staticSchema : $Static_[$Schema_[$name]] = $Static_(schema)"
      )
    )
  ).addImports(syntaxImport)

  private def renderTypeAlias(
      name: String,
      originalName: String,
      tpe: Type,
      hints: List[Hint]
  ): Lines = {
    val imports = /* tpe.imports ++*/ Set("smithy4s.Newtype") ++ syntaxImport
    lines(
      obj(name, line"Newtype[$tpe]")(
        renderId(originalName),
        renderHintsValWithId(hints),
        line"val underlyingSchema : $Schema_[${tpe}] = ${tpe.schemaRef}.withHints(hints)",
        lines(
          s"val schema : $Schema_[$name] = bijection(underlyingSchema, $name(_), (_ : $name).value)",
          s"implicit val staticSchema : $Static_[$Schema_[$name]] = $Static_(schema)"
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

    def renderParams =
      if (op.input == Type.unit) ""
      else if (op.hints.contains(Hint.PackedInputs)) {
        "input"
      } else op.params.map(_.name).mkString(", ")

    def methodName = uncapitalise(op.name)

    def renderAlgParams = {
      line"${op.input}, ${if (op.errors.isEmpty) "Nothing"
      else op.name + "Error"}, ${op.output}, ${op.streamedInput.map(_.tpe).getOrElse(Type.PrimitiveType(Nothing))}, ${op.streamedOutput
        .map(_.tpe)
        .getOrElse(Type.PrimitiveType(Nothing))}"
    }
  }

  implicit class TypeExt(tpe: Type) {

    def schemaRef: String = tpe match {
      case Type.PrimitiveType(p) => schemaRefP(p)
      case Type.List(member)     => s"list(${member.schemaRef})"
      case Type.Set(member)      => s"set(${member.schemaRef})"
      case Type.Map(key, value)  => s"map(${key.schemaRef}, ${value.schemaRef})"
      case Type.Alias(_, name, Type.PrimitiveType(_)) =>
        s"$name.schema"
      case Type.Alias(_, name, _) =>
        s"$name.underlyingSchema"
      case Type.Ref(_, name) => s"$name.schema"
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
      case Primitive.Nothing    => "Nothing"
    }

    def name: Option[String] = tpe match {
      case Type.Alias(_, name, _) => Some(name)
      case Type.Ref(_, name)      => Some(name)
      case _                      => None
    }
  }

  private def toCamelCase(value: String): String = {
    val (_, output) = value.foldLeft((false, "")) {
      case ((wasLastSkipped, str), c) =>
        if (c.isLetterOrDigit) {
          val newC =
            if (wasLastSkipped) c.toString.capitalize else c.toString
          (false, str + newC)
        } else {
          (true, str)
        }
    }
    output
  }

  private def enumValueClassName(
      name: Option[String],
      value: String,
      ordinal: Int
  ) = {
    name.getOrElse {
      val camel = toCamelCase(value).capitalize
      if (camel.nonEmpty) camel else "Value" + ordinal
    }

  }

  private implicit class EnumValueOps(enumValue: EnumValue) {
    def className =
      enumValueClassName(enumValue.name, enumValue.value, enumValue.ordinal)
  }

  private def renderHint(hint: Hint): Option[String] = hint match {
    case Hint.Native(typedNode) =>
      smithy4s.recursion.cata(renderTypedNode)(typedNode).run(true)._2.some
    case _ => None
  }

  def renderId(name: String, ns: String = namespace): Line =
    line"""val id: $ShapeId_ = $ShapeId_("$ns", "$name")"""

  def renderHintsValWithId(hints: List[Hint]): Lines =
    Lines(s"val hints : $Hints_ = $Hints_").args {
      "id" :: hints.flatMap(renderHint(_).toList)
    }

  def renderHintsVal(hints: List[Hint]): Lines =
    Lines(s"val hints : $Hints_ = $Hints_").args {
      hints.flatMap(renderHint(_).toList)
    }

  def memberHints(hints: List[Hint]): String = {
    val h = hints.map(renderHint).collect { case Some(v) => v }
    if (h.isEmpty) "" else h.mkString(", ")
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
    case EnumerationTN(ref, value, ordinal, name) =>
      (ref.show + "." + enumValueClassName(name, value, ordinal)).write
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

    case AltTN(ref, altName, alt) =>
      (s"${ref.show}" + "." + s"${altName.capitalize}Case(${alt.runDefault})").write

    case ListTN(values) =>
      s"List(${values.map(_.runDefault).mkString(", ")})".writeCollection
    case SetTN(values) =>
      s"Set(${values.map(_.runDefault).mkString(", ")})".writeCollection
    case MapTN(values) =>
      s"Map(${values
        .map { case (k, v) => k.runDefault + " -> " + v.runDefault }
        .mkString(", ")})".write
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

  val syntaxImport = Set("smithy4s.syntax._")

}
