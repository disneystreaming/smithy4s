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

package smithy4s.json

import cats.Show
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalacheck.Gen
import smithy4s.Document
import smithy4s.Schema
import smithy4s.scalacheck.DynData
import munit._
import org.scalacheck.Prop
import Prop._

class DocumentPropertyTests() extends FunSuite with ScalaCheckSuite {

  val genSchemaData: Gen[(Schema[DynData], Any)] = for {
    schema <- Gen.const(
      Schema.float.asInstanceOf[Schema[DynData]]
    ) // SchemaGenerator.genSchema(1, 1)
    data <- schema.compile(smithy4s.scalacheck.SchemaVisitorGen)
  } yield (schema -> data)

  implicit val schemaAndDataShow: Show[(Schema[DynData], Any)] =
    Show.fromToString

  implicit val documentCodec: JsonCodec[Document] =
    Json.jsoniter.fromSchema(Schema.document)

  property(
    "Going through json directly or via the adt give the same results"
  ) {
    // We're randomly generating a schema, and using it
    // to randomly generate compliant data, and
    // asserting roundtrip there.
    forAll(genSchemaData) { schemaAndData =>
      val (schema, data) = schemaAndData
      implicit val codec: JsonCodec[Any] = Json.jsoniter.fromSchema(schema)
      val decoder = Document.Decoder.fromSchema(schema)
      val encoder = Document.Encoder.fromSchema(schema)
      val schemaStr =
        schema.compile(smithy4s.internals.SchemaDescriptionDetailed)
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

      val e1 = Prop(dataDirect.exists(_ == data))
      val e2 = Prop(dataFromDocument.exists(_ == data))
      dataFromDocument.left.foreach(_.printStackTrace())
      (e1 && e2).label(
        s"""|schema: $schemaStr
            |data: $data
            |jsonFromDocument: $jsonFromDocument
            |jsonDirect: $jsonDirect
            |documentFromJson: $documentFromJson
            |dataFromDocument: $dataFromDocument
            |""".stripMargin
      )
    }

  }

}
