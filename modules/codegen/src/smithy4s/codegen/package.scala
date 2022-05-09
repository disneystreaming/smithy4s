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

package smithy4s

import cats.syntax.all._
import smithy4s.api.UuidFormatTrait
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.HttpErrorTrait
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.traits.RequiredTrait
import software.amazon.smithy.model.traits.TimestampFormatTrait

import java.{util => ju}
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.traits.EnumTrait

package object codegen {

  val uuidShapeId = ShapeId.from("smithy4s.api#UUID")

  private[codegen] type Lines = Renderable.WithValue[_]

  private[codegen] val empty: RenderResult = RenderResult.empty
  private[codegen] val newline: RenderResult = RenderResult(List(""))
  private[codegen] def line(l: Lines): RenderResult = lines(l)
  private[codegen] def lines(l: Lines*): RenderResult =
    l.toList.foldMap(_.render)

  private[codegen] def indent(l: List[String]): List[String] = l.map { line =>
    if (line.length > 0)
      "  " + line
    else
      line
  }
  private[codegen] def indent(l: Lines*): RenderResult =
    lines(l: _*).transformLines(indent)

  private[codegen] def block(s: String): PartialBlock = new PartialBlock(s)

  private[codegen] def obj(
      name: String,
      ext: String = "",
      w: String = ""
  ): PartialBlock = {
    var str = s"object $name"
    if (ext.nonEmpty) str += s" extends $ext"
    if (w.nonEmpty) str += s" with $w"
    new PartialBlock(str)
  }

  private[codegen] def obj(
      name: String,
      extensions: Seq[String]
  ): PartialBlock = {
    obj(name, extensions.mkString(" with "))
  }

  private[codegen] def commas(lines: List[String]): List[String] =
    lines match {
      case Nil               => Nil
      case list @ (_ :: Nil) => list
      case head :: tl        => (head + ",") :: commas(tl)
    }

  object S {
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
    object Set extends ShapeExtractor(_.asSetShape())
    object Map extends ShapeExtractor(_.asMapShape())
    object Structure extends ShapeExtractor(_.asStructureShape())
    object Union extends ShapeExtractor(_.asUnionShape())
    object Enumeration extends ShapeExtractor(_.asEnumShape())
    object Service extends ShapeExtractor(_.asServiceShape())
    object Resource extends ShapeExtractor(_.asResourceShape())
    object Operation extends ShapeExtractor(_.asOperationShape())
    object Member extends ShapeExtractor(_.asMemberShape())
  }

  object T {
    object http extends TraitExtractor[HttpTrait]
    object error extends TraitExtractor[ErrorTrait]
    object httpError extends TraitExtractor[HttpErrorTrait]
    object required extends TraitExtractor[RequiredTrait]
    @annotation.nowarn("msg=class EnumTrait in package traits is deprecated")
    object enumeration extends TraitExtractor[EnumTrait]
    object timestampFormat extends TraitExtractor[TimestampFormatTrait]
    object uuidFormat extends TraitExtractor[UuidFormatTrait]
  }

  object N {
    object ObjectNode
        extends NodeExtractor(node =>
          node
            .asObjectNode()
            .asScala
            .map(_.getMembers().asScala.map(_.leftMap(_.getValue())))
        )
    object MapNode
        extends NodeExtractor(node =>
          node
            .asObjectNode()
            .asScala
            .map(_.getMembers().asScala.toList)
        )
    object ArrayNode
        extends NodeExtractor(node =>
          node.asArrayNode().asScala.map(_.asScala.toList)
        )
    object NumberNode
        extends NodeExtractor(node =>
          node.asNumberNode().asScala.map(_.getValue())
        )
    object NullNode {
      def unapply(node: Node): Boolean = node.isNullNode()
    }
    object StringNode
        extends NodeExtractor(node =>
          node.asStringNode().asScala.map(_.getValue())
        )
    object BooleanNode
        extends NodeExtractor(node =>
          node.asBooleanNode().asScala.map(_.getValue())
        )
  }

  implicit class IterOps[A](i: Iterable[A]) {
    def filteredForeach(pf: PartialFunction[A, Unit]) = i.foreach { a =>
      if (pf.isDefinedAt(a)) pf(a)
      else ()
    }
  }

  implicit class OptionalOps[A](opt: ju.Optional[A]) {
    def asScala: Option[A] = if (opt.isPresent()) Some(opt.get()) else None
  }

  def uncapitalise(s: String) =
    if (s == s.toUpperCase()) s.toLowerCase()
    else s.take(1).toLowerCase() + s.drop(1)

}
