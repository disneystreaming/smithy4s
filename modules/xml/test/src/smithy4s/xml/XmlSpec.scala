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
