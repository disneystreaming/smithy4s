package smithy4s.xml
package internals

import smithy4s.schema._
import smithy4s.schema.Schema
import smithy4s._
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy.api.XmlAttribute
import cats.syntax.all._
import smithy4s.internals.SchemaDescription

object XmlSchemaVisitor extends XmlSchemaVisitor

abstract class XmlSchemaVisitor extends SchemaVisitor[XmlDecoder] { compile =>
  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): XmlDecoder[P] = {
    val desc = SchemaDescription.primitive(shapeId, hints, tag)
    Primitive.stringParser(tag, hints) match {
      case Some(parser) => XmlDecoder.fromStringParser(desc)(parser)
      case None => XmlDecoder.alwaysFailing(s"Cannot decode $desc from XML")
    }
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): XmlDecoder[C[A]] = {
    val xmlName = member.hints.get(XmlName).map(_.value).getOrElse("member")
    val isFlattened = hints.has(XmlFlattened)
    val memberReader = compile(member)
    new XmlDecoder[C[A]] {
      def read(cursor: XmlCursor): Either[XmlReadError, C[A]] = {
        val realCursor = if (isFlattened) cursor else cursor.down(xmlName)
        realCursor match {
          case XmlCursor.MultipleNodes(history, nodes) =>
            nodes.zipWithIndex
              .traverse { case (elem, index) =>
                memberReader.read(
                  XmlCursor.SingleNode(history.appendIndex(index), elem)
                )
              }
              .map(list => tag.fromIterator(list.iterator))
          case XmlCursor.SingleNode(history, elem) =>
            memberReader
              .read(XmlCursor.SingleNode(history.appendIndex(0), elem))
              .map(value => tag.fromIterator(Iterator.single(value)))
          case XmlCursor.NoNode(_) => Right(tag.empty)
          case other =>
            Left(XmlReadError(other.history, s"Expected one or multiple nodes"))
        }
      }
    }
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): XmlDecoder[Map[K, V]] = ???

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): XmlDecoder[E] = ???

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): XmlDecoder[S] = {
    def fieldReader[A](field: SchemaField[S, A]): XmlDecoder[A] = {
      val isAttribute = field.instance.hints.has(XmlAttribute)
      val xmlName =
        field.instance.hints.get(XmlName).map(_.value).getOrElse(field.label)
      field
        .foldK(new Field.FolderK[Schema, S, XmlDecoder] {
          def onRequired[AA](
              label: String,
              instance: Schema[AA],
              get: S => AA
          ): XmlDecoder[AA] = {
            if (isAttribute) compile(instance).attribute(xmlName)
            else compile(instance).down(xmlName)
          }
          def onOptional[AA](
              label: String,
              instance: Schema[AA],
              get: S => Option[AA]
          ): XmlDecoder[Option[AA]] = {
            if (isAttribute) compile(instance).optional.attribute(xmlName)
            else compile(instance).optional.down(xmlName)
          }
        })
    }
    val readers = fields.map(fieldReader(_))
    new XmlDecoder[S] {
      def read(cursor: XmlCursor): Either[XmlReadError, S] =
        readers.traverse(_.read(cursor)).map(make)
    }
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): XmlDecoder[U] = ???

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): XmlDecoder[B] =
    schema.compile(this).map(bijection.to)

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): XmlDecoder[B] =
    schema.compile(this).emap(refinement.asFunction)

  def lazily[A](suspend: Lazy[Schema[A]]): XmlDecoder[A] = new XmlDecoder[A] {
    lazy val underlying: XmlDecoder[A] = suspend.map(compile(_)).value
    def read(cursor: XmlCursor): Either[XmlReadError, A] = {
      underlying.read(cursor)
    }
  }

}
