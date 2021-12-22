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

package smithy4s.http.json

import cats.Show
import cats.effect.IO
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalacheck.Gen
import schematic.scalacheck.DynData
import smithy4s.Document
import smithy4s.Schema
import weaver._
import weaver.scalacheck._

object DocumentPropertyTests extends SimpleIOSuite with Checkers {

  override val maxParallelism = 1

  override val checkConfig: CheckConfig =
    super.checkConfig.copy(perPropertyParallelism = 1)

  val genSchemaData: Gen[(Schema[DynData], Any)] = for {
    schema <- Gen.const(
      smithy4s.syntax.float.asInstanceOf[Schema[DynData]]
    ) // SchemaGenerator.genSchema(1, 1)
    data <- schema.compile(smithy4s.scalacheck.SchematicGen)
  } yield (schema -> data)

  implicit val schemaAndDataShow: Show[(Schema[DynData], Any)] =
    Show.fromToString

  implicit val documentCodec: JCodec[Document] =
    smithy4s.syntax.document.compile(schematicJCodec).get

  loggedTest(
    "Going through json directly or via the adt give the same results"
  ) { log =>
    // We're randomly generating a schema, and using it
    // to randomly generate compliant data, and
    // asserting roundtrip there.
    forall(genSchemaData) { schemaAndData =>
      val (schema, data) = schemaAndData
      implicit val codec: JCodec[Any] =
        schema.compile(schematicJCodec).get
      val decoder = Document.Decoder.fromSchema(schema)
      val encoder = Document.Encoder.fromSchema(schema)
      val schemaStr = schema.compile(smithy4s.SchematicRepr)
      val document = encoder.encode(data)
      val jsonFromDocument = writeToString(document)
      val jsonDirect = writeToString(data)

      val config = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)

      val dataDirect =
        scala.util.Try(readFromString[Any](jsonDirect, config)).toEither

      val documentFromJson = scala.util
        .Try(readFromString[Document](jsonFromDocument, config))
        .toEither
      val dataFromDocument = documentFromJson.flatMap(decoder.decode)

      val e = expect.all(
        dataDirect.exists(_ == data),
        dataFromDocument.exists(_ == data)
      )
      dataFromDocument.left.foreach(_.printStackTrace())
      if (e.run.isInvalid) {
        for {
          _ <- log.debug("wassup ?")
          _ <- log.debug("schema: " + schemaStr)
          _ <- log.debug("data: " + data)
          _ <- log.debug("jsonFromDocument: " + jsonFromDocument)
          _ <- log.debug("jsonDirect: + " + jsonDirect)
          _ <- log.debug("documentFromJson: " + documentFromJson)
          _ <- log.debug("dataFromDocument: " + dataFromDocument)
        } yield e
      } else IO.pure(e)
    }

  }

}
