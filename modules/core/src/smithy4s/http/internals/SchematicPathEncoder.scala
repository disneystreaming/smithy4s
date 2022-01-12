package smithy4s
package http.internals

import smithy.api.TimestampFormat
import schematic.Field
import smithy4s.internals.Hinted
import smithy.api.Http
import smithy4s.http.PathSegment
import smithy4s.http.PathSegment.StaticSegment
import smithy4s.http.PathSegment.LabelSegment
import smithy4s.http.PathSegment.GreedySegment

object SchematicPathEncoder
    extends Schematic[PathEncode.Make]
    with schematic.StubSchematic[PathEncode.Make] {

  def default[A]: PathEncode.Make[A] = PathEncode.Make.noop
  def document: PathEncode.Make[Document] = PathEncode.Make.noop
  def withHints[A](
      fa: PathEncode.Make[A],
      hints: smithy4s.Hints
  ): PathEncode.Make[A] =
    fa.addHints(hints)

  override def bijection[A, B](
      f: PathEncode.Make[A],
      to: A => B,
      from: B => A
  ): PathEncode.Make[B] =
    Hinted[PathEncode.MaybePathEncode, B](
      f.hints,
      make = hints => f.make(hints).map(_.contramap(from))
    )

  override val bigdecimal: PathEncode.Make[BigDecimal] =
    PathEncode.Make.fromToString
  override val bigint: PathEncode.Make[BigInt] = PathEncode.Make.fromToString
  override val double: PathEncode.Make[Double] = PathEncode.Make.fromToString
  override val int: PathEncode.Make[Int] = PathEncode.Make.fromToString
  override val float: PathEncode.Make[Float] = PathEncode.Make.fromToString
  override val short: PathEncode.Make[Short] = PathEncode.Make.fromToString
  override val long: PathEncode.Make[Long] = PathEncode.Make.fromToString
  override val string: PathEncode.Make[String] = PathEncode.Make.fromToString
  override val uuid: PathEncode.Make[java.util.UUID] =
    PathEncode.Make.fromToString
  override val boolean: PathEncode.Make[Boolean] = PathEncode.Make.fromToString
  override val timestamp: PathEncode.Make[Timestamp] =
    PathEncode.Make.from(_.format(TimestampFormat.DATE_TIME))
  override val unit: PathEncode.Make[Unit] =
    genericStruct(Vector.empty)(_ => ())

  def genericStruct[S](fields: Vector[Field[PathEncode.Make, S, _]])(
      const: Vector[Any] => S
  ): PathEncode.Make[S] = {
    type Writer = (StringBuilder, S) => Unit

    def toPathEncoder[A](
        field: Field[PathEncode.Make, S, A],
        greedy: Boolean
    ): Option[Writer] = {
      field.fold(new Field.Folder[PathEncode.Make, S, Option[Writer]] {
        def onRequired[AA](
            label: String,
            instance: PathEncode.Make[AA],
            get: S => AA
        ): Option[Writer] =
          if (greedy)
            instance.get.map(encoder =>
              (sb, s) => encoder.encodeGreedy(sb, get(s))
            )
          else
            instance.get.map(encoder => (sb, s) => encoder.encode(sb, get(s)))
        def onOptional[AA](
            label: String,
            instance: PathEncode.Make[AA],
            get: S => Option[AA]
        ): Option[Writer] = None
      })
    }

    def compile1(path: PathSegment): Option[Writer] = path match {
      case StaticSegment(value) =>
        Some((sb, _) => { val _ = sb.append(value) })
      case LabelSegment(value) =>
        fields
          .find(_.label == value)
          .flatMap(field => toPathEncoder(field, greedy = false))
      case GreedySegment(value) =>
        fields
          .find(_.label == value)
          .flatMap(field => toPathEncoder(field, greedy = true))
    }

    def compilePath(path: Vector[PathSegment]): Option[Vector[Writer]] =
      path.traverse(compile1(_))

    Hinted[PathEncode.MaybePathEncode].onHintOpt[Http, S] { maybeHttpHint =>
      for {
        httpHint <- maybeHttpHint
        path <- pathSegments(httpHint.uri.value)
        writers <- compilePath(path)
      } yield new PathEncode[S] {
        def encode(sb: StringBuilder, s: S): Unit = {
          var first = true
          writers.foreach { w =>
            if (first) { w.apply(sb, s); first = false }
            else w.apply(sb.append('/'), s)
          }
        }

        def encodeGreedy(sb: StringBuilder, s: S) = ()
      }
    }
  }
}
