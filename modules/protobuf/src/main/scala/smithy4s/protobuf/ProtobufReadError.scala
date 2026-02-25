/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.protobuf

import alloy.proto.ProtoIndex
import smithy4s.{ Hint, Hints, Schema, ShapeId }
import smithy4s.schema.Schema.{document, int, string, struct, union}

sealed trait ProtobufReadError extends Throwable

// scalafmt: { maxColumn = 120}
object ProtobufReadError {
  val id: ShapeId = ShapeId("smithy4s.protobuf", "ProtobufReadError")

  implicit val schema: Schema[ProtobufReadError] = {
    val missingRequiredField = MissingRequiredField.schema
      .oneOf[ProtobufReadError]("missingRequiredField")
    val violatedConstraint = ViolatedConstraint.schema
      .oneOf[ProtobufReadError]("violatedConstraint")
    val other = Other.schema
      .oneOf[ProtobufReadError]("other")

    union(missingRequiredField, violatedConstraint, other) {
      case _: MissingRequiredField => 0
      case _: ViolatedConstraint   => 1
      case _: Other                => 2
    }.withId(id)
  }

  final case class Other private (cause: Throwable) extends ProtobufReadError {
    override def getMessage() = cause.getMessage()
    override def getCause(): Throwable = cause

  }

  object Other {
    def apply(cause: Throwable): Other = new Other(cause)
    def unapply(error: Other): Some[Other] = Some(error)
    val schema: Schema[Other] = {
      val message = string.required[Other]("message", _.getMessage)
        .addHints(ProtoIndex(1))
      struct(message)(message => Other(new RuntimeException(message)))
    }
  }

  final case class MissingRequiredField private (
      shapeId: ShapeId,
      fieldName: String,
      index: Int
  ) extends ProtobufReadError
      with scala.util.control.NoStackTrace {

    override def getMessage(): String =
      s"Required message field $fieldName (index $index) of $shapeId was missing"
  }

  object MissingRequiredField {
    def apply(shapeId: ShapeId, fieldName: String, index: Int): MissingRequiredField =
      new MissingRequiredField(shapeId, fieldName, index)
    def unapply(error: MissingRequiredField): Some[MissingRequiredField] = Some(error)
    val schema: Schema[MissingRequiredField] = {
      val shapeId = ShapeId.schema.required[MissingRequiredField]("shapeId", _.shapeId)
        .addHints(ProtoIndex(1))
      val fieldName = string.required[MissingRequiredField]("fieldName", _.fieldName)
        .addHints(ProtoIndex(2))
      val index = int.required[MissingRequiredField]("index", _.index)
        .addHints(ProtoIndex(3))
      struct(shapeId, fieldName, index)(MissingRequiredField.apply)
    }
  }

  final case class ViolatedConstraint private (
      hint: Hint,
      message: String
  ) extends ProtobufReadError
      with scala.util.control.NoStackTrace {

    override def getMessage(): String =
      s"Constraint violated ($hint): $message"
  }

  object ViolatedConstraint {
    def apply(hint: Hint, message: String): ViolatedConstraint = new ViolatedConstraint(hint, message)
    def unapply(error: ViolatedConstraint): Some[ViolatedConstraint] = Some(error)
    private val hintSchema: Schema[Hint] = {
      // Static bindings are rehydrated as DynamicBinding values after round-trip.
      val keyId = ShapeId.schema.required[Hint]("keyId", _.keyId)
        .addHints(ProtoIndex(1))
      val value = document.required[Hint]("value", hint => hint match {
        case static: Hints.Binding.StaticBinding[_] => static.toDynamicBinding.value
        case dynamic: Hints.Binding.DynamicBinding  => dynamic.value
      }).addHints(ProtoIndex(2))
      struct(keyId, value)(Hints.Binding.DynamicBinding.apply)
    }
    val schema: Schema[ViolatedConstraint] = {
      val hint = hintSchema.required[ViolatedConstraint]("hint", _.hint)
        .addHints(ProtoIndex(1))
      val message = string.required[ViolatedConstraint]("message", _.message)
        .addHints(ProtoIndex(2))
      struct(hint, message)(ViolatedConstraint.apply)
    }
  }

}
