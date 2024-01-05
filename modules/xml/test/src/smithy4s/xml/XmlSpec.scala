/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.xml

import weaver._

import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import smithy4s.{ShapeId, Blob}

object XmlSpec extends FunSuite {

  implicit class SchemaOps[A](schema: Schema[A]) {
    def named(name: String) = schema.withId(ShapeId("default", name))
    def x = named("x")
    def n = named("Foo")
  }

  test("roundtrip") {
    implicit val schema: Schema[Int] = int.x
    val xml = "<x>1</x>"
    val decoded = Xml.read[Int](Blob(xml))
    val encoded = Xml.write[Int](1)
    expect.eql(Some(1), decoded.toOption) &&
    expect(Blob(xml).sameBytesAs(encoded))
  }

}
