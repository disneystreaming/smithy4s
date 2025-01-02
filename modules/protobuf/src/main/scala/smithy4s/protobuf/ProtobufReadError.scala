/*
 *  Copyright 2021-2025 Disney Streaming
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

import smithy4s.ShapeId
import smithy4s.Hint

sealed trait ProtobufReadError extends Throwable

// scalafmt: { maxColumn = 120}
object ProtobufReadError {
  final case class Other private (cause: Throwable) extends ProtobufReadError {
    override def getMessage() = "Failed to decode protobuf message"
    override def getCause(): Throwable = cause
  }

  object Other {
    def apply(cause: Throwable): Other = new Other(cause)
    def unapply(error: Other): Some[Other] = Some(error)
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
  }

}
