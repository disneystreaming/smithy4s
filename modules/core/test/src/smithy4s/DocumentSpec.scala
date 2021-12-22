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

import smithy.api.JsonName
import smithy4s.example.IntList
import weaver._

object DocumentSpec extends FunSuite {

  test("Recursive document codecs should not blow up the stack") {
    val recursive: IntList = IntList(1, Some(IntList(2, Some(IntList(3)))))

    val document = Document.encode(recursive)
    import Document._
    val expectedDocument =
      obj(
        "head" -> fromInt(1),
        "tail" -> obj(
          "head" -> fromInt(2),
          "tail" -> obj("head" -> fromInt(3))
        )
      )

    val roundTripped = Document.decode[IntList](document)

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(recursive))
  }

  import smithy4s.syntax._
  implicit val tupleIntStringSchema: Static[Schema[(Int, String)]] =
    Static {
      val i = int.required[(Int, String)]("int", _._1)
      val s =
        string
          .required[(Int, String)]("string", _._2)
          .withHints(JsonName("_string"))
      struct(i, s)((_, _))
    }

  implicit val eitherIntStringSchema: Static[Schema[Either[Int, String]]] =
    Static {
      val left = int.oneOf[Either[Int, String]]("int", (int: Int) => Left(int))
      val right =
        string
          .oneOf[Either[Int, String]]("string", (str: String) => Right(str))
          .withHints(JsonName("_string"))
      union(left, right) {
        case Left(i)    => left(i)
        case Right(str) => right(str)
      }
    }

  test("jsonName is handled correctly on structures") {
    val intAndString: (Int, String) = (1, "hello")

    val document = Document.encode(intAndString)
    import Document._
    val expectedDocument =
      obj(
        "int" -> fromInt(1),
        "_string" -> fromString("hello")
      )

    val roundTripped = Document.decode[(Int, String)](document)

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(intAndString))
  }

  test("jsonName is handled correctly on unions") {
    val intOrString: Either[Int, String] = Right("hello")

    val document = Document.encode(intOrString)
    import Document._
    val expectedDocument =
      obj(
        "_string" -> fromString("hello")
      )

    val roundTripped = Document.decode[Either[Int, String]](document)

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(intOrString))
  }

}
