package smithy4s.aws.query

import smithy4s.schema._
import smithy.api.{XmlFlattened, XmlName}
import smithy4s.{Schema => _, _}

private[aws] class AwsSchemaVisitorAwsQueryCodec(
    val cache: CompilationCache[AwsQueryCodec]
) extends SchemaVisitor.Cached[AwsQueryCodec] { compile =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): AwsQueryCodec[P] =
    (p: P) => {
      Primitive.stringWriter(tag, hints) match {
        case Some(writer) => FormData.simple(writer(p))
        case None         => FormData.empty
      }
    }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): AwsQueryCodec[C[A]] = {
    val memberWriter = compile(member)
    val key = if (hints.has[XmlFlattened]) {
      Nil
    } else {
      List(getKey(hints, "member"))
    }
    println(s"KEY: ${key}")

    (collection: C[A]) =>
      tag
        .iterator(collection)
        .zipWithIndex
        .map { case (member, index) =>
          val newKey = ((index + 1).toString :: key).reverse.mkString(".")
          println(s"NEW KEY: ${newKey}")
          val data = memberWriter(member)
          println(s"DATA: ${data}")
          data.childOf(newKey)
        }
        .reduce(_ combine _)
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): AwsQueryCodec[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _)).addHints(XmlName("entry"))
    }
    val schema =
      Schema.vector(kvSchema).addHints(hints)
    val codec = compile(schema)

    (m: Map[K, V]) => codec(m.toVector)
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): AwsQueryCodec[E] = {
    if (hints.has(IntEnum))
      (value: E) => FormData.simple(total(value).intValue.toString)
    else (value: E) => FormData.simple(total(value).stringValue)
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): AwsQueryCodec[S] = {
    def fieldEncoder[A](field: SchemaField[S, A]): AwsQueryCodec[S] = {
      val fieldKey = getKey(field.hints, field.label)

      val encoder = field.foldK(new Field.FolderK[Schema, S, AwsQueryCodec] {
        override def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): AwsQueryCodec[AA] = (a: AA) => compile(instance)(a)

        override def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): AwsQueryCodec[Option[AA]] = {
          case Some(value) => compile(instance)(value)
          case None        => FormData.empty
        }
      })

      (s: S) => encoder(field.get(s)).childOf(fieldKey)
    }

    val codecs: Vector[AwsQueryCodec[S]] =
      fields.map(field => fieldEncoder(field))

    val structKey = getKey(hints, shapeId.name)

    (s: S) =>
      codecs
        .map(codec => codec(s))
        .reduce(_ combine _)
        .childOf(structKey)
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): AwsQueryCodec[U] = {

    def encode[A](u: U, alt: SchemaAlt[U, A]): FormData = {
      val key = getKey(alt.hints, alt.label)
      dispatch
        .projector(alt)(u)
        .fold(FormData.empty)(a => compile(alt.instance)(a))
        .childOf(key)
    }

    (u: U) =>
      alternatives
        .map(alt => encode(u, alt))
        .reduce((l: FormData, r: FormData) => l.combine(r))
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): AwsQueryCodec[B] = (b: B) => compile(schema)(bijection.from(b))

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): AwsQueryCodec[B] = (b: B) => compile(schema)(refinement.from(b))

  override def lazily[A](suspend: Lazy[Schema[A]]): AwsQueryCodec[A] = (a: A) =>
    suspend.map(schema => compile(schema)).value(a)

  private def getKey(hints: Hints, default: String): String =
    hints
      .get(XmlName)
      .map(_.value)
      .getOrElse(default)
}
