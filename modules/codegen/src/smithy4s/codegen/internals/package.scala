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

import alloy.UuidFormatTrait
import cats.syntax.all._
import smithy4s.codegen.internals.LineSegment.NameRef
import smithy4s.codegen.internals.LineSyntax.LineInterpolator
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.HttpErrorTrait
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.traits.RequiredTrait
import software.amazon.smithy.model.traits.TimestampFormatTrait

import java.{util => ju}
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

package object internals {

  val uuidShapeId = ShapeId.from("alloy#UUID")

  private[internals] type LinesWithValue = WithValue.ToLinesWithValue[_]
  private[internals] type LineWithValue = WithValue.ToLineWithValue[_]
  private[internals] implicit class LinesSyntaxWithValue[A](
      val value: WithValue.ToLinesWithValue[A]
  ) extends AnyVal {
    def render: Lines = value.typeclass.render(value.value)
  }
  implicit class LineSyntaxWithValue[A](val value: WithValue.ToLineWithValue[A])
      extends AnyVal {
    def render: Line = value.typeclass.render(value.value)
  }

  private[internals] val empty: Lines = Lines.empty
  private[internals] val newline: Lines = Lines(Line.empty)
  private[internals] def lines(l: LinesWithValue*): Lines =
    l.toList.foldMap(_.render)

  private[internals] def indent(l: List[Line]): List[Line] = l.map { line =>
    if (line.segments.length > 0)
      line"  " + line
    else
      line
  }

  private[internals] def indent(l: LinesWithValue*): Lines =
    lines(l: _*).transformLines(indent)

  private[internals] def block(l: Line): PartialBlock = new PartialBlock(l)

  private[internals] def obj(
      name: NameRef,
      ext: Line,
      w: Line = Line.empty
  ): PartialBlock = {
    val start = line"object $name"
    val withExt = if (ext.nonEmpty) start combine line" extends $ext" else start
    val withW = if (w.nonEmpty) withExt combine line" with $w" else withExt
    new PartialBlock(withW)
  }
  private[internals] def obj(
      name: NameRef
  ): PartialBlock = {
    obj(name, Line.empty)
  }

  private[internals] def commas(lines: List[String]): List[String] =
    lines match {
      case Nil               => Nil
      case list @ (_ :: Nil) => list
      case head :: tl        => (head + ",") :: commas(tl)
    }

  private[internals] object S {
    object Blob extends ShapeExtractor(_.asBlobShape())
    object Boolean extends ShapeExtractor(_.asBooleanShape())
    object String extends ShapeExtractor(_.asStringShape())
    object Timestamp extends ShapeExtractor(_.asTimestampShape())
    object Byte extends ShapeExtractor(_.asByteShape())
    object Short extends ShapeExtractor(_.asShortShape())
    object Integer extends ShapeExtractor(_.asIntegerShape())
    object Float extends ShapeExtractor(_.asFloatShape())
    object Document extends ShapeExtractor(_.asDocumentShape())
    object Double extends ShapeExtractor(_.asDoubleShape())
    object Long extends ShapeExtractor(_.asLongShape())
    object BigDecimal extends ShapeExtractor(_.asBigDecimalShape())
    object BigInteger extends ShapeExtractor(_.asBigIntegerShape())
    object List extends ShapeExtractor(_.asListShape())
    @nowarn("msg=method asSetShape in class Shape is deprecated")
    object Set extends ShapeExtractor(_.asSetShape())
    object Map extends ShapeExtractor(_.asMapShape())
    object Structure extends ShapeExtractor(_.asStructureShape())
    object Union extends ShapeExtractor(_.asUnionShape())
    object Enumeration extends ShapeExtractor(_.asEnumShape())
    object IntEnumeration extends ShapeExtractor(_.asIntEnumShape())
    object Service extends ShapeExtractor(_.asServiceShape())
    object Resource extends ShapeExtractor(_.asResourceShape())
    object Operation extends ShapeExtractor(_.asOperationShape())
    object Member extends ShapeExtractor(_.asMemberShape())
  }

  private[internals] object T {
    object http extends TraitExtractor[HttpTrait]
    object error extends TraitExtractor[ErrorTrait]
    object httpError extends TraitExtractor[HttpErrorTrait]
    object required extends TraitExtractor[RequiredTrait]
    @annotation.nowarn("msg=class EnumTrait in package traits is deprecated")
    object enumeration extends TraitExtractor[EnumTrait]
    object timestampFormat extends TraitExtractor[TimestampFormatTrait]
    object uuidFormat extends TraitExtractor[UuidFormatTrait]
  }

  private[internals] object N {
    object ObjectNode
        extends NodeExtractor(node =>
          node
            .asObjectNode()
            .asScala
            .map(_.getMembers().asScala.map(_.leftMap(_.getValue())))
        )
    private[internals] object MapNode
        extends NodeExtractor(node =>
          node
            .asObjectNode()
            .asScala
            .map(_.getMembers().asScala.toList)
        )
    private[internals] object ArrayNode
        extends NodeExtractor(node =>
          node.asArrayNode().asScala.map(_.asScala.toList)
        )
    private[internals] object NumberNode
        extends NodeExtractor(node =>
          node.asNumberNode().asScala.map(_.getValue())
        )
    private[internals] object NullNode {
      def unapply(node: Node): Boolean = node.isNullNode()
    }
    private[internals] object StringNode
        extends NodeExtractor(node =>
          node.asStringNode().asScala.map(_.getValue())
        )
    private[internals] object BooleanNode
        extends NodeExtractor(node =>
          node.asBooleanNode().asScala.map(_.getValue())
        )
  }

  private[internals] implicit class IterOps[A](i: Iterable[A]) {
    def filteredForeach(pf: PartialFunction[A, Unit]) = i.foreach { a =>
      if (pf.isDefinedAt(a)) pf(a)
      else ()
    }
  }

  private[internals] implicit class OptionalOps[A](opt: ju.Optional[A]) {
    def asScala: Option[A] = if (opt.isPresent()) Some(opt.get()) else None
  }

  private[internals] def uncapitalise(s: String) = {
    if (s == s.toUpperCase()) s.toLowerCase()
    else {
      val count: Int = s.segmentLength(c => c.==('_'), 0)
      val after = count + 1
      s.take(after).toLowerCase + s.drop(after)
    }
  }
}
