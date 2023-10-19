/*
 *  Copyright 2021-2023 Disney Streaming
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

import smithy4s.example._
import smithy4s.kinds.PolyFunction
import munit._
import scala.util.{Failure, Success, Try}
class TransformationSpec() extends FunSuite {

  test("transform method can be called with poly functions") {
    object stub extends Weather.Default[Option](None) {
      override def getCurrentTime(): Option[GetCurrentTimeOutput] = None
    }

    case object Empty extends Throwable

    // Not ascribing the type to verify type inference in the following statement.
    val transformed = stub.transform(new PolyFunction[Option, Try] {
      def apply[A](fa: Option[A]): Try[A] = fa match {
        case Some(value) => scala.util.Success(value)
        case None        => scala.util.Failure(Empty)
      }
    })
    expect(transformed.getCurrentTime().isFailure)
  }

  test("errors can be surfaced") {
    object kvStoreTry extends KVStore[Try] {
      def delete(key: String): Try[Unit] = Success(())
      def put(key: String, value: String): Try[Unit] = Success(())
      def get(key: String): Failure[Value] =
        Failure(KeyNotFoundError(s"Key $key wasn't found"))
    }

    val toEither: Transformation.SurfaceError[Try, Either] =
      new Transformation.SurfaceError[Try, Either] {
        def apply[E, A](
            value: Try[A],
            catcher: Throwable => Option[E]
        ): Either[E, A] = value match {
          case Success(value) => Right(value)
          case Failure(error) =>
            catcher(error) match {
              case None    => throw error
              case Some(e) => Left(e)
            }
        }
      }

    val kvStoreEither: KVStore.ErrorAware[Either] =
      kvStoreTry.transform(toEither)

    expect.same(
      kvStoreEither.get("foo"): Either[KVStore.GetError, Value],
      Left(
        KVStore.GetError.KeyNotFoundErrorCase(
          KeyNotFoundError(s"Key foo wasn't found")
        )
      )
    )
  }

  test("errors can be absorbed") {
    object kvStoreEither extends KVStore.ErrorAware[Either] {
      def delete(key: String): Either[KVStore.DeleteError, Unit] = Right(())
      def put(key: String, value: String): Either[Nothing, Unit] = Right(())
      def get(key: String): Either[KVStore.GetError, Value] =
        Left(
          KVStore.GetError.KeyNotFoundErrorCase(
            KeyNotFoundError(s"Key $key wasn't found")
          )
        )
    }

    val toTry: Transformation.AbsorbError[Either, Try] =
      new Transformation.AbsorbError[Either, Try] {
        def apply[E, A](
            value: Either[E, A],
            thrower: E => Throwable
        ): Try[A] = value match {
          case Left(error)  => Failure(thrower(error))
          case Right(value) => Success(value)
        }
      }

    val kvStoreTry: KVStore[Try] = kvStoreEither.transform(toTry)

    expect.same(
      kvStoreTry.get("foo"): Try[Value],
      Failure(
        KeyNotFoundError(s"Key foo wasn't found")
      )
    )

  }

}
