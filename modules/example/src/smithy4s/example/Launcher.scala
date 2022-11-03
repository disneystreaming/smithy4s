/*
 * Copyright 2021 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txthttps://disneystreaming.github.io/TOST-1.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smithy4s.example

import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

object Launcher extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val (host, port) = hostAndPort(args)

    val serverRes = for {
      impl <- Resource.eval(ObjectServiceImpl.makeIO)
      docs = smithy4s.http4s.swagger.docs[IO](ObjectService)
      service <- SimpleRestJsonBuilder.routes(impl).resource
      app = (docs <+> service).orNotFound
      server <- EmberServerBuilder.default[IO].withHost(host).withPort(port).withHttpApp(app).build
    } yield ()

    val status = IO.delay(
      println(
        s"Running server on http://${host}:${port}\nPress ENTER to terminate"
      )
    )

    val waitForInput = IO.delay(
      Console.in.readLine()
    )

    serverRes.use(_ => status *> waitForInput).as(ExitCode.Success)
  }

  def hostAndPort(args: List[String]) = {
    val host = Host.fromString(args.headOption.getOrElse("localhost")).get
    val port = Port.fromInt(args.drop(1).headOption.map(_.toInt).getOrElse(8080)).get

    (host, port)
  }
}

import smithy4s.example.ObjectServiceImpl._
class ObjectServiceImpl(store: Ref[IO, Map[Key, Value]])
    extends ObjectService[IO] {
  override def getObject(
      key: ObjectKey,
      bucketName: BucketName
  ): IO[GetObjectOutput] = store.get.map(_.get(key -> bucketName)).map {
    maybeData => GetObjectOutput(maybeData.map(_.size).map(ObjectSize(_)).getOrElse(ObjectSize(0)), maybeData)
  }
  override def putObject(
      key: ObjectKey,
      bucketName: BucketName,
      data: String,
      foo: Option[LowHigh],
      someValue: Option[SomeValue]
  ): IO[Unit] = {
    store.update { r =>
      r.updated(key -> bucketName, data)
    }
  }
}

object ObjectServiceImpl {
  type Key = (ObjectKey, BucketName)
  type Value = String

  def makeIO = {
    Ref
      .of[IO, Map[Key, Value]](Map.empty)
      .map(store => new ObjectServiceImpl(store))
  }
}
