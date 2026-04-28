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

package smithy4s.interopcats

import cats.data.EitherT
import smithy4s.example.hello.HelloWorldService
import smithy4s.example.hello.HelloWorldServiceGen
import smithy4s.example.hello.HelloWorldServiceOperation.HelloError
import smithy4s.example.hello.Greeting
import smithy4s.example.hello.GenericServerError
import weaver.FunSuite

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object EitherTTransformationsSpec extends FunSuite {

  type EitherTTry[E, A] = EitherT[Try, E, A]

  test("absorbError lifts a service from EitherT[F, *, *] to F") {
    val errorAware: HelloWorldServiceGen.ErrorAware[EitherTTry] =
      new HelloWorldServiceGen.ErrorAware[EitherTTry] {
        def hello(
            name: String,
            town: Option[String] = None
        ): EitherT[Try, HelloError, Greeting] =
          if (name.isEmpty)
            EitherT.leftT(
              HelloError.genericServerError(
                GenericServerError(Some("name was empty"))
              )
            )
          else
            EitherT.rightT(Greeting(s"hello, $name"))
      }

    val functor: HelloWorldService[Try] =
      errorAware.transform(EitherTTransformations.absorbError[Try])

    expect.same(functor.hello("world"), Success(Greeting("hello, world")))
    expect.same(
      functor.hello(""),
      Failure(GenericServerError(Some("name was empty")))
    )
  }

  test(
    "absorbError preserves underlying F failures from EitherT-wrapped services"
  ) {
    val underlying = new RuntimeException("boom")

    val errorAware: HelloWorldServiceGen.ErrorAware[EitherTTry] =
      new HelloWorldServiceGen.ErrorAware[EitherTTry] {
        def hello(
            name: String,
            town: Option[String] = None
        ): EitherT[Try, HelloError, Greeting] =
          EitherT[Try, HelloError, Greeting](Failure(underlying))
      }

    val functor: HelloWorldService[Try] =
      errorAware.transform(EitherTTransformations.absorbError[Try])

    expect.same(functor.hello("anyone"), Failure(underlying))
  }

  test("surfaceError lifts a service from F to EitherT[F, *, *]") {
    val functor: HelloWorldService[Try] =
      new HelloWorldService[Try] {
        def hello(name: String, town: Option[String] = None): Try[Greeting] =
          if (name.isEmpty) Failure(GenericServerError(Some("name was empty")))
          else Success(Greeting(s"hello, $name"))
      }

    val errorAware: HelloWorldServiceGen.ErrorAware[EitherTTry] =
      functor.transform(EitherTTransformations.surfaceError[Try])

    expect.same(
      errorAware.hello("world").value,
      Success(Right(Greeting("hello, world")))
    )
    expect.same(
      errorAware.hello("").value,
      Success(
        Left(
          HelloError.genericServerError(
            GenericServerError(Some("name was empty"))
          )
        )
      )
    )
  }

  test("surfaceError re-raises errors not declared by the service") {
    val unknown = new RuntimeException("unknown")

    val functor: HelloWorldService[Try] =
      new HelloWorldService[Try] {
        def hello(name: String, town: Option[String] = None): Try[Greeting] =
          Failure(unknown)
      }

    val errorAware: HelloWorldServiceGen.ErrorAware[EitherTTry] =
      functor.transform(EitherTTransformations.surfaceError[Try])

    expect.same(errorAware.hello("anyone").value, Failure(unknown))
  }

  test("absorbError andThen surfaceError round-trips a service") {
    val errorAware: HelloWorldServiceGen.ErrorAware[EitherTTry] =
      new HelloWorldServiceGen.ErrorAware[EitherTTry] {
        def hello(
            name: String,
            town: Option[String] = None
        ): EitherT[Try, HelloError, Greeting] =
          if (name.isEmpty)
            EitherT.leftT(
              HelloError.genericServerError(
                GenericServerError(Some("name was empty"))
              )
            )
          else
            EitherT.rightT(Greeting(s"hello, $name"))
      }

    val roundTripped: HelloWorldServiceGen.ErrorAware[EitherTTry] =
      errorAware
        .transform(EitherTTransformations.absorbError[Try])
        .transform(EitherTTransformations.surfaceError[Try])

    expect.same(
      roundTripped.hello("world").value,
      Success(Right(Greeting("hello, world")))
    )
    expect.same(
      roundTripped.hello("").value,
      Success(
        Left(
          HelloError.genericServerError(
            GenericServerError(Some("name was empty"))
          )
        )
      )
    )
  }

}
