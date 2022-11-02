/*
 *  Copyright 2021-2022 Disney Streaming
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

import munit._
import scala.util.{Failure, Success, Try}
class TransformationSmokeSpec() extends FunSuite {

  test("transform method can be called with poly functions") {
    object stub extends Weather[Option] {
      def getCurrentTime(): Option[GetCurrentTimeOutput] = None
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

  test("errors can be lifted") {
    object kvStoreTry extends KVStore[Try] {
      def delete(key: String): Try[Unit] = Success(())
      def put(key: String, value: String): Try[Unit] = Success(())
      def get(key: String): Failure[Value] =
        Failure(KeyNotFoundError(s"Key $key wasn't found"))
    }

    val toEither = new Transformation.ErrorLift[Try, Either] {
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

    val kvStoreEither: KVStore.WithError[Either] =
      kvStoreTry.transform(toEither)

    expect.same(
      kvStoreEither.get("foo"): Either[KVStoreGen.GetError, Value],
      Left(
        KVStoreGen.GetError.KeyNotFoundErrorCase(
          KeyNotFoundError(s"Key foo wasn't found")
        )
      )
    )

    // case object Empty extends Throwable

    // // Not ascribing the type to verify type inference in the following statement.
    // val transformed = stub.transform(new PolyFunction[Option, Try] {
    //   def apply[A](fa: Option[A]): Try[A] = fa match {
    //     case Some(value) => scala.util.Success(value)
    //     case None        => scala.util.Failure(Empty)
    //   }
    // })
    // expect(transformed.getCurrentTime().isFailure)
  }

}
