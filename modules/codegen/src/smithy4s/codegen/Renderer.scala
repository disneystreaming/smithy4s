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

  def renderDecl(decl: Decl): RenderResult = decl match {
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
    case _ => RenderResult.empty
  }

  def renderPackageContents: RenderResult = {
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

  private def renderDeclPackageContents(decl: Decl): RenderResult = decl match {
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
  ): RenderResult = {

    val genName = name + "Gen"
    val opTraitName = name + "Operation"

    lines(
      block(s"trait $genName[F[_, _, _, _, _]]")(
        "self =>",
        newline,
        ops.map { op =>
          line(
            s"def ${op.methodName}(${op.renderArgs}) : F[${op.renderAlgParams}]"
          ).addImports(
            if (op.errors.isEmpty) Set() else Set(s"$genName.${op.name}Error")
          )
        },
        newline,
        s"def transform[G[_, _, _, _, _]](transformation : $Transformation_[F, G]) : $genName[G] = new Transformed(transformation)",
        block(
          s"class Transformed[G[_, _, _, _, _]](transformation : $Transformation_[F, G]) extends $genName[G]"
        ) {
          ops.map { op =>
            val opName = op.methodName
            s"def $opName(${op.renderArgs}) = transformation[${op.renderAlgParams}](self.$opName(${op.renderParams}))"
          }
        }
      ),
      newline,
      obj(
        genName,
        ext = s"$Service_[$genName, $opTraitName]"
      )(
        newline,
        line(
          s"def apply[F[_]](implicit F: smithy4s.Monadic[$genName, F]): F.type = F"
        ),
        newline,
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        newline,
        line(s"val endpoints = List").args(ops.map(_.name)),
        newline,
        line(s"""val version: String = "$version""""),
        newline,
        if (ops.isEmpty) {
          line(
            s"""def endpoint[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) = sys.error("impossible")"""
          )
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
              s"def ${op.methodName}(${op.renderArgs}) = ${op.name}()"
            case op if op.hints.contains(Hint.PackedInputs) =>
              s"def ${op.methodName}(${op.renderArgs}) = ${op.name}(input)"
            case op =>
              s"def ${op.methodName}(${op.renderArgs}) = ${op.name}(${op.input.render}(${op.renderParams}))"
          }
        },
        newline,
        s"def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[$opTraitName, P]): $genName[P] = reified.transform(transformation)",
        newline,
        line(
          s"def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: $genName[P], transformation: smithy4s.Transformation[P, P1]): $genName[P1] = alg.transform(transformation)"
        ),
        newline,
        block(
          s"def asTransformation[P[_, _, _, _, _]](impl : $genName[P]): smithy4s.Transformation[$opTraitName, P] = new smithy4s.Transformation[$opTraitName, P]"
        ) {
          if (ops.isEmpty) {
            line(
              s"""def apply[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = sys.error("impossible")"""
            )
          } else {
            block(
              s"def apply[I, E, O, SI, SO](op : $opTraitName[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match "
            ) {
              ops.map {
                case op if op.input == Type.unit =>
                  s"case ${op.name}() => impl.${op.methodName}(${op.renderParams})"
                case op if op.hints.contains(Hint.PackedInputs) =>
                  s"case ${op.name}(input) => impl.${op.methodName}(${op.renderParams})"
                case op =>
                  s"case ${op.name}(${op.input.render}(${op.renderParams})) => impl.${op.methodName}(${op.renderParams})"

              }
            }
          }
        },
        ops.map(renderOperation(name, _))
      ),
      newline,
      line(
        s"sealed trait $opTraitName[Input, Err, Output, StreamedInput, StreamedOutput]"
      ),
      newline
    )
  }

  private def renderOperation(
      serviceName: String,
      op: Operation
  ): RenderResult = {
    val params = if (op.input != Type.unit) {
      s"input: ${op.input.render}"
    } else ""
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

    val additionalImports =
      if (op.input == Type.unit || op.output == Type.unit) syntaxImport
      else Set.empty[String]

    lines(
      s"case class $opName($params) extends $traitName[${op.renderAlgParams}]",
      obj(
        opName,
        ext = s"$Endpoint_[${traitName}, ${op.renderAlgParams}]$errorable"
      )(
        renderId(op.name, op.originalNamespace),
        s"val input: $Schema_[${op.input.render}] = ${op.input.schemaRef}.addHints(smithy4s.internals.InputOutput.Input)",
        s"val output: $Schema_[${op.output.render}] = ${op.output.schemaRef}.addHints(smithy4s.internals.InputOutput.Output)",
        renderStreamingSchemaVal("streamedInput", op.streamedInput),
        renderStreamingSchemaVal("streamedOutput", op.streamedOutput),
        renderHintsVal(op.hints),
        s"def wrap(input: ${op.input.render}) = ${opName}($input)",
        renderErrorable(op)
      ),
      renderedErrorUnion
    ).addImports(op.imports).addImports(additionalImports)
  }

  private def renderStreamingSchemaVal(
      valName: String,
      sField: Option[StreamingField]
  ) = sField match {
    case Some(StreamingField(name, tpe, hints)) =>
      val mh = if (hints.isEmpty) "" else s".addHints${memberHints(hints)}"
      line(
        s"""val $valName : $StreamingSchema_[${tpe.render}] = $StreamingSchema_("$name", ${tpe.schemaRef}$mh)"""
      )
    case None =>
      line(
        s"""val $valName : $StreamingSchema_[Nothing] = $StreamingSchema_.nothing"""
      )
  }

  private def renderProtocol(name: String, hints: List[Hint]): RenderResult = {
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
  ): RenderResult = {
    val decl = s"case class $name(${renderArgs(fields)})"
    val imports = fields.foldMap(_.tpe.imports) ++ syntaxImport
    lines(
      if (hints.contains(Hint.Error)) {
        block(s"$decl extends Throwable") {
          fields
            .find(_.name == "message")
            .map(field => (field.tpe, field.required))
            .filter { case (tpe, _) =>
              tpe.dealiased == Type.PrimitiveType(Primitive.String)
            } match {
            case Some((tpe, true)) if (tpe.dealiased == tpe) =>
              line("override def getMessage() : String = message")
            case Some((tpe, false)) if (tpe.dealiased == tpe) =>
              line("override def getMessage() : String = message.orNull")
            case Some((_, true)) =>
              line("override def getMessage() : String = message.value")
            case Some((_, false)) =>
              line(
                "override def getMessage() : String = message.map(_.value).orNull"
              )
            case None => empty
          }
        }
      } else line(decl),
      obj(name, ext = shapeTag(name))(
        renderId(originalName),
        newline,
        renderHintsVal(hints),
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
                  // format: off
                  s"""${tpe.schemaRef}.$req[$name]("$realName", _.$fieldName).addHints($mh)${renderFieldConstraintCheck(hints, tpe)}"""
                  // format: on
                }
            }
          if (fields.size <= 22) {
            val definition = if (recursive) "recursive(struct" else "struct"
            line(s"implicit val schema: $Schema_[$name] = $definition")
              .args(renderedFields)
              .block(s"$name.apply")
              .appendToLast(".withId(id).addHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          } else {
            val definition =
              if (recursive) "recursive(struct.genericArity"
              else "struct.genericArity"
            line(s"implicit val schema: $Schema_[$name] = $definition")
              .args(renderedFields)
              .block(
                line(s"arr => new $name").args(
                  fields.zipWithIndex.map {
                    case (Field(_, _, tpe, required, _), idx) =>
                      val scalaTpe =
                        if (required) tpe.render
                        else s"Option[${tpe.render}]"
                      line(s"arr($idx).asInstanceOf[$scalaTpe]")
                  }
                )
              )
              .appendToLast(".withId(id).addHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          }
        } else {
          line(
            s"implicit val schema: $Schema_[$name] = constant($name()).withId(id).addHints(hints)"
          )
        }
      )
    ).addImports(imports)
  }

  private def renderErrorable(op: Operation): RenderResult = {
    val errorName = op.name + "Error"

    if (op.errors.isEmpty) RenderResult.empty
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
            line(s"case $errorName.${name}Case(e) => e")
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
  ): RenderResult = {
    def caseName(altName: String) =
      altName.dropWhile(_ == '_').capitalize + "Case"
    val caseNames = alts.map(_.name).map(caseName)
    val imports = alts.foldMap(_.tpe.imports) ++ syntaxImport

    lines(
      s"sealed trait $name extends scala.Product with scala.Serializable",
      obj(name, ext = shapeTag(name))(
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        newline,
        alts.map { case Alt(altName, _, tpe, _) =>
          val cn = caseName(altName)
          s"case class $cn(${uncapitalise(altName)}: ${tpe.render}) extends $name"
        },
        newline,
        alts.map { case Alt(altName, realName, tpe, altHints) =>
          val cn = caseName(altName)
          block(s"object $cn")(
            renderHintsVal(altHints),
            s"val schema: $Schema_[$cn] = bijection(${tpe.schemaRef}, $cn(_), _.${uncapitalise(altName)})",
            s"""val alt = schema.oneOf[$name]("$realName")"""
          )
        },
        newline, {
          val union =
            if (error)
              s"implicit val schema: $unionSchema_[$name] = union"
            else if (recursive)
              s"implicit val schema: $Schema_[$name] = recursive(union"
            else
              s"implicit val schema: $Schema_[$name] = union"
          line(union)
            .args {
              caseNames.map(_ + ".alt")
            }
            .block {
              caseNames.map { caseName =>
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

  private def renderArgs(fields: List[Field]): String = fields
    .map {
      case Field(name, _, tpe, true, _) => name + ": " + tpe.render
      case Field(name, _, tpe, false, _) =>
        name + ": Option[" + tpe.render + "] = None"
    }
    .mkString(", ")

  private def renderEnum(
      name: String,
      originalName: String,
      values: List[EnumValue],
      hints: List[Hint]
  ): RenderResult =
    lines(
      block(
        s"sealed abstract class $name(_value: String, _ordinal: Int) extends $Enumeration_.Value"
      )(
        "override val value : String = _value",
        "override val ordinal: Int = _ordinal",
        s"override val hints: $Hints_ = $Hints_.empty"
      ),
      obj(name, ext = s"$Enumeration_[$name]", w = shapeTag(name))(
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        newline,
        values.map { case e @ EnumValue(value, ordinal, _, _) =>
          line(
            s"""case object ${e.name} extends $name("$value", $ordinal)"""
          )
        },
        newline,
        line(s"val values: List[$name] = List").args(
          values.toList.map(_.name)
        ),
        lines(
          s"implicit val schema: $Schema_[$name] = enumeration(values).withId(id).addHints(hints)"
        )
      )
    ).addImports(syntaxImport)

  private def renderTypeAlias(
      name: String,
      originalName: String,
      tpe: Type,
      hints: List[Hint]
  ): RenderResult = {
    val imports = tpe.imports ++ Set("smithy4s.Newtype") ++ syntaxImport

    val trailingCalls =
      s".withId(id).addHints(hints)${renderConstraintCheck(hints)}"

    lines(
      obj(name, extensions = List(s"Newtype[${tpe.render}]"))(
        renderId(originalName),
        renderHintsVal(hints),
        s"val underlyingSchema : $Schema_[${tpe.render}] = ${tpe.schemaRef}$trailingCalls",
        lines(
          s"implicit val schema : $Schema_[$name] = bijection(underlyingSchema, $name(_), (_ : $name).value)"
        )
      )
    ).addImports(imports)
  }

  private implicit class OperationExt(op: Operation) {
    def renderArgs =
      if (op.input == Type.unit) ""
      else if (op.hints.contains(Hint.PackedInputs)) {
        "input: " + renderInput
      } else self.renderArgs(op.params)

    def renderParams =
      if (op.input == Type.unit) ""
      else if (op.hints.contains(Hint.PackedInputs)) {
        "input"
      } else op.params.map(_.name).mkString(", ")

    def methodName = uncapitalise(op.name)

    def imports =
      (op.input :: op.output :: op.params.map(_.tpe) ++ op.errors)
        .foldMap(_.imports)

    def renderInput = op.input.render
    def renderOutput = op.output.render
    def renderError = if (op.errors.isEmpty) "Nothing" else op.name + "Error"

    def renderStreamedInput =
      op.streamedInput.map(_.tpe.render).getOrElse("Nothing")
    def renderStreamedOutput =
      op.streamedOutput.map(_.tpe.render).getOrElse("Nothing")

    def renderAlgParams =
      s"$renderInput, $renderError, $renderOutput, $renderStreamedInput, $renderStreamedOutput"
  }

  implicit class TypeRefExt(tpe: Type.Ref) {
    def renderFull: String = s"${tpe.namespace}.${tpe.name}"
  }

  implicit class TypeExt(tpe: Type) {

    def render: String = importsAndRender._2
    def imports: Set[String] = importsAndRender._1

    /**
      * Returns both the rendered string of the type,
      * and the necessary imports.
      */
    def importsAndRender: (Set[String], String) = tpe match {
      case Type.PrimitiveType(p) => importAndRenderP(p)
      case Type.List(member) =>
        val (imports, m) = member.importsAndRender
        imports -> s"List[$m]"
      case Type.Set(member) =>
        val (imports, m) = member.importsAndRender
        imports -> s"Set[$m]"
      case Type.Map(key, value) =>
        val (kimports, k) = key.importsAndRender
        val (vimports, v) = value.importsAndRender
        (kimports ++ vimports) -> s"Map[$k, $v]"
      case Type.Alias(ns, name, Type.PrimitiveType(_)) =>
        Set(s"$ns.$name") -> name
      case Type.Alias(ns, name, aliased) =>
        val (imports, t) = aliased.importsAndRender
        imports + s"$ns.$name" -> t
      case Type.Ref(ns, name) =>
        val imports =
          if (ns != namespace) Set(s"$ns.$name") else Set.empty[String]
        imports -> name
    }

    private def importAndRenderP(p: Primitive): (Set[String], String) =
      p match {
        case Primitive.Unit       => Set.empty -> "Unit"
        case Primitive.ByteArray  => Set("smithy4s.ByteArray") -> "ByteArray"
        case Primitive.Bool       => Set.empty -> "Boolean"
        case Primitive.String     => Set.empty -> "String"
        case Primitive.Timestamp  => Set("smithy4s.Timestamp") -> "Timestamp"
        case Primitive.Byte       => Set.empty -> "Byte"
        case Primitive.Int        => Set.empty -> "Int"
        case Primitive.Short      => Set.empty -> "Short"
        case Primitive.Long       => Set.empty -> "Long"
        case Primitive.Float      => Set.empty -> "Float"
        case Primitive.Double     => Set.empty -> "Double"
        case Primitive.BigDecimal => Set.empty -> "BigDecimal"
        case Primitive.BigInteger => Set.empty -> "BigInteger"
        case Primitive.Uuid       => Set("java.util.UUID") -> "UUID"
        case Primitive.Document   => Set("smithy4s.Document") -> "Document"
      }

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
    }

    def name: Option[String] = tpe match {
      case Type.Alias(_, name, _) => Some(name)
      case Type.Ref(_, name)      => Some(name)
      case _                      => None
    }
  }

  private def renderHint(hint: Hint): Option[String] = hint match {
    case Hint.Native(typedNode) =>
      smithy4s.recursion.cata(renderTypedNode)(typedNode).run(true)._2.some
    case Hint.IntEnum => "smithy4s.IntEnum()".some
    case _            => None
  }

  def renderId(name: String, ns: String = namespace): RenderResult =
    line(s"""val id: $ShapeId_ = $ShapeId_("$ns", "$name")""")

  def renderHintsVal(hints: List[Hint]): RenderResult = if (hints.isEmpty) {
    line(s"val hints : $Hints_ = $Hints_.empty")
  } else {
    line(s"val hints : $Hints_ = $Hints_").args {
      hints.flatMap(renderHint(_).toList)
    }
  }

  def memberHints(hints: List[Hint]): String = {
    val h = hints.map(renderHint).collect { case Some(v) => v }
    if (h.isEmpty) "" else h.mkString(", ")
  }

  def renderConstraintCheck(hints: List[Hint]): String = {
    val tags = hints.collect { case Hint.Constraint(tr) => tr }
    if (tags.isEmpty) ""
    else {
      tags.map(t => s".checked[${t.renderFull}]").mkString(".")
    }
  }

  def renderFieldConstraintCheck(hints: List[Hint], tpe: Type): String = {
    val tags = hints.collect { case Hint.Constraint(tr) => tr }
    if (tags.isEmpty) ""
    else {
      tags.map(t => s".checked[${t.renderFull}, ${tpe.render}]").mkString(".")
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
      (ref.show + "." + name).write
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
      case Primitive.BigInteger => (bi: BigInt) => s"scala.math.BigInteger($bi)"
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
