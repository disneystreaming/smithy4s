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
package http
package internals

import smithy4s.internals.Hinted
import smithy4s.internals.InputOutput
import smithy4s.schema._

import java.{util => ju}
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}

import MetaDecode._

private[http] object SchematicMetadataReader extends SchematicMetadataReader

/**
  * This construct creates a Map[String Any] by pulling expected
  * values from   http request's metadata (ie not the body)
  *
  * This Map is then passed to the Schematic that is aware
  * of the request's body, for quick access.
  */
private[http] class SchematicMetadataReader()
    extends Schematic[MetaDecode.Make]
    with ScalaCompat {

  def short: MetaDecode.Make[Short] =
    MetaDecode.Make
      .from("Short")(
        _.toShortOption
      )

  def int: MetaDecode.Make[Int] =
    MetaDecode.Make
      .from("Int")(_.toIntOption)

  def long: MetaDecode.Make[Long] =
    MetaDecode.Make
      .from("Long")(_.toLongOption)

  def double: MetaDecode.Make[Double] =
    MetaDecode.Make
      .from("Double")(
        _.toDoubleOption
      )

  def float: MetaDecode.Make[Float] =
    MetaDecode.Make
      .from("Float")(
        _.toFloatOption
      )

  def bigdecimal: MetaDecode.Make[BigDecimal] =
    MetaDecode.Make.fromUnsafe("BigDecimal")(BigDecimal(_))
  def bigint: MetaDecode.Make[BigInt] =
    MetaDecode.Make.fromUnsafe("BigInt")(BigInt(_))

  def string: MetaDecode.Make[String] =
    MetaDecode.Make.from("String")(Some(_))

  def boolean: MetaDecode.Make[Boolean] =
    MetaDecode.Make.from("Boolean") {
      case "true"  => Some(true)
      case "false" => Some(false)
      case _       => None
    }

  def byte: MetaDecode.Make[Byte] = MetaDecode.Make.empty

  def unit: MetaDecode.Make[Unit] = Hinted.static {
    MetaDecode.StructureMetaDecode(
      _ => Right(MMap.empty[String, Any]),
      Some(_ => Right(()))
    )
  }

  def uuid: MetaDecode.Make[ju.UUID] =
    MetaDecode.Make.fromUnsafe("UUID")(ju.UUID.fromString)

  def bytes: MetaDecode.Make[ByteArray] =
    MetaDecode.Make.fromUnsafe("Bytes")(string =>
      ByteArray(ju.Base64.getDecoder().decode(string))
    )

  def timestamp: MetaDecode.Make[Timestamp] = Hinted[MetaDecode].from {
    (hints: Hints) =>
      (
        hints.get(HttpBinding).map(_.tpe),
        hints.get(smithy.api.TimestampFormat)
      ) match {
        case (_, Some(format)) =>
          MetaDecode.from(Timestamp.showFormat(format))(str =>
            Timestamp.parse(str, format)
          )
        case (Some(HttpBinding.Type.QueryType), None) |
            (Some(HttpBinding.Type.PathType), None) =>
          val formatString =
            Timestamp.showFormat(smithy.api.TimestampFormat.DATE_TIME)
          // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpquery-trait
          MetaDecode.from(formatString)(str =>
            Timestamp.parse(str, smithy.api.TimestampFormat.DATE_TIME)
          )
        case (Some(HttpBinding.Type.HeaderType), None) =>
          val formatString =
            Timestamp.showFormat(smithy.api.TimestampFormat.HTTP_DATE)
          // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpheader-trait
          MetaDecode.from(formatString)(str =>
            Timestamp.parse(str, smithy.api.TimestampFormat.HTTP_DATE)
          )
        case (None, None) =>
          MetaDecode.EmptyMetaDecode
      }
  }

  def document: Make[Document] = MetaDecode.Make.empty

  def list[S](fs: MetaDecode.Make[S]): MetaDecode.Make[List[S]] =
    fs.transform[List[S]] {
      case MetaDecode.StringValueMetaDecode(f) =>
        MetaDecode.StringListMetaDecode[List[S]] { iterator =>
          val buffer = ListBuffer.empty[S]
          iterator.foreach(string => buffer += f(string))
          buffer.toList
        }
      case _ => MetaDecode.EmptyMetaDecode
    }

  def set[S](fs: MetaDecode.Make[S]): MetaDecode.Make[Set[S]] =
    fs.transform[Set[S]] {
      case MetaDecode.StringValueMetaDecode(f) =>
        MetaDecode.StringListMetaDecode[Set[S]] { iterator =>
          val buffer = ListBuffer.empty[S]
          iterator.foreach(string => buffer += f(string))
          buffer.toSet
        }
      case _ => MetaDecode.EmptyMetaDecode
    }

  def vector[S](fs: MetaDecode.Make[S]): MetaDecode.Make[Vector[S]] =
    fs.transform[Vector[S]] {
      case MetaDecode.StringValueMetaDecode(f) =>
        MetaDecode.StringListMetaDecode[Vector[S]] { iterator =>
          val buffer = ListBuffer.empty[S]
          iterator.foreach(string => buffer += f(string))
          buffer.toVector
        }
      case _ => MetaDecode.EmptyMetaDecode
    }

  def map[K, V](
      fk: MetaDecode.Make[K],
      fv: MetaDecode.Make[V]
  ): MetaDecode.Make[Map[K, V]] =
    fk.productTransform(fv) {
      case (StringValueMetaDecode(readK), StringValueMetaDecode(readV)) =>
        StringMapMetaDecode[Map[K, V]](map =>
          map.map { case (k, v) => (readK(k), readV(v)) }.toMap
        )
      case (StringValueMetaDecode(readK), StringListMetaDecode(readV)) =>
        StringListMapMetaDecode[Map[K, V]](map =>
          map.map { case (k, v) => (readK(k), readV(v)) }.toMap
        )
      case _ => EmptyMetaDecode
    }

  def union[S](
      first: Alt[MetaDecode.Make, S, _],
      rest: Vector[Alt[MetaDecode.Make, S, _]]
  )(total: S => Alt.WithValue[MetaDecode.Make, S, _]): MetaDecode.Make[S] =
    MetaDecode.Make.empty

  def enumeration[A](
      to: A => (String, Int),
      fromString: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): MetaDecode.Make[A] =
    Hinted[MetaDecode].onHintOpt[IntEnum, A] {
      case Some(_) =>
        MetaDecode
          .from(
            s"Enum[${fromString.keySet.mkString(",")}]"
          )(s => fromOrdinal.get(s.toInt))
      case None =>
        MetaDecode
          .from(
            s"Enum[${fromString.keySet.mkString(",")}]"
          )(fromString.get)
    }

  def suspend[A](f: Lazy[MetaDecode.Make[A]]): MetaDecode.Make[A] =
    MetaDecode.Make.empty

  private case class FieldDecode(
      fieldName: String,
      binding: HttpBinding,
      update: (Metadata, PutField) => Unit
  )
  def struct[S](
      fields: Vector[Field[MetaDecode.Make, S, _]]
  )(const: Vector[Any] => S): MetaDecode.Make[S] =
    Hinted[MetaDecode].onHintOpt[InputOutput, S] { maybeInputOutput =>
      val reservedQueries =
        fields
          .map(f =>
            HttpBinding.fromHints(f.label, f.instance.hints, maybeInputOutput)
          )
          .collect { case Some(HttpBinding.QueryBinding(query)) =>
            query
          }
          .toSet

      def decodeField[A](
          field: Field[MetaDecode.Make, S, A]
      ): Option[FieldDecode] = {
        val instance = field.instance
        val label = field.label
        val hints = instance.hints
        HttpBinding.fromHints(label, hints, maybeInputOutput).map { binding =>
          val decoder: MetaDecode[_] =
            instance.addHints(Hints(binding)).get
          val update = decoder
            .updateMetadata(binding, label, field.isOptional, reservedQueries)
          FieldDecode(label, binding, update)
        }
      }

      val fieldUpdates: Vector[FieldDecode] =
        fields.map(f => decodeField(f)).collect { case Some(fieldUpdate) =>
          fieldUpdate
        }

      val partial = { (metadata: Metadata) =>
        val buffer = MMap.empty[String, Any]
        val putField: PutField = new PutField {
          def putRequired(fieldName: String, value: Any): Unit =
            buffer += (fieldName -> value)

          def putSome(fieldName: String, value: Any): Unit =
            buffer += (fieldName -> value)

          def putNone(fieldName: String): Unit = ()
        }
        var currentFieldName: String = null
        var currentBinding: HttpBinding = null
        try {
          fieldUpdates.foreach { case FieldDecode(fieldName, binding, update) =>
            currentFieldName = fieldName
            currentBinding = binding
            update(metadata, putField)
          }
          Right(buffer)
        } catch {
          case e: MetadataError => Left(e)
          case MetaDecode.MetaDecodeError(const) =>
            Left(const(currentFieldName, currentBinding))
          case ConstraintError(_, message) =>
            Left(
              MetadataError.FailedConstraint(
                currentFieldName,
                currentBinding,
                message
              )
            )
        }
      }

      val total =
        if (fieldUpdates.size < fields.size) None
        else
          Some { (metadata: Metadata) =>
            val buffer = Vector.newBuilder[Any]
            val putField: PutField = new PutField {
              def putRequired(fieldName: String, value: Any): Unit =
                buffer += value

              def putSome(fieldName: String, value: Any): Unit =
                buffer += Some(value)

              def putNone(fieldName: String): Unit = buffer += None
            }

            var currentFieldName: String = null
            var currentBinding: HttpBinding = null
            try {
              fieldUpdates.foreach {
                case FieldDecode(fieldName, binding, update) =>
                  currentFieldName = fieldName
                  currentBinding = binding
                  update(metadata, putField)
              }
              Right(const(buffer.result()))
            } catch {
              case e: MetadataError => Left(e)
              case MetaDecode.MetaDecodeError(const) =>
                Left(const(currentFieldName, currentBinding))
              case ConstraintError(_, message) =>
                Left(
                  MetadataError.FailedConstraint(
                    currentFieldName,
                    currentBinding,
                    message
                  )
                )
            }
          }

      StructureMetaDecode(partial, total)
    }

  def bijection[A, B](fa: MetaDecode.Make[A], to: A => B, from: B => A) =
    fa.map(to)

  def surjection[A, B](
      fa: MetaDecode.Make[A],
      to: Refinement[A, B],
      from: B => A
  ): MetaDecode.Make[B] =
    fa.emap(to.asFunction)

  def withHints[A](
      fa: MetaDecode.Make[A],
      hints: Hints
  ): MetaDecode.Make[A] =
    fa.addHints(hints)
}
