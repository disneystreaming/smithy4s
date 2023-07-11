package smithy4s
package dynamic

import software.amazon.smithy.model.{Model => SModel}
import smithy4s.schema.Schema
import smithy.api.{Documentation => Doc}

class DynamicMemberHintsSpec() extends DummyIO.Suite {

  def loadFrom(modelString: String, shapeId: ShapeId): Schema[_] = {
    val model = SModel
      .assembler()
      .addUnparsedModel("foo.smithy", modelString)
      .assemble()
      .unwrap()

    val dsi = DynamicSchemaIndex
      .loadModel(model)
      .getOrElse(sys.error("Couldn't load model"))

    dsi
      .getSchema(shapeId)
      .getOrElse(sys.error(s"Couldn't find model shape $shapeId"))
  }

  test("Map member traits are compiled to member hints") {
    val model = """|namespace foo
                   |
                   |
                   |/// map
                   |map MyMap {
                   |  /// key
                   |  key: String
                   |  /// value
                   |  value: String
                   |}
                   |""".stripMargin
    loadFrom(model, ShapeId("foo", "MyMap")) match {
      case m: Schema.MapSchema[_, _] =>
        expect.same(m.hints.get(Doc), Some(Doc("map")))
        expect.same(m.key.hints.memberHints.get(Doc), Some(Doc("key")))
        expect.same(m.value.hints.memberHints.get(Doc), Some(Doc("value")))
      case _ => fail("expected map schema")
    }
  }

  test("List member traits are compiled to member hints") {
    val model = """|namespace foo
                   |
                   |/// list
                   |list MyList {
                   |  /// member
                   |  member: String
                   |}
                   |""".stripMargin
    loadFrom(model, ShapeId("foo", "MyList")) match {
      case c: Schema.CollectionSchema[_, _] =>
        expect.same(c.hints.get(Doc), Some(Doc("list")))
        expect.same(c.member.hints.memberHints.get(Doc), Some(Doc("member")))
      case _ => fail("expected list schema")
    }
  }

  test("Structure member traits are compiled to member hints") {
    val model = """|namespace foo
                   |
                   |structure Foo {
                   |  /// member
                   |  member: String
                   |}
                   |""".stripMargin
    loadFrom(model, ShapeId("foo", "Foo")) match {
      case s: Schema.StructSchema[_] =>
        val hint =
          s.fields.find(_.label == "member").flatMap(_.memberHints.get(Doc))
        expect.same(hint, Some(Doc("member")))
      case _ => fail("expected struct schema")
    }
  }

  test("Union member traits are compiled to member hints") {
    val model = """|namespace foo
                   |
                   |union Foo {
                   |  /// member
                   |  member: String
                   |}
                   |""".stripMargin
    loadFrom(model, ShapeId("foo", "Foo")) match {
      case s: Schema.UnionSchema[_] =>
        val hint =
          s.alternatives
            .find(_.label == "member")
            .flatMap(_.memberHints.get(Doc))
        expect.same(hint, Some(Doc("member")))
      case _ => fail("expected union schema")
    }
  }

}
