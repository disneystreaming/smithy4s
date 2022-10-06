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
import smithy4s.xml.internals.XmlCursor.SingleNode

object XmlSchemaVisitor extends XmlSchemaVisitor

abstract class XmlSchemaVisitor
    extends SchemaVisitor[XmlDecoder]
    with smithy4s.ScalaCompat { compile =>
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
      def read(cursor: XmlCursor): Either[XmlDecodeError, C[A]] = {
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
            Left(XmlDecodeError(other.history, s"Expected one or multiple nodes"))
        }
      }
    }
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): XmlDecoder[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _)).addHints(hints)
    }
    compile(Schema.vector(kvSchema.addHints(XmlName("entry")))).map(_.toMap)
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): XmlDecoder[E] = {
    val isIntEnum = hints.has(IntEnum)
    if (isIntEnum) {
      val desc = s"enum[${values.map(_.intValue).mkString(", ")}]"
      val valueMap = values.map(ev => ev.intValue -> ev.value).toMap
      XmlDecoder.fromStringParser(desc)(_.toIntOption.flatMap(valueMap.get))
    } else {
      val desc = s"enum[${values.map(_.stringValue).mkString(", ")}]"
      val valueMap = values.map(ev => ev.stringValue -> ev.value).toMap
      XmlDecoder.fromStringParser(desc)(valueMap.get)
    }
  }

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
      def read(cursor: XmlCursor): Either[XmlDecodeError, S] =
        readers.traverse(_.read(cursor)).map(make)
    }
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): XmlDecoder[U] = {
    def altDecoder[A](alt: SchemaAlt[U, A]): (String, XmlDecoder[U]) = {
      val xmlName =
        alt.instance.hints.get(XmlName).map(_.value).getOrElse(alt.label)
      val decoder = compile(alt.instance).map(alt.inject)
      (xmlName, decoder)
    }
    val altMap = alternatives.map(altDecoder(_)).toMap[String, XmlDecoder[U]]
    new XmlDecoder[U] {
      def read(cursor: XmlCursor): Either[XmlDecodeError, U] = cursor match {
        case s @ SingleNode(history, node) =>
          val xmlName = node.name
          altMap.get(node.name) match {
            case Some(value) => value.read(s)
            case None =>
              Left(XmlDecodeError(history, s"Not a valid alternative: $xmlName"))
          }
        case other =>
          Left(XmlDecodeError(other.history, "Expected a single node"))
      }
    }
  }

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
    def read(cursor: XmlCursor): Either[XmlDecodeError, A] = {
      underlying.read(cursor)
    }
  }

}
