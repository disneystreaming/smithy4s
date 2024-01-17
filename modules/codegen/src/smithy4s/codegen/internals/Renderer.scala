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

package smithy4s.codegen
package internals

import cats.data.NonEmptyList
import cats.data.Reader
import cats.syntax.all._
import smithy4s.codegen.internals.EnumTag.IntEnum
import smithy4s.codegen.internals.EnumTag.StringEnum
import smithy4s.codegen.internals.LineSegment._
import smithy4s.codegen.internals.Primitive.Nothing
import smithy4s.codegen.internals.Type.Nullable
import smithy4s.codegen.internals.TypedNode._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node._
import software.amazon.smithy.model.shapes.ShapeId

import scala.jdk.CollectionConverters._

import Line._
import LineSyntax.LineInterpolator
import ToLines.lineToLines

private[internals] object Renderer {

  case class Result(namespace: String, name: String, content: String)

  case class Config(
      errorsAsScala3Unions: Boolean,
      wildcardArgument: String,
      renderOptics: Boolean
  )
  object Config {
    def load(metadata: Map[String, Node]): Renderer.Config = {
      val errorsAsScala3Unions = metadata
        .get("smithy4sErrorsAsScala3Unions")
        .flatMap(_.asBooleanNode().asScala)
        .map(_.getValue())
        .getOrElse(false)
      val wildcardArgument = metadata
        .get("smithy4sWildcardArgument")
        .flatMap(_.asStringNode().asScala)
        .map(_.getValue())
        .getOrElse("_")

      val renderOptics = metadata
        .get("smithy4sRenderOptics")
        .flatMap(_.asBooleanNode().asScala)
        .map(_.getValue())
        .getOrElse(false)

      if (wildcardArgument != "?" && wildcardArgument != "_") {
        throw new IllegalArgumentException(
          s"`smithy4sWildcardArgument` possible values are: `?` or `_`. found `$wildcardArgument`."
        )
      }

      Renderer.Config(
        errorsAsScala3Unions = errorsAsScala3Unions,
        wildcardArgument = wildcardArgument,
        renderOptics = renderOptics
      )
    }
  }

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
      val renderResult = r.renderDecl(decl) ++ newline
      val p = s"package ${unit.namespace}"

      val segments = renderResult.list.flatMap(_.segments.toList)
      val localCollisions: Set[String] = segments
        .groupBy {
          // we need to compare NameRefs as they would be imported in order to avoid unnecessarily qualifying types in the same package
          case ref: NameRef => ref.asImport
          case other        => other
        }
        .map(_._2.head)
        .collect {
          // Here we collect the NameRefs using the start of the Name so Age.schema and Age would be considered a collision if they refer to different types
          case ref: NameRef  => ref.getNamePrefix
          case NameDef(name) => name
        }
        .groupBy(identity)
        .filter(_._2.size > 1)
        .keySet

      val otherDecls =
        unit.declarations.filterNot(_ == decl).map(_.name).toSet

      def differentPackage(ref: NameRef): Boolean =
        ref.pkg.mkString(".") != unit.namespace

      // Collisions between references in the current file and declarations
      // in the current package
      val namespaceCollisions = segments.collect {
        case ref: NameRef if otherDecls(ref.name) && differentPackage(ref) =>
          ref.name
      }.toSet

      val nameCollisions = localCollisions ++ namespaceCollisions

      val allImports: List[String] = renderResult.list.flatMap { line =>
        line.segments.toList.collect {
          case nameRef @ NameRef(pkg, _, _)
              if pkg.nonEmpty && !nameCollisions.contains(
                nameRef.getNamePrefix
              )
                && !nameRef.isAutoImported &&
                !pkg.mkString(".").equalsIgnoreCase(unit.namespace) =>
            nameRef.show
          case Import(value) => value
        }
      }

      val code: List[String] = renderResult.list
        .map { line =>
          line.segments.toList.collect {
            case Literal(value) => value
            case NameDef(name)  => name
            case nameRef: NameRef =>
              if (nameCollisions.contains(nameRef.getNamePrefix))
                nameRef.asValue
              else nameRef.name
          }.mkString
        }

      val allLines: List[String] = List(p, "") ++
        allImports.distinct.sorted.map("import " + _) ++
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

private[internals] class Renderer(compilationUnit: CompilationUnit) { self =>

  val names = new CollisionAvoidance.Names()
  import compilationUnit.namespace
  import compilationUnit.rendererConfig.wildcardArgument
  import names._

  def renderDecl(decl: Decl): Lines = decl match {
    case Service(shapeId, name, ops, hints, version) =>
      renderService(shapeId, name, ops, hints, version)
    case p: Product => renderProduct(p)
    case union @ Union(shapeId, _, alts, mixins, recursive, hints) =>
      renderUnion(shapeId, union.nameRef, alts, mixins, recursive, hints)
    case ta @ TypeAlias(shapeId, _, tpe, _, recursive, hints) =>
      renderNewtype(shapeId, ta.nameRef, tpe, recursive, hints)
    case enumeration @ Enumeration(shapeId, _, tag, values, hints) =>
      renderEnum(shapeId, enumeration.nameRef, tag, values, hints)
  }

  private def deprecationAnnotation(hints: List[Hint]): Line = {
    hints
      .collectFirst { case h: Hint.Deprecated => h }
      .foldMap { dep =>
        val messagePart =
          line"message = ${renderStringLiteral(dep.message.getOrElse("N/A"))}"

        val versionPart =
          line"since = ${renderStringLiteral(dep.since.getOrElse("N/A"))}"

        val args = List(messagePart, versionPart).intercalate(comma)

        val argListOrEmpty = if (args.nonEmpty) line"($args)" else line""

        line"@deprecated$argListOrEmpty"
      }
  }

  /**
   * Returns the given list of Smithy documentation strings formatted as Scaladoc comments.
   *
   * @return formatted list of scaladoc lines
   */
  private def makeDocLines(
      rawLines: List[String]
  ): Lines = {
    lines(
      rawLines
        .mkString_("/** ", "\n  * ", "\n  */")
        .linesIterator
        .toList
    )
  }

  private def documentationAnnotation(
      hints: List[Hint],
      skipMemberDocs: Boolean = false
  ): Lines = {
    val atLiteral: String => String = _.replace("@", "{@literal @}")
    val dollarLiteral: String => String = _.replace("$", "`$`")
    val slashStarLiteral: String => String = _.replace("/*", "/&ast;")

    val literalReplacements: String => String =
      atLiteral.andThen(dollarLiteral).andThen(slashStarLiteral)

    hints
      .collectFirst { case h: Hint.Documentation => h }
      .foldMap { doc =>
        val shapeDocs: List[String] =
          doc.docLines
            .map(literalReplacements)
        val memberDocs: List[String] =
          if (skipMemberDocs) List.empty
          else
            doc.memberDocLines.flatMap { case (memberName, text) =>
              s"@param $memberName" :: text
                .map(literalReplacements)
                .map("  " + _)
            }.toList

        val maybeNewline =
          if (shapeDocs.nonEmpty && memberDocs.nonEmpty) List("", "") else Nil
        val allDocs = shapeDocs ++ maybeNewline ++ memberDocs
        if (allDocs.size == 1) lines("/** " + allDocs.head + " */")
        else makeDocLines(shapeDocs ++ memberDocs)
      }
  }

  def renderPackageContents: Lines = {
    val typeAliases = compilationUnit.declarations.collect {
      case TypeAlias(_, name, _, _, _, hints) =>
        lines(
          documentationAnnotation(hints),
          deprecationAnnotation(hints),
          line"type $name = ${compilationUnit.namespace}.${name}.Type"
        )
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
      val nameGen = NameRef(s"${name}Gen")
      lines(
        deprecationAnnotation(s.hints),
        line"type ${NameDef(name)}[F[_]] = $FunctorAlgebra_[$nameGen, F]",
        line"val ${NameRef(name)} = $nameGen"
      )
    case _ => Lines.empty
  }

  private def renderService(
      shapeId: ShapeId,
      name: String,
      ops: List[Operation],
      hints: List[Hint],
      version: String
  ): Lines = {

    val genName: NameDef = NameDef(name + "Gen")
    val genNameRef: NameRef = genName.toNameRef
    val genNameProduct: NameDef = NameDef(name + "ProductGen")
    val genNameProductRef: NameRef = genNameProduct.toNameRef
    val opTraitName = NameDef(name + "Operation")
    val opTraitNameRef = opTraitName.toNameRef
    val generateServiceProduct = hints.contains(Hint.GenerateServiceProduct)

    lines(
      documentationAnnotation(hints),
      deprecationAnnotation(hints),
      block(line"trait $genName[F[_, _, _, _, _]]")(
        line"self =>",
        newline,
        ops.map { op =>
          lines(
            documentationAnnotation(
              op.hints,
              op.hints.contains(Hint.PackedInputs)
            ),
            deprecationAnnotation(op.hints),
            line"def ${op.methodName}(${op.renderArgs}): F[${op
              .renderAlgParams(opTraitNameRef.name)}]"
          )
        },
        newline,
        line"def $transform_: $Transformation.PartiallyApplied[$genName[F]] = $Transformation.of[$genName[F]](this)"
      ),
      newline,
      lines(
        block(line"trait $genNameProduct[F[_, _, _, _, _]]")(
          line"self =>",
          newline,
          ops.map { op =>
            lines(
              deprecationAnnotation(op.hints),
              line"def ${op.methodName}: F[${op
                .renderAlgParams(opTraitNameRef.name)}]"
            )
          }
        ),
        newline
      ).when(generateServiceProduct),
      obj(
        genNameRef,
        ext = line"$Service_.Mixin[$genNameRef, $opTraitNameRef]",
        w = line"${ServiceProductMirror}[${genNameRef}]".when(
          generateServiceProduct
        )
      )(
        newline,
        renderId(shapeId),
        line"""val version: $string_ = "$version"""",
        newline,
        renderHintsVal(hints),
        newline,
        line"def $apply_[F[_]](implicit F: $Impl_[F]): F.type = F",
        newline,
        block(line"object $ErrorAware_")(
          line"def $apply_[F[_, _]](implicit F: $ErrorAware_[F]): F.type = F",
          line"type $Default_[F[+_, +_]] = $Constant_[smithy4s.kinds.stubs.Kind2[F]#toKind5]"
        ),
        newline,
        line"val endpoints: $vector[smithy4s.Endpoint[$opTraitName, $wildcardArgument, $wildcardArgument, $wildcardArgument, $wildcardArgument, $wildcardArgument]] = $vector"
          .args(ops.map(op => line"${opTraitNameRef}.${op.name}")),
        newline,
        line"def $input_[I, E, O, SI, SO](op: $opTraitNameRef[I, E, O, SI, SO]): I = op.$input_",
        line"def $ordinal_[I, E, O, SI, SO](op: $opTraitNameRef[I, E, O, SI, SO]): Int = op.$ordinal_",
        line"override def $endpoint_[I, E, O, SI, SO](op: $opTraitNameRef[I, E, O, SI, SO]) = op.$endpoint_",
        line"class $Constant_[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ${opTraitNameRef}.$Transformed_[$opTraitNameRef, P](reified, $const5_(value))",
        line"type $Default_[F[+_]] = $Constant_[smithy4s.kinds.stubs.Kind1[F]#toKind5]",
        line"def reified: $genNameRef[$opTraitNameRef] = ${opTraitNameRef}.${NameRef("reified")}",
        line"def $mapK5_[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: $genNameRef[P], f: $PolyFunction5_[P, P1]): $genNameRef[P1] = new $opTraitNameRef.$Transformed_(alg, f)",
        line"def $fromPolyFunction_[P[_, _, _, _, _]](f: $PolyFunction5_[$opTraitNameRef, P]): $genNameRef[P] = new $opTraitNameRef.$Transformed_(reified, f)",
        line"def $toPolyFunction_[P[_, _, _, _, _]](impl: $genNameRef[P]): $PolyFunction5_[$opTraitNameRef, P] = $opTraitNameRef.$toPolyFunction_(impl)",
        newline,
        ops.map { op =>
          if (op.errors.isEmpty) Lines.empty
          else {
            val errorName = NameRef(op.name + "Error")
            lines(
              line"type $errorName = $opTraitNameRef.$errorName",
              line"val $errorName = $opTraitNameRef.$errorName"
            )
          }
        },
        lines(
          line"type Prod[F[_, _, _, _, _]] = ${genNameProduct}[F]",
          line"val serviceProduct: ${ServiceProduct}.Aux[${genNameProduct}, ${genName}] = ${genNameProduct}"
        ).when(generateServiceProduct)
      ),
      newline,
      lines(
        obj(
          genNameProductRef,
          ext = line"$ServiceProduct[$genNameProductRef]"
        )(
          line"type Alg[F[_, _, _, _, _]] = ${genNameRef}[F]",
          line"val service: $genName.type = $genName",
          newline,
          block(
            line"def endpointsProduct: ${genNameProductRef}[service.Endpoint] = new ${genNameProductRef}[service.Endpoint]"
          )(
            ops.map { op =>
              line"def ${op.methodName}: service.Endpoint[${op
                .renderAlgParams(opTraitNameRef.name)}] = ${opTraitNameRef}.${op.name}"
            }
          ),
          newline,
          block(
            line"def $toPolyFunction_[P2[_, _, _, _, _]](algebra: ${genNameProductRef}[P2]) = new $PolyFunction5_[service.Endpoint, P2]"
          )(
            line"def $apply_[I, E, O, SI, SO](fa: service.Endpoint[I, E, O, SI, SO]): P2[I, E, O, SI, SO] =",
            if (ops.isEmpty) line"""sys.error("impossible")"""
            else
              block(line"fa match")(
                ops.map { op =>
                  // This normally compiles, but Scala 3 seems to have an issue with
                  // it so we have to cast it.
                  line"case ${opTraitNameRef}.${op.name} => algebra.${op.methodName}.asInstanceOf[P2[I, E, O, SI, SO]]"
                }
              )
          ),
          newline,
          block(
            line"def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: ${genNameProductRef}[F], f: $PolyFunction5_[F, G]): ${genNameProductRef}[G] ="
          )(
            block(
              line"new ${genNameProductRef}[G]"
            )(
              ops.map { op =>
                val argsTypes = op.renderAlgParams(opTraitNameRef.name)
                line"def ${op.methodName}: G[${argsTypes}] = f[${argsTypes}](alg.${op.methodName})"
              }
            )
          )
        ),
        newline
      ).when(generateServiceProduct),
      block(
        line"sealed trait $opTraitName[Input, Err, Output, StreamedInput, StreamedOutput]"
      )(
        line"def run[F[_, _, _, _, _]](impl: $genName[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]",
        line"def ordinal: Int",
        line"def input: Input",
        line"def endpoint: $Endpoint_[$opTraitName, Input, Err, Output, StreamedInput, StreamedOutput]"
      ),
      newline,
      block(
        line"object $opTraitName"
      )(
        newline,
        block(
          line"object ${NameDef("reified")} extends $genNameRef[$opTraitNameRef]"
        ) {
          ops.map {
            case op if op.input == Type.unit =>
              line"def ${op.methodName}(${op.renderArgs}): ${op.name} = ${op.name}()"
            case op if op.hints.contains(Hint.PackedInputs) =>
              line"def ${op.methodName}(${op.renderArgs}): ${op.name} = ${op.name}(input)"
            case op =>
              line"def ${op.methodName}(${op.renderArgs}): ${op.name} = ${op.name}(${op.input}(${op.renderParams}))"
          }
        },
        block(
          line"class $Transformed_[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: $genNameRef[P], f: $PolyFunction5_[P, P1]) extends $genNameRef[P1]"
        ) {
          ops.map { op =>
            val opName = op.methodName
            line"def $opName(${op.renderArgs}): P1[${op
              .renderAlgParams(opTraitNameRef.name)}] = f[${op
              .renderAlgParams(opTraitNameRef.name)}](alg.$opName(${op.renderParams}))"
          }
        },
        newline,
        block(
          line"def $toPolyFunction_[P[_, _, _, _, _]](impl: $genNameRef[P]): $PolyFunction5_[$opTraitNameRef, P] = new $PolyFunction5_[$opTraitNameRef, P]"
        )(
          if (ops.isEmpty) {
            line"""def $apply_[I, E, O, SI, SO](op: $opTraitNameRef[I, E, O, SI, SO]): P[I, E, O, SI, SO] = sys.error("impossible")"""
          } else {
            line"def $apply_[I, E, O, SI, SO](op: $opTraitNameRef[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) "
          }
        ),
        ops.zipWithIndex.map { case (op, ordinal) =>
          renderOperation(name, op, ordinal)
        }
      ),
      newline
    )
  }

  // scalafmt: { maxColumn = 120}
  private def renderOperation(
      serviceName: String,
      op: Operation,
      ordinal: Int
  ): Lines = {
    val params = if (op.input != Type.unit) {
      line"input: ${op.input}"
    } else Line.empty
    val genServiceName = serviceName + "Gen"
    val opObjectName = serviceName + "Operation"
    val opName = op.name
    val opNameRef = NameRef(opName)
    val opErrorDef = NameDef(opName + "Error")
    val traitName = NameRef(s"${serviceName}Operation")
    val input =
      if (op.input == Type.unit) "" else "input"

    val errorUnion: Option[Union] = for {
      errorNel <- NonEmptyList.fromList(op.errors)
      alts <- errorNel.traverse { t =>
        t.name.map(n => Alt(n, UnionMember.TypeCase(t)))
      }
      name = opName + "Error"
    } yield Union(
      ShapeId.fromParts(namespace, op.shapeId.getName() + "Error"),
      name,
      alts,
      List.empty
    )

    val renderedErrorUnion = errorUnion.foldMap { case union @ Union(shapeId, _, alts, mixin, recursive, hints) =>
      if (compilationUnit.rendererConfig.errorsAsScala3Unions)
        renderErrorAsScala3Union(
          shapeId,
          union.nameRef,
          alts,
          recursive,
          hints
        )
      else
        renderUnion(
          shapeId,
          union.nameRef,
          alts,
          mixin,
          recursive,
          hints,
          error = true
        )
    }
    val ns = op.shapeId.getNamespace()

    lines(
      block(
        line"final case class ${NameDef(opName)}($params) extends $traitName[${op.renderAlgParams(opObjectName)}]"
      )(
        line"def run[F[_, _, _, _, _]](impl: $genServiceName[F]): F[${op
          .renderAlgParams(opObjectName)}] = impl.${op.methodName}(${op.renderAccessedParams})",
        line"def ordinal: Int = $ordinal",
        if (op.input == Type.unit) line"def input: Unit = ()" else Lines.empty,
        line"def endpoint: smithy4s.Endpoint[$traitName,${op
          .renderAlgParams(opObjectName)}] = $opNameRef"
      ),
      obj(
        opNameRef,
        ext = line"smithy4s.Endpoint[$traitName,${op.renderAlgParams(opObjectName)}]"
      )(
        line"""val schema: $OperationSchema_[${op.renderAlgParams(
          opObjectName
        )}] = $Schema_.operation($ShapeId_("$ns", "$opName"))""",
        indent(
          line".withInput(${op.input.schemaRef})",
          Option(op.errors).filter(_.nonEmpty).as(line".withError(${opErrorDef}.errorSchema)"),
          line".withOutput(${op.output.schemaRef})",
          op.streamedInput.map(si => line".withStreamedInput(${renderStreamingSchema(si)})"),
          op.streamedOutput.map(si => line".withStreamedOutput(${renderStreamingSchema(si)})"),
          Option(op.hints).filter(_.nonEmpty).map(h => line".withHints(${memberHints(h)})")
        ),
        line"def wrap(input: ${op.input}): $opNameRef = ${opNameRef}($input)"
      ),
      renderedErrorUnion
    )
  }

  private def renderStreamingSchema(
      sField: StreamingField
  ): Line = {
    import sField._
    val mh =
      if (hints.isEmpty) Line.empty
      else line".addHints(${memberHints(hints)})"
    line"""$StreamingSchema_("$name", ${tpe.schemaRef}$mh)"""
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
          line"def traits: $set[$ShapeId_] = $set($protocolTraits)"
        }
      )
    }
  }

  private def renderTypeclass(hint: Hint.Typeclass, tpe: NameRef): Line = {
    val target = NameRef(hint.targetType)
    val interpreter = NameRef(hint.interpreter)
    val lowerCasedName = uncapitalise(tpe.name)
    line"implicit val $lowerCasedName${hint.id.getName.capitalize}: $target[$tpe] = $interpreter.fromSchema(schema)"
  }

  private def renderTypeclasses(
      hints: List[Hint],
      tpe: NameRef
  ): Lines = {
    val result = hints.collect { case h: Hint.Typeclass =>
      renderTypeclass(h, tpe)
    }
    if (result.isEmpty) Lines.empty else newline ++ Lines(result)
  }

  private def renderLenses(product: Product, hints: List[Hint]): Lines = if (
    (compilationUnit.rendererConfig.renderOptics || hints.contains(
      Hint.GenerateOptics
    )) && product.fields.nonEmpty
  ) {
    val smithyLens = NameRef("smithy4s.optics.Lens")
    val lenses = product.fields.map { field =>
      val fieldType =
        if (field.modifier.none) Line.optional(line"${field.tpe}")
        else line"${field.tpe}"
      line"val ${field.name}: $smithyLens[${product.nameRef}, $fieldType] = $smithyLens[${product.nameRef}, $fieldType](_.${field.name})(n => a => a.copy(${field.name} = n))"
    }
    obj(product.nameRef.copy(name = "optics"))(lenses) ++
      newline
  } else Lines.empty

  private def renderProductNonMixin(
      product: Product,
      adtParent: Option[NameRef],
      additionalLines: Lines,
      classBody: Lines
  ): Lines = {
    import product._
    val renderedArgs = renderArgs(fields)
    val decl =
      line"final case class ${product.nameDef}($renderedArgs)"
    val schemaImplicit = if (adtParent.isEmpty) "implicit " else ""

    lines(
      if (hints.contains(Hint.Error)) {
        val exception =
          if (hints.contains(Hint.NoStackTrace))
            noStackTrace
          else smithy4sThrowable
        val mixinExtensions = if (mixins.nonEmpty) {
          val ext = mixins.map(m => line"$m").intercalate(line" with ")
          line" with $ext"
        } else Line.empty
        block(line"$decl extends $exception$mixinExtensions") {
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

        if (classBody.isEmpty) line"$decl$ext"
        else
          block(line"$decl$ext") {
            classBody
          }
      },
      newline,
      obj(product.nameRef, shapeTag(product.nameRef))(
        renderId(shapeId),
        newline,
        renderHintsVal(hints),
        renderProtocol(product.nameRef, hints),
        newline,
        renderLenses(product, hints),
        if (fields.nonEmpty) {
          val renderedFields =
            fields.map { case Field(fieldName, realName, tpe, modifier, hints) =>
              val fieldBuilder = modifier match {
                case Field.Modifier.RequiredMod              => "required"
                case Field.Modifier.RequiredDefaultMod(_, _) => "required"
                case Field.Modifier.DefaultMod(_, _)         => "field"
                case Field.Modifier.NoModifier               => "optional"
              }

              if (hints.isEmpty) {
                line"""${tpe.schemaRef}.$fieldBuilder[${product.nameRef}]("$realName", _.$fieldName)"""
              } else {
                val memHints = memberHints(hints)
                val addMemHints =
                  if (memHints.nonEmpty) line".addHints($memHints)"
                  else Line.empty
                // format: off
                line"""${tpe.schemaRef}${renderConstraintValidation(hints)}.$fieldBuilder[${product.nameRef}]("$realName", _.$fieldName)$addMemHints"""
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
                  fields.zipWithIndex.map { case (Field(_, _, tpe, mod, _), idx) =>
                    val scalaTpe = line"$tpe"
                    val optional = if (mod.none) Line.optional(scalaTpe) else scalaTpe

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
        renderTypeclasses(product.hints, product.nameRef),
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
        fields.map { f =>
          lines(
            deprecationAnnotation(f.hints),
            Line.empty,
            line"def ${fieldToRenderLine(f, noDefault = true)}"
          )
        }
      )
    }
  }

  private def renderProduct(
      product: Product,
      adtParent: Option[NameRef] = None,
      additionalLines: Lines = Lines.empty,
      classBody: Lines = Lines.empty
  ): Lines = {
    import product._
    val base =
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
          additionalLines,
          classBody
        )

    lines(
      documentationAnnotation(product.hints),
      deprecationAnnotation(product.hints),
      base
    )
  }

  private def renderGetMessage(field: Field) = {
    val hasModifier = !field.modifier.none
    field match {
      case field if field.tpe.isResolved && hasModifier =>
        line"override def getMessage(): $string_ = ${field.name}"
      case field if field.tpe.isResolved =>
        line"override def getMessage(): $string_ = ${field.name}.orNull"
      case field if hasModifier =>
        line"override def getMessage(): $string_ = ${field.name}.value"
      case field =>
        line"override def getMessage(): $string_ = ${field.name}.map(_.value).orNull"
    }
  }
  private def renderErrorSchemaMethods(errorName: NameRef, errors: List[Type]): Lines = {
    val scala3Unions = compilationUnit.rendererConfig.errorsAsScala3Unions
    lines(
      block(
        line"def liftError(throwable: Throwable): $option[$errorName] = throwable match"
      ) {
        if (scala3Unions) {
          List(
            line"case e: $errorName => $some(e)",
            line"case _ => $none"
          )
        } else {
          errors.collect { case Type.Ref(pkg, name) =>
            line"case e: ${NameRef(pkg + "." + name)} => $some($errorName.${name}Case(e))"
          } ++ List(line"case _ => $none")
        }
      },
      if (scala3Unions) line"def unliftError(e: $errorName): Throwable = e"
      else
        block(
          line"def unliftError(e: $errorName): Throwable = e match"
        ) {
          errors.collect { case Type.Ref(_, name) =>
            line"case $errorName.${name}Case(e) => e"
          }
        }
    )
  }

  private def renderErrorAsScala3Union(
      shapeId: ShapeId,
      name: NameRef,
      alts: NonEmptyList[Alt],
      recursive: Boolean,
      hints: List[Hint]
  ) = {
    // Only Alts with UnionMember.TypeCase are valid for errors
    val members = alts.collect { case Alt(altName, _, UnionMember.TypeCase(tpe), _) =>
      altName -> tpe
    }
    def altVal(altName: String) = line"${uncapitalise(altName)}Alt"
    lines(
      documentationAnnotation(hints),
      deprecationAnnotation(hints),
      line"type ${NameDef(name.name)} = ${members.map { case (_, tpe) => line"$tpe" }.intercalate(line" | ")}",
      obj(name, line"$ErrorSchema_.Companion[${NameRef(name.name)}]")(
        renderId(shapeId),
        newline,
        renderHintsVal(hints),
        newline,
        block(
          line"val schema: $Schema_[$name] ="
        )(
          members.map { case (altName, tpe) =>
            line"""val ${altVal(
              altName
            )} = $tpe.schema.oneOf[${name}]("$altName")"""
          },
          block(
            line"$union_(${members.map { case (n, _) => altVal(n) }.intercalate(line", ")})"
          )(
            members.zipWithIndex.map { case ((altName, _), index) =>
              line"case _: $altName => $index"
            }
          )
        ),
        renderErrorSchemaMethods(NameRef(name.name), members.map(_._2))
      )
    )
  }

  private def caseName(unionName: NameRef, alt: Alt): NameRef =
    alt.member match {
      case UnionMember.ProductCase(product) =>
        unionName.down(product.name)
      case UnionMember.TypeCase(_) | UnionMember.UnitCase =>
        unionName.down(
          alt.name.dropWhile(_ == '_').capitalize + "Case"
        )
    }

  private def caseNameForMatch(unionName: NameRef, alt: Alt): NameRef =
    alt.member match {
      case UnionMember.ProductCase(product) =>
        unionName.down(product.name)
      case UnionMember.TypeCase(_) =>
        unionName.down(alt.name.dropWhile(_ == '_').capitalize + "Case")
      case UnionMember.UnitCase =>
        unionName.down(alt.name.dropWhile(_ == '_').capitalize + "Case.type")
    }

  private def caseNameType(unionName: NameRef, alt: Alt): Line =
    alt.member match {
      case UnionMember.ProductCase(product) =>
        line"${unionName.down(product.name)}"
      case UnionMember.TypeCase(tpe) => line"$tpe"
      case UnionMember.UnitCase =>
        line"${unionName.down(alt.name.dropWhile(_ == '_').capitalize + "Case.type")}"
    }

  private def caseNameWithAlt(unionName: NameRef, alt: Alt): Line =
    alt.member match {
      case UnionMember.ProductCase(product) =>
        line"${unionName.down(product.name)}.alt"
      case UnionMember.TypeCase(_) =>
        val n = unionName.down(alt.name.dropWhile(_ == '_').capitalize + "Case")
        line"$n.alt"
      case UnionMember.UnitCase =>
        val n =
          unionName.down(alt.name.dropWhile(_ == '_').capitalize + "CaseAlt")
        line"$n"
    }

  private def renderPrisms(
      unionName: NameRef,
      alts: NonEmptyList[Alt],
      hints: List[Hint]
  ): Lines = if (
    compilationUnit.rendererConfig.renderOptics || hints.contains(
      Hint.GenerateOptics
    )
  ) {
    val smithyPrism = NameRef("smithy4s.optics.Prism")
    val altLines = alts.map { alt =>
      alt.member match {
        case UnionMember.ProductCase(p) =>
          val (mat, tpe) = (p.nameDef, line"${p.name}")
          line"val ${alt.name}: $smithyPrism[$unionName, $tpe] = $smithyPrism.partial[$unionName, $tpe]{ case t: $mat => t }(identity)"
        case UnionMember.TypeCase(t) =>
          val (mat, tpe) = (caseName(unionName, alt), line"$t")
          line"val ${alt.name}: $smithyPrism[$unionName, $tpe] = $smithyPrism.partial[$unionName, $tpe]{ case $mat(t) => t }($mat.apply)"
        case UnionMember.UnitCase =>
          val (mat, tpe) =
            (caseName(unionName, alt), line"${caseName(unionName, alt)}")
          line"val ${alt.name}: $smithyPrism[$unionName, $tpe.type] = $smithyPrism.partial[$unionName, $tpe.type]{ case t: $mat.type => t }(identity)"
      }
    }

    obj(unionName.copy(name = "optics"))(altLines) ++
      newline
  } else Lines.empty

  private def renderPrismsEnum(
      enumName: NameRef,
      values: List[EnumValue],
      hints: List[Hint],
      isOpen: Boolean
  ): Lines = if (
    compilationUnit.rendererConfig.renderOptics || hints.contains(
      Hint.GenerateOptics
    )
  ) {
    val smithyPrism = NameRef("smithy4s.optics.Prism")
    val openLine =
      if (isOpen)
        List(
          line"val $$unknown: $smithyPrism[$enumName, $enumName.$$Unknown] = $smithyPrism.partial[$enumName, $enumName.$$Unknown]{ case u: $enumName.$$Unknown => u }(identity)"
        )
      else List.empty
    val valueLines = values.map { value =>
      val (mat, tpe) = (value.name, line"${value.name}")
      line"val ${value.name}: $smithyPrism[$enumName, $enumName.$tpe.type] = $smithyPrism.partial[$enumName, $enumName.$tpe.type]{ case $enumName.$mat => $enumName.$mat }(identity)"
    }

    obj(enumName.copy(name = "optics"))(valueLines ++ openLine) ++
      newline
  } else Lines.empty

  private def renderUnion(
      shapeId: ShapeId,
      name: NameRef,
      alts: NonEmptyList[Alt],
      mixins: List[Type],
      recursive: Boolean,
      hints: List[Hint],
      error: Boolean = false
  ): Lines = {
    def smartConstructor(alt: Alt): Lines = {
      val cn = caseName(name, alt).name
      val ident = NameDef(uncapitalise(alt.name))
      val prefix = line"def $ident"
      val constructor = alt.member match {
        case UnionMember.ProductCase(product) =>
          val args = renderArgs(product.fields)
          val values = product.fields.map(_.name).intercalate(", ")

          line"$prefix($args): ${product.nameRef} = ${product.nameRef}($values)"

        case UnionMember.UnitCase =>
          line"$prefix(): $name = ${caseName(name, alt)}"

        case UnionMember.TypeCase(tpe) =>
          line"$prefix($ident: $tpe): $name = $cn($ident)"
      }

      lines(
        documentationAnnotation(alt.hints),
        deprecationAnnotation(alt.hints),
        constructor
      )
    }
    val caseNames = alts.map(caseName(name, _))
    val caseNamesAndIsUnit =
      caseNames.zip(alts.map(_.member == UnionMember.UnitCase))
    val types = alts.map(_.member).collect { case UnionMember.TypeCase(tpe) =>
      tpe
    }

    val companionTpe = if (error) line"$ErrorSchema_.Companion[$name]" else line"$ShapeTag_.Companion[$name]"

    val mixinLines = mixins.map(m => line"$m")
    val mixinExtends = mixinLines.intercalate(line" with ")
    val mixinExtendsStatement =
      if (mixinExtends.segments.isEmpty) Line.empty
      else line"$mixinExtends with "
    val projectors: Lines = obj(NameRef("project"))(
      alts.map { alt =>
        val ident = NameDef(uncapitalise(alt.name))
        val maybeMap = alt.member match {
          case UnionMember.TypeCase(_) => line".map(_.$ident)"
          case _                       => Line.empty
        }
        line"def $ident: Option[${caseNameType(name, alt)}] = ${caseNameWithAlt(name, alt)}.project.lift(self)$maybeMap"
      }
    )
    val visitor: Lines = lines(
      block(line"trait Visitor[A]")(
        alts.map { alt =>
          val ident = NameDef(uncapitalise(alt.name))
          line"def $ident(value: ${caseNameType(name, alt)}): A"
        }
      ),
      newline,
      block(line"object Visitor")(
        block(line"trait Default[A] extends Visitor[A]")(
          line"def default: A",
          alts.map { alt =>
            val ident = NameDef(uncapitalise(alt.name))
            line"def $ident(value: ${caseNameType(name, alt)}): A = default"
          }
        )
      )
    )
    val accept: Lines =
      block(line"def accept[A](visitor: $name.Visitor[A]): A = this match")(
        alts.map { alt =>
          val ident = NameDef(uncapitalise(alt.name))
          val innerValue = alt.member match {
            case UnionMember.TypeCase(_) => line".$ident"
            case _                       => Line.empty
          }
          line"case value: ${caseNameForMatch(name, alt)} => visitor.$ident(value$innerValue)"
        }
      )
    lines(
      documentationAnnotation(hints),
      deprecationAnnotation(hints),
      block(
        line"sealed trait ${NameDef(name.name)} extends ${mixinExtendsStatement}scala.Product with scala.Serializable"
      ).withSameLineValue(line" self =>")(
        line"@inline final def widen: $name = this",
        line"def $$ordinal: Int",
        newline,
        projectors,
        newline,
        accept
      ),
      obj(name, companionTpe)(
        newline,
        alts.map(smartConstructor),
        newline,
        renderId(shapeId),
        newline,
        renderHintsVal(hints),
        newline,
        renderPrisms(name, alts, hints),
        alts.zipWithIndex.map {
          case (a @ Alt(_, realName, UnionMember.UnitCase, altHints), index) =>
            val cn = caseName(name, a)
            // format: off
            lines(
              documentationAnnotation(altHints),
              deprecationAnnotation(altHints),
              line"case object ${cn.nameDef} extends $name { final def $$ordinal: Int = $index }",
              line"""private val ${cn.nameDef}Alt = $Schema_.constant($cn)${renderConstraintValidation(altHints)}.oneOf[$name]("$realName").addHints(hints)""",
            )
            // format: on
          case (
                a @ Alt(altName, _, UnionMember.TypeCase(tpe), altHints),
                index
              ) =>
            val cn = caseName(name, a)
            lines(
              documentationAnnotation(altHints),
              deprecationAnnotation(altHints),
              line"final case class ${cn.nameDef}(${uncapitalise(altName)}: $tpe) extends $name { final def $$ordinal: Int = $index }"
            )
          case (
                Alt(_, realName, UnionMember.ProductCase(struct), altHints),
                index
              ) =>
            val additionalLines = lines(
              newline,
              line"""val alt = schema.oneOf[$name]("$realName")"""
            )
            // In case of union members that are inline structs (as opposed to structs being referenced and wrapped by a new class),
            // we want to put a deprecation note (if it exists on the alt) on the struct - there's nowhere else to put it.
            renderProduct(
              // putting alt hints first should result in higher priority of these.
              // might need deduplication (although the Hints type will take care of it, just in case)
              struct.copy(hints = altHints ++ struct.hints),
              adtParent = Some(name),
              additionalLines,
              classBody = Lines(line"def $$ordinal: Int = $index")
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
            val cn = caseName(name, a)
            block(line"object ${cn.nameDef}")(
              renderHintsVal(altHints),
              // format: off
              line"val schema: $Schema_[$cn] = $bijection_(${tpe.schemaRef}.addHints(hints)${renderConstraintValidation(altHints)}, $cn(_), _.${uncapitalise(altName)})",
              line"""val alt = schema.oneOf[$name]("$realName")""",
              // format: on
            )
        },
        newline,
        visitor,
        newline,
        locally {
          val union =
            if (recursive)
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
              line"_.$$ordinal"
            }
            .appendToLast(
              if (error) "" else ".withId(id).addHints(hints)"
            )
            .appendToLast(if (recursive) ")" else "")
        },
        renderErrorSchemaMethods(name, types).when(error),
        renderTypeclasses(hints, name)
      )
    )
  }

  private def fieldToRenderLine(
      field: Field,
      noDefault: Boolean = false
  ): Line = {
    import field._
    val ` = ` = Literal(" = ")
    val shouldRenderDefault = !noDefault && !field.hints.contains(Hint.NoDefault)
    val tpeLine = line"$tpe"
    val tpeAndDefault = if (shouldRenderDefault) {
      field.modifier match {
        case Field.Modifier.RequiredMod                          => tpeLine
        case Field.Modifier.RequiredDefaultMod(_, Some(default)) => tpeLine + ` = ` + renderDefault(default)
        case Field.Modifier.RequiredDefaultMod(_, None)          => tpeLine
        case Field.Modifier.DefaultMod(_, Some(default))         => tpeLine + ` = ` + renderDefault(default)
        case Field.Modifier.DefaultMod(_, None)                  => tpeLine
        case Field.Modifier.NoModifier => Line.optional(tpeLine) + ` = ` + NameRef("scala.None")
      }
    } else if (field.modifier.none) {
      Line.optional(tpeLine)
    } else {
      tpeLine
    }

    line"$name: " + tpeAndDefault
  }

  private def renderArgs(fields: List[Field]): Line = fields
    .map { f =>
      deprecationAnnotation(f.hints).appendIf(_.nonEmpty)(Line.space) +
        fieldToRenderLine(f)
    }
    .intercalate(Line.comma)

  private def renderEnum(
      shapeId: ShapeId,
      name: NameRef,
      tag: EnumTag,
      values: List[EnumValue],
      hints: List[Hint]
  ): Lines = {
    val isOpen = hints.contains(Hint.OpenEnum)
    val isIntEnum = tag match {
      case EnumTag.IntEnum | EnumTag.OpenIntEnum => true
      case _                                     => false
    }
    lines(
      documentationAnnotation(hints),
      deprecationAnnotation(hints),
      block(
        line"sealed abstract class ${name.name}(_value: $string_, _name: $string_, _intValue: $int_, _hints: $Hints_) extends $Enumeration_.Value"
      )(
        line"override type EnumType = $name",
        line"override val value: $string_ = _value",
        line"override val name: $string_ = _name",
        line"override val intValue: $int_ = _intValue",
        line"override val hints: $Hints_ = _hints",
        line"override def enumeration: $Enumeration_[EnumType] = $name",
        line"@inline final def widen: $name = this"
      ),
      obj(name, ext = line"$Enumeration_[$name]", w = line"${shapeTag(name)}")(
        renderId(shapeId),
        newline,
        renderHintsVal(hints),
        newline,
        renderPrismsEnum(name, values, hints, isOpen),
        values.map { case e @ EnumValue(value, intValue, _, _, hints) =>
          val valueName = NameRef(e.name)

          val baseLine =
            line"""case object $valueName extends $name("$value", "${e.realName}", $intValue, $Hints_.empty)"""

          lines(
            documentationAnnotation(hints),
            deprecationAnnotation(hints),
            if (e.hints.isEmpty) baseLine
            else
              block(baseLine)(
                line"override val hints: $Hints_ = $Hints_(${memberHints(e.hints)}).lazily"
              )
          )
        },
        if (isOpen) {
          val (paramName, paramType) =
            if (isIntEnum) ("int", "Int") else ("str", "String")
          val intValue = if (isIntEnum) paramName else "-1"
          val stringValue = if (isIntEnum) "\"$Unknown\"" else paramName
          lines(
            line"""final case class $$Unknown($paramName: $paramType) extends $name($stringValue, "$$Unknown", $intValue, Hints.empty)""",
            newline,
            line"val $$unknown: $paramType => $name = $$Unknown(_)"
          )
        } else Lines.empty,
        newline,
        line"val values: $list[$name] = $list".args(
          values.map(_.name)
        ),
        renderEnumTag(name, tag),
        line"implicit val schema: $Schema_[$name] = $enumeration_(tag, values).withId(id).addHints(hints)",
        renderTypeclasses(hints, name)
      )
    )
  }

  private def renderNewtype(
      shapeId: ShapeId,
      name: NameRef,
      tpe: Type,
      recursive: Boolean,
      hints: List[Hint]
  ): Lines = {
    val definition =
      if (recursive) line"$recursive_("
      else Line.empty
    val trailingCalls =
      line".withId(id).addHints(hints)${renderConstraintValidation(hints)}"
    val closing = if (recursive) ")" else ""
    lines(
      documentationAnnotation(hints),
      deprecationAnnotation(hints),
      obj(name, line"$Newtype_[$tpe]")(
        renderId(shapeId),
        renderHintsVal(hints),
        line"val underlyingSchema: $Schema_[$tpe] = ${tpe.schemaRef}$trailingCalls",
        lines(
          line"implicit val schema: $Schema_[$name] = $definition$bijection_(underlyingSchema, asBijection)$closing"
        ),
        renderTypeclasses(hints, name)
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

    def renderAccessedParams: Line =
      if (op.input == Type.unit) Line.empty
      else if (op.hints.contains(Hint.PackedInputs)) {
        line"input"
      } else op.params.map(f => line"input.${f.name}").intercalate(Line.comma)

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
      case Type.Collection(collectionType, member, hints) =>
        val col = collectionType match {
          case CollectionType.List       => s"$schemaPkg_.list"
          case CollectionType.Set        => s"$schemaPkg_.set"
          case CollectionType.Vector     => s"$schemaPkg_.vector"
          case CollectionType.IndexedSeq => s"$schemaPkg_.indexedSeq"
        }
        val hintsLine =
          if (hints.isEmpty) Line.empty
          else line".addMemberHints(${memberHints(hints)})"
        line"${NameRef(col)}(${member.schemaRef}$hintsLine)"
      case Type.Map(key, keyHints, value, valueHints) =>
        val keyHintsLine =
          if (keyHints.isEmpty) Line.empty
          else line".addMemberHints(${memberHints(keyHints)})"
        val valueHintsLine =
          if (valueHints.isEmpty) Line.empty
          else line".addMemberHints(${memberHints(valueHints)})"
        line"${NameRef(s"$schemaPkg_.map")}(${key.schemaRef}$keyHintsLine, ${value.schemaRef}$valueHintsLine)"
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
      case e @ Type.ExternalType(
            _,
            _,
            _,
            maybeProviderImport,
            underlyingTpe,
            hint
          ) =>
        line"${underlyingTpe.schemaRef}.refined[${e: Type}](${renderNativeHint(hint)})${maybeProviderImport
          .map { providerImport => Import(providerImport).toLine }
          .getOrElse(Line.empty)}"
      case Nullable(underlying) => line"${underlying.schemaRef}.option"
    }

    private def schemaRefP(primitive: Primitive): String = primitive match {
      case Primitive.Unit       => s"${schemaPkg_}.unit"
      case Primitive.Blob       => s"${schemaPkg_}.bytes"
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
    recursion
      .cata(renderTypedNode)(hint.typedNode)
      .run(true)
      ._2

  private def renderDefault(hint: Fix[TypedNode]): Line =
    recursion
      .cata(renderTypedNode)(hint)
      .run(true)
      ._2

  private def renderHint(hint: Hint): Option[Line] = hint match {
    case h: Hint.Native => renderNativeHint(h).some
    case _              => None
  }

  def renderId(shapeId: ShapeId): Line = {
    val ns = shapeId.getNamespace()
    val name = shapeId.getName()
    line"""val id: $ShapeId_ = $ShapeId_("$ns", "$name")"""
  }

  def renderEnumTag(parentType: NameRef, tag: EnumTag): Line = {
    val tagStr = tag match {
      case IntEnum                => "ClosedIntEnum"
      case StringEnum             => "ClosedStringEnum"
      case EnumTag.OpenIntEnum    => "OpenIntEnum($unknown)"
      case EnumTag.OpenStringEnum => "OpenStringEnum($unknown)"
    }
    line"val tag: $EnumTag_[$parentType] = $EnumTag_.$tagStr"
  }

  def renderHintsVal(hints: List[Hint]): Lines = {
    val lhs = line"val hints: $Hints_"

    hints.flatMap(renderHint) match {
      case Nil => lines(line"$lhs = $Hints_.empty")
      case args =>
        line"$lhs = $Hints_".args(args).appendToLast(".lazily")
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
  type CString = Contextual[(InCollection, Line)]

  implicit class ContextualOps(val line: Line) {
    def write: CString = (false, line).pure[Contextual]
    def writeCollection: CString = (true, line).pure[Contextual]
  }

  implicit class CStringOps(val str: CString) {
    def runDefault = str.run(false)._2
  }

  private def renderTypedNode(tn: TypedNode[CString]): CString = tn match {
    case EnumerationTN(ref, _, _, name) =>
      line"${ref.show + "." + name + ".widen"}".write
    case StructureTN(ref, fields) =>
      val fieldStrings = fields.map {
        case (name, FieldTN.RequiredTN(value)) =>
          line"$name = ${value.runDefault}"
        case (name, FieldTN.OptionalSomeTN(value)) =>
          line"$name = $some(${value.runDefault})"
        case (name, FieldTN.OptionalNoneTN) => line"$name = $none"
      }
      line"${ref.show}(${fieldStrings.intercalate(Line.comma)})".write
    case NewTypeTN(ref, target) =>
      Reader(topLevel => {
        val (wroteCollection, text) = target.run(topLevel)
        if (wroteCollection && !topLevel)
          false -> text
        else
          false -> line"${ref.show}($text)"
      })

    case AltTN(ref, altName, AltValueTN.TypeAltTN(alt)) =>
      line"${ref.show}.${altName.capitalize}Case(${alt.runDefault}).widen".write

    case AltTN(_, _, AltValueTN.ProductAltTN(alt)) =>
      alt.runDefault.write

    case CollectionTN(collectionType, values) =>
      val col = collectionType.tpe
      line"$col(${values.map(_.runDefault).intercalate(Line.comma)})".writeCollection
    case MapTN(values) =>
      line"$map(${values
        .map { case (k, v) => k.runDefault + line" -> " + v.runDefault }
        .intercalate(Line.comma)})".writeCollection
    case PrimitiveTN(prim, value) =>
      renderPrimitive[prim.T](prim)(value).write
  }

  private def renderPrimitive[T](prim: Primitive.Aux[T]): T => Line =
    prim match {
      case Primitive.BigDecimal =>
        (bd: BigDecimal) => line"scala.math.BigDecimal($bd)"
      case Primitive.BigInteger => (bi: BigInt) => line"scala.math.BigInt($bi)"
      case Primitive.Unit       => _ => line"()"
      case Primitive.Double     => t => line"${t.toString}d"
      case Primitive.Float      => t => line"${t.toString}f"
      case Primitive.Long       => t => line"${t.toString}L"
      case Primitive.Int        => t => line"${t.toString}"
      case Primitive.Short      => t => line"${t.toString}"
      case Primitive.Bool       => t => line"${t.toString}"
      case Primitive.Uuid       => uuid => line"java.util.UUID.fromString($uuid)"
      case Primitive.String     => renderStringLiteral
      case Primitive.Byte       => b => line"${b.toString}"
      case Primitive.Blob =>
        ba =>
          val blob = NameRef("smithy4s", "Blob")
          if (ba.isEmpty) line"$blob.empty"
          else
            line"$blob(Array[Byte](${ba.mkString(", ")}))"
      case Primitive.Timestamp =>
        ts => line"${NameRef("smithy4s", "Timestamp")}(${ts.toEpochMilli}, 0)"
      case Primitive.Document => { (node: Node) =>
        node.accept(new NodeVisitor[Line] {
          def arrayNode(x: ArrayNode): Line = {
            val innerValues = x.getElements().asScala.map(_.accept(this))
            line"smithy4s.Document.array(${innerValues.toList.intercalate(Line.comma)})"
          }
          def booleanNode(x: BooleanNode): Line =
            line"smithy4s.Document.fromBoolean(${x.getValue})"
          def nullNode(x: NullNode): Line =
            line"smithy4s.Document.nullDoc"
          def numberNode(x: NumberNode): Line =
            line"smithy4s.Document.fromDouble(${x.getValue.doubleValue()}d)"
          def objectNode(x: ObjectNode): Line = {
            val members = x.getMembers.asScala.map { member =>
              val key = s""""${member._1.getValue()}""""
              val value = member._2.accept(this)
              line"$key -> $value"
            }
            line"smithy4s.Document.obj(${members.toList.intercalate(Line.comma)})"
          }
          def stringNode(x: StringNode): Line =
            line"""smithy4s.Document.fromString(${renderStringLiteral(
              x.getValue
            )})"""
        })
      }
      case _ => _ => line"null"
    }

  private def renderStringLiteral(raw: String): Line = {
    import scala.reflect.runtime.universe._
    val str = Literal(Constant(raw))
      .toString()
      // Replace sequences like "\\uD83D" (how Smithy specs refer to unicode characters)
      // with unicode character escapes like "\uD83D" that can be parsed in the regex implementations on all platforms.
      // See https://github.com/disneystreaming/smithy4s/pull/499
      .replace("\\\\u", "\\u")
    // If the string contains "$", the use of -Xlint:missing-interpolator when
    // compiling the generated code will result in warnings. To prevent that we
    // render any such strings as interpolated strings (even though that would
    // otherwise be unecessary) so that we can render "$" as "$$", which get
    // converted back to "$" during interpolation.
    val escaped = if (str.contains('$')) s"s${str.replace("$", "$$")}" else str

    line"$escaped"
  }
}
