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
import Line._

import scala.jdk.CollectionConverters._
import LineSyntax.LineInterpolator
import ToLines.lineToLines
import smithy4s.codegen.LineSegment._

object Renderer {

  case class Result(namespace: String, name: String, content: String)

  def apply(unit: CompilationUnit): List[Result] = {
    val r = new Renderer(unit)

    val pack = Result(
      unit.namespace,
      "package",
      r.renderPackageContents.list
        .map(_.segments.toList.map(_.show).mkString)
        .mkString(
          System.lineSeparator()
        )
    )

    val classes = unit.declarations.map { decl =>
      val renderResult = r.renderDecl(decl)
      val p = s"package ${unit.namespace}"

      val nameCollisions: Set[String] = renderResult.list
        .flatMap(_.segments.toList)
        .distinct
        .collect {
          case NameRef(_, name) => name
          case NameDef(name)    => name
        }
        .groupBy(identity)
        .filter(_._2.size > 1)
        .keySet

      val allImports: List[String] = renderResult.list
        .flatMap { line =>
          line.segments.toList.collect {
            case tr @ NameRef(pkg, name)
                if pkg.nonEmpty && !nameCollisions.contains(name) &&
                  !pkg.mkString(".").equalsIgnoreCase(unit.namespace) =>
              tr.show
            case Import(value) => value
          }
        }

      val code: List[String] = renderResult.list
        .map { line =>
          line.segments.toList.collect {
            case Literal(value) => value
            case NameDef(name)  => name
            case tr: NameRef =>
              if (nameCollisions.contains(tr.name)) tr.asValue else tr.name
          }.mkString
        }

      val allLines: List[String] = List(p, "") ++
        allImports.toSet.map("import " + _) ++
        List("") ++ code

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
    case p: Product => renderProduct(p)
    case union @ Union(_, originalName, alts, recursive, hints) =>
      renderUnion(union.nameRef, originalName, alts, recursive, hints)
    case ta @ TypeAlias(_, originalName, tpe, _, hints) =>
      renderTypeAlias(ta.nameRef, originalName, tpe, hints)
    case enumeration @ Enumeration(_, originalName, values, hints) =>
      renderEnum(enumeration.nameRef, originalName, values, hints)
    case _ => Lines.empty
  }

  def renderPackageContents: Lines = {
    val typeAliases = compilationUnit.declarations.collect {
      case TypeAlias(name, _, _, _, _) =>
        line"type $name = ${compilationUnit.namespace}.${name}.Type"
    }

    val blk =
      block(
        line"package object ${compilationUnit.namespace.split('.').last}"
      )(
        compilationUnit.declarations.map(renderDeclPackageContents),
        newline,
        typeAliases,
        newline
      )
    val parts = compilationUnit.namespace.split('.').filter(_.nonEmpty)
    if (parts.size > 1) {
      lines(
        line"package ${parts.dropRight(1).mkString(".")}",
        newline,
        blk
      )
    } else blk
  }

  private def renderDeclPackageContents(decl: Decl): Lines = decl match {
    case s: Service =>
      val name = s.name
      lines(
        line"type ${NameDef(name)}[F[_]] = $Monadic_[${name}Gen, F]",
        block(
          line"object ${NameRef(name)} extends $Service_.Provider[${name}Gen, ${name}Operation]"
        )(
          line"def apply[F[_]](implicit F: $name[F]): F.type = F",
          line"def service: $Service_[${name}Gen, ${name}Operation] = ${name}Gen",
          line"val id: $ShapeId_ = service.id"
        )
      )
    case _ => Lines.empty
  }

  private def renderService(
      name: String,
      originalName: String,
      ops: List[Operation],
      hints: List[Hint],
      version: String
  ): Lines = {

    val genName = name + "Gen"
    val genNameRef = NameRef(genName)
    val opTraitName = name + "Operation"
    val opTraitNameRef = NameRef(opTraitName)

    lines(
      block(line"trait ${NameDef(genName)}[F[_, _, _, _, _]]")(
        line"self =>",
        newline,
        ops.map { op =>
          line"def ${op.methodName}(${op.renderArgs}) : F[${op.renderAlgParams(genName)}]"

        },
        newline,
        line"def transform[G[_, _, _, _, _]](transformation : $Transformation_[F, G]) : $genNameRef[G] = new Transformed(transformation)",
        block(
          line"class $Transformed_[G[_, _, _, _, _]](transformation : $Transformation_[F, G]) extends $genNameRef[G]"
        ) {
          ops.map { op =>
            val opName = op.methodName
            line"def $opName(${op.renderArgs}) = transformation[${op
              .renderAlgParams(genName)}](self.$opName(${op.renderParams}))"
          }
        }
      ),
      newline,
      obj(
        genNameRef,
        ext = line"$Service_[$genNameRef, $opTraitNameRef]"
      )(
        newline,
        line"def apply[F[_]](implicit F: $Monadic_[$genNameRef, F]): F.type = F",
        newline,
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        newline,
        line"val endpoints: List[$Endpoint_[$opTraitNameRef, _, _, _, _, _]] = List"
          .args(ops.map(_.name)),
        newline,
        line"""val version: String = "$version"""",
        newline,
        if (ops.isEmpty) {
          line"""def endpoint[I, E, O, SI, SO](op : $opTraitNameRef[I, E, O, SI, SO]) = sys.error("impossible")"""

        } else {
          block(
            line"def endpoint[I, E, O, SI, SO](op : $opTraitNameRef[I, E, O, SI, SO]) = op match"
          ) {
            ops.map {
              case op if op.input != Type.unit =>
                line"case ${op.name}(input) => (input, ${op.name})"
              case op =>
                line"case ${op.name}() => ((), ${op.name})"
            }
          }
        },
        newline,
        block(
          line"object ${NameRef("reified")} extends $genNameRef[$opTraitNameRef]"
        ) {
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
        line"def transform[P[_, _, _, _, _]](transformation: $Transformation_[$opTraitNameRef, P]): $genNameRef[P] = reified.transform(transformation)",
        newline,
        line"def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: $genNameRef[P], transformation: $Transformation_[P, P1]): $genNameRef[P1] = alg.transform(transformation)",
        newline,
        block(
          line"def asTransformation[P[_, _, _, _, _]](impl : $genNameRef[P]): $Transformation_[$opTraitNameRef, P] = new $Transformation_[$opTraitNameRef, P]"
        ) {
          if (ops.isEmpty) {
            line"""def apply[I, E, O, SI, SO](op : $opTraitNameRef[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = sys.error("impossible")""".toLines
          } else {
            block(
              line"def apply[I, E, O, SI, SO](op : $opTraitNameRef[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match "
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
      line"sealed trait ${NameDef(opTraitName)}[Input, Err, Output, StreamedInput, StreamedOutput]",
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
    val opNameRef = NameRef(opName)
    val traitName = line"${serviceName}Operation"
    val input =
      if (op.input == Type.unit) "" else "input"
    val errorName =
      if (op.errors.isEmpty) line"Nothing" else line"${op.name}Error"

    val errorable = if (op.errors.nonEmpty) {
      line" with $Errorable_[$errorName]"
    } else Line.empty

    val errorUnion: Option[Union] = for {
      errorNel <- NonEmptyList.fromList(op.errors)
      alts <- errorNel.traverse { t =>
        t.name.map(n => Alt(n, UnionMember.TypeCase(t)))
      }
      name = opName + "Error"
    } yield Union(name, name, alts)

    val renderedErrorUnion = errorUnion.foldMap {
      case union @ Union(_, originalName, alts, recursive, hints) =>
        renderUnion(
          union.nameRef,
          originalName,
          alts,
          recursive,
          hints,
          error = true
        )
    }

    lines(
      line"case class ${NameDef(opName)}($params) extends $traitName[${op
        .renderAlgParams(serviceName + "Gen")}]",
      obj(
        opNameRef,
        ext =
          line"$Endpoint_[${traitName}, ${op.renderAlgParams(serviceName + "Gen")}]$errorable"
      )(
        renderId(op.name, op.originalNamespace),
        line"val input: $Schema_[${op.input}] = ${op.input.schemaRef}.addHints(smithy4s.internals.InputOutput.Input.widen)",
        line"val output: $Schema_[${op.output}] = ${op.output.schemaRef}.addHints(smithy4s.internals.InputOutput.Output.widen)",
        renderStreamingSchemaVal("streamedInput", op.streamedInput),
        renderStreamingSchemaVal("streamedOutput", op.streamedOutput),
        renderHintsVal(op.hints),
        line"def wrap(input: ${op.input}) = ${opNameRef}($input)",
        renderErrorable(op)
      ),
      renderedErrorUnion
    )
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

  private def renderProtocol(name: NameRef, hints: List[Hint]): Lines = {
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

  private def renderProductNonMixin(
      product: Product,
      adtParent: Option[NameRef],
      additionalLines: Lines
  ): Lines = {
    import product._
    val decl =
      line"case class ${product.nameDef}(${renderArgs(fields)})"
    val schemaImplicit = if (adtParent.isEmpty) "implicit " else ""

    lines(
      if (hints.contains(Hint.Error)) {
        val mixinExtensions = if (mixins.nonEmpty) {
          val ext = mixins.map(m => line"$m").intercalate(line" with ")
          line" with $ext"
        } else Line.empty
        block(line"$decl extends Throwable$mixinExtensions") {
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
        val extendAdt = adtParent.map(t => line"$t").toList
        val mixinLines = mixins.map(m => line"$m")
        val extend = (extendAdt ++ mixinLines).intercalate(line" with ")
        val ext =
          if (extend.nonEmpty) line" extends $extend"
          else Line.empty
        line"$decl$ext"
      },
      obj(product.nameRef, shapeTag(product.nameRef))(
        renderId(originalName),
        newline,
        renderHintsVal(hints),
        renderProtocol(product.nameRef, hints),
        newline,
        if (fields.nonEmpty) {
          val renderedFields =
            fields.map { case Field(fieldName, realName, tpe, required, hints) =>
              val req = if (required) "required" else "optional"
              if (hints.isEmpty) {
                line"""${tpe.schemaRef}.$req[${product.nameRef}]("$realName", _.$fieldName)"""
              } else {
                val mh = memberHints(hints)
                  // format: off
                  line"""${tpe.schemaRef}${renderConstraintValidation(hints)}.$req[${product.nameRef}]("$realName", _.$fieldName).addHints($mh)"""
                  // format: on
              }
            }
          if (fields.size <= 22) {
            val definition =
              if (recursive) line"$recursive_($struct_" else line"$struct_"
            line"${schemaImplicit}val schema: $Schema_[${product.nameRef}] = $definition"
              .args(renderedFields)
              .block(line"${product.nameRef}.apply")
              .appendToLast(".withId(id).addHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          } else {
            val definition =
              if (recursive) line"$recursive_($struct_.genericArity"
              else line"$struct_.genericArity"
            line"${schemaImplicit}val schema: $Schema_[${product.nameRef}] = $definition"
              .args(renderedFields)
              .block(
                line"arr => new ${product.nameRef}".args(
                  fields.zipWithIndex.map {
                    case (Field(_, _, tpe, required, _), idx) =>
                      val scalaTpe = line"$tpe"
                      val optional =
                        if (required) scalaTpe else Line.optional(scalaTpe)

                      line"arr($idx).asInstanceOf[$optional]"
                  }
                )
              )
              .appendToLast(".withId(id).addHints(hints)")
              .appendToLast(if (recursive) ")" else "")
          }
        } else {
          line"implicit val schema: $Schema_[${product.nameRef}] = $constant_(${product.nameRef}()).withId(id).addHints(hints)"
        },
        additionalLines
      )
    )
  }

  private def renderProductMixin(
      product: Product,
      adtParent: Option[NameRef],
      additionalLines: Lines
  ): Lines = {
    import product._
    val ext = if (mixins.nonEmpty) {
      val mixinExtensions = mixins.map(m => line"$m").intercalate(line" with ")
      line" extends $mixinExtensions"
    } else Line.empty
    block(line"trait $name$ext") {
      lines(
        fields.map(f => line"def ${fieldToRenderLine(f, noDefault = true)}")
      )
    }
  }

  private def renderProduct(
      product: Product,
      adtParent: Option[NameRef] = None,
      additionalLines: Lines = Lines.empty
  ): Lines = {
    import product._
    if (isMixin)
      renderProductMixin(
        product,
        adtParent,
        additionalLines
      )
    else
      renderProductNonMixin(
        product,
        adtParent,
        additionalLines
      )
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
        line"override val errorable: Option[$Errorable_[$errorName]] = Some(this)",
        line"val error: $unionSchema_[$errorName] = $errorName.schema",
        block(
          line"def liftError(throwable: Throwable) : Option[$errorName] = throwable match"
        ) {
          op.errors.collect { case Type.Ref(_, name) =>
            line"case e: ${name} => Some($errorName.${name}Case(e))"
          } ++ List(line"case _ => None")
        },
        block(
          line"def unliftError(e: $errorName) : Throwable = e match"
        ) {
          op.errors.collect { case Type.Ref(_, name) =>
            line"case $errorName.${name}Case(e) => e"
          }
        }
      )
  }

  private def renderUnion(
      name: NameRef,
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

    lines(
      block(
        line"sealed trait ${NameDef(name.name)} extends scala.Product with scala.Serializable"
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
              line"case object ${NameRef(cn)} extends $name",
              line"""private val ${cn}Alt = $Schema_.constant($cn)${renderConstraintValidation(altHints)}.oneOf[$name]("$realName").addHints(hints)""",
              line"private val ${cn}AltWithValue = ${cn}Alt($cn)"
            )
            // format: on
          case a @ Alt(altName, _, UnionMember.TypeCase(tpe), _) =>
            val cn = caseName(a)
            lines(
              line"case class ${NameDef(cn)}(${uncapitalise(altName)}: ${tpe}) extends $name"
            )
          case Alt(_, realName, UnionMember.ProductCase(struct), _) =>
            val additionalLines = lines(
              newline,
              line"""val alt = schema.oneOf[$name]("$realName")"""
            )
            renderProduct(
              struct,
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
            block(line"object ${NameRef(cn)}")(
              renderHintsVal(altHints),
            // format: off
            line"val schema: $Schema_[$cn] = $bijection_(${tpe.schemaRef}.addHints(hints)${renderConstraintValidation(altHints)}, $cn(_), _.${uncapitalise(altName)})",
            line"""val alt = schema.oneOf[$name]("$realName")""",
            // format: on
            )
        },
        newline, {
          val union =
            if (error)
              line"implicit val schema: $unionSchema_[$name] = $union_"
            else if (recursive)
              line"implicit val schema: $Schema_[$name] = $recursive_($union_"
            else
              line"implicit val schema: $Schema_[$name] = $union_"
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
                  line"case $caseName => ${caseName}AltWithValue"
                case (caseName, false) =>
                  line"case c : $caseName => $caseName.alt(c)"
              }
            }
            .appendToLast(
              if (error) "" else ".withId(id).addHints(hints)"
            )
            .appendToLast(if (recursive) ")" else "")
        }
      )
    )
  }

  private def fieldToRenderLine(
      field: Field,
      noDefault: Boolean = false
  ): Line = {
    field match {
      case Field(name, _, tpe, required, hints) =>
        val line = line"$tpe"
        val tpeAndDefault = if (required) {
          val maybeDefault = hints
            .collectFirst { case d @ Hint.Default(_) => d }
            .map(renderDefault)
          Line.required(line, maybeDefault)
        } else {
          Line.optional(line, !noDefault)
        }

        line"$name: " + tpeAndDefault
    }
  }
  private def renderArgs(fields: List[Field]): Line = fields
    .map(fieldToRenderLine(_))
    .intercalate(Line.comma)

  private def renderEnum(
      name: NameRef,
      originalName: String,
      values: List[EnumValue],
      hints: List[Hint]
  ): Lines = lines(
    block(
      line"sealed abstract class ${name.name}(_value: String, _name: String, _ordinal: Int) extends $Enumeration_.Value"
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
        line"""case object ${NameRef(
          e.name
        )} extends $name("$value", "${e.name}", $ordinal)"""
      },
      newline,
      line"val values: List[$name] = List".args(
        values.map(_.name)
      ),
      line"implicit val schema: $Schema_[$name] = $enumeration_(values).withId(id).addHints(hints)"
    )
  )

  private def renderTypeAlias(
      name: NameRef,
      originalName: String,
      tpe: Type,
      hints: List[Hint]
  ): Lines = {
    val trailingCalls =
      line".withId(id).addHints(hints)${renderConstraintValidation(hints)}"
    lines(
      obj(name, line"$Newtype_[$tpe]")(
        renderId(originalName),
        renderHintsVal(hints),
        line"val underlyingSchema : $Schema_[$tpe] = ${tpe.schemaRef}$trailingCalls",
        lines(
          line"implicit val schema : $Schema_[$name] = $bijection_(underlyingSchema, asBijection)"
        )
      )
    )
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

    def renderAlgParams(serviceName: String) = {
      line"${op.input}, ${if (op.errors.isEmpty) line"Nothing"
      else NameRef(s"$serviceName.${op.name}Error")}, ${op.output}, ${op.streamedInput
        .map(_.tpe)
        .getOrElse(Type.PrimitiveType(Nothing))}, ${op.streamedOutput
        .map(_.tpe)
        .getOrElse(Type.PrimitiveType(Nothing))}"
    }
  }

  implicit class TypeRefExt(tpe: Type.Ref) {
    def renderFull: String = s"${tpe.namespace}.${tpe.name}"
  }

  implicit class TypeExt(tpe: Type) {
    val schemaPkg_ = "smithy4s.schema.Schema"
    def schemaRef: Line = tpe match {
      case Type.PrimitiveType(p) => NameRef(schemaRefP(p)).toLine
      case Type.Collection(collectionType, member) =>
        val col = collectionType match {
          case CollectionType.List       => s"$schemaPkg_.list"
          case CollectionType.Set        => s"$schemaPkg_.set"
          case CollectionType.Vector     => s"$schemaPkg_.vector"
          case CollectionType.IndexedSeq => s"$schemaPkg_.indexedSeq"
        }
        line"${NameRef(col)}(${member.schemaRef})"
      case Type.Map(key, value) =>
        line"${NameRef(s"$schemaPkg_.map")}(${key.schemaRef}, ${value.schemaRef})"
      case Type.Alias(
            ns,
            name,
            _,
            false
          ) =>
        NameRef(ns, s"$name.schema").toLine
      case Type.Alias(ns, name, _, _) =>
        NameRef(ns, s"$name.underlyingSchema").toLine
      case Type.Ref(ns, name) => NameRef(ns, s"$name.schema").toLine
      case Type.ExternalType(
            _,
            fqn,
            maybeProviderImport,
            underlyingTpe,
            hint
          ) =>
        line"${underlyingTpe.schemaRef}.refined[$fqn](${renderNativeHint(hint)})${maybeProviderImport
          .map { providerImport => Import(providerImport).toLine }
          .getOrElse(Line.empty)}"
    }

    private def schemaRefP(primitive: Primitive): String = primitive match {
      case Primitive.Unit       => s"${schemaPkg_}.unit"
      case Primitive.ByteArray  => s"${schemaPkg_}.bytes"
      case Primitive.Bool       => s"${schemaPkg_}.boolean"
      case Primitive.String     => s"${schemaPkg_}.string"
      case Primitive.Timestamp  => s"${schemaPkg_}.timestamp"
      case Primitive.Byte       => s"${schemaPkg_}.byte"
      case Primitive.Int        => s"${schemaPkg_}.int"
      case Primitive.Short      => s"${schemaPkg_}.short"
      case Primitive.Long       => s"${schemaPkg_}.long"
      case Primitive.Float      => s"${schemaPkg_}.float"
      case Primitive.Double     => s"${schemaPkg_}.double"
      case Primitive.BigDecimal => s"${schemaPkg_}.bigdecimal"
      case Primitive.BigInteger => s"${schemaPkg_}.bigint"
      case Primitive.Uuid       => s"${schemaPkg_}.uuid"
      case Primitive.Document   => s"${schemaPkg_}.document"
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

  private def renderDefault(hint: Hint.Default): Line =
    Line(
      smithy4s.recursion
        .cata(renderTypedNode)(hint.typedNode)
        .run(true)
        ._2
    )

  private def renderHint(hint: Hint): Option[Line] = hint match {
    case h: Hint.Native => renderNativeHint(h).some
    case Hint.IntEnum   => line"${NameRef("smithy4s", "IntEnum")}()".some
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

  private def shapeTag(name: NameRef): Line =
    line"$ShapeTag_.Companion[$name]"

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

}
