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

package smithy4s
package dynamic

import munit.Location
import software.amazon.smithy.model.{Model => SModel}
import smithy4s.schema.{Field, Primitive, Schema}
import smithy.api.Default

class DynamicFieldModifierSpec() extends DummyIO.Suite {

  def loadFieldInfo(
      modelString: String,
      shapeId: ShapeId = ShapeId("foo", "Foo"),
      fieldName: String = "bar"
  ): Field[_, _] = {
    val model = SModel
      .assembler()
      .addUnparsedModel("foo.smithy", modelString)
      .addImport(
        // Alloy nullable
        getClass().getClassLoader.getResource("META-INF/smithy/presence.smithy")
      )
      .assemble()
      .unwrap()

    val dsi = DynamicSchemaIndex
      .loadModel(model)

    val shapeSchema = dsi
      .getSchema(shapeId)
      .getOrElse(sys.error(s"Couldn't find model shape $shapeId"))
    shapeSchema match {
      case m: Schema.StructSchema[_] =>
        m.fields
          .find(_.label == fieldName)
          .getOrElse(fail(s"No field named $fieldName present in structure"))
      case _ => fail("expected structure schema")
    }
  }


  test("Optional field results in optional schema") {
    val model = """|namespace foo
                   |
                   |use smithy.api#required
                   |
                   |structure Foo {
                   |  bar: String
                   |}
                   |""".stripMargin
    val field = loadFieldInfo(model)
    checkRequired(field, false)
    checkNullable(field, false)
    field.schema match {
      case Schema.OptionSchema(s) => expectPrimitiveStringSchema(s)
      case other                  => fail(s"Expected option schema, got $other")
    }
  }

  test("Required field results in regular schema with required hint") {
    val model =
      """|namespace foo
         |
         |use smithy.api#required
         |
         |structure Foo {
         |  @required
         |  bar: String
         |}
         |""".stripMargin
    val field = loadFieldInfo(model)
    checkRequired(field, true)
    checkNullable(field, false)
    expectPrimitiveStringSchema(field.schema)
  }

  test("Nullable field results in nullable hint and optional nullable schema") {
    val model = """|$version: "2"
                   |
                   |namespace foo
                   |
                   |use smithy.api#required
                   |use alloy#nullable
                   |
                   |structure Foo {
                   |  @nullable
                   |  bar: String
                   |}
                   |""".stripMargin
    val field = loadFieldInfo(model)
    checkRequired(field, false)
    checkNullable(field, true)
    field.schema match {
      case Schema.OptionSchema(Nullable.Schema(s)) =>
        expectPrimitiveStringSchema(s)
      case other => fail(s"Expected optional nullable schema, got $other")
    }
  }

  test(
    "Nullable field with value default parses default and schema correctly"
  ) {
    val model =
      """|$version: "2"
         |
         |namespace foo
         |
         |use alloy#nullable
         |
         |structure Foo {
         |  @nullable
         |  bar: String = "foo"
         |}
         |""".stripMargin
    val field = loadFieldInfo(model)
    checkRequired(field, false)
    checkNullable(field, true)
    checkDefault(field, Document.DString("foo"))
  }

  test(
    "Nullable field with null default parses default and schema correctly"
  ) {
    val model =
      """|$version: "2"
         |
         |namespace foo
         |
         |use smithy.api#default
         |use alloy#nullable
         |
         |structure Foo {
         |  @nullable
         |  @default
         |  bar: String
         |}
         |""".stripMargin
    val field = loadFieldInfo(model)
    checkRequired(field, false)
    checkNullable(field, true)
    checkDefault(field, Document.DNull)
  }

  test(
    "Required nullable field results in nullable hint and nullable schema"
  ) {
    val model = """|$version: "2"
                   |
                   |namespace foo
                   |
                   |use smithy.api#required
                   |use alloy#nullable
                   |
                   |structure Foo {
                   |  @nullable
                   |  @required
                   |  bar: String
                   |}
                   |""".stripMargin
    val field = loadFieldInfo(model)
    checkRequired(field, true)
    checkNullable(field, true)
    field.schema match {
      case Nullable.Schema(s) => expectPrimitiveStringSchema(s)
      case other              => fail(s"Expected nullable schema, got $other")
    }
  }

  private def expectPrimitiveStringSchema[A](schema: Schema[A]): Unit =
    schema match {
      case Schema.PrimitiveSchema(_, _, Primitive.PString) => ()
      case other => fail(s"Expected primitive string schema, got $other")
    }

  private def checkDefault[A, B](
      field: Field[A, B],
      expectedDefault: Document
  )(implicit loc: Location): Unit = {
    val defaultHint = field.memberHints
      .get(smithy.api.Default)
      .getOrElse(fail("No default hint present."))
    expect.same(defaultHint, Default(expectedDefault))
  }
  private def checkRequired[A, B](
      field: Field[A, B],
      isRequired: Boolean
  )(implicit loc: Location): Unit = expect(field.memberHints.has(smithy.api.Required) == isRequired)
  private def checkNullable[A, B](
      field: Field[A, B],
      isNullable: Boolean
  )(implicit loc: Location): Unit = expect(field.memberHints.has(alloy.Nullable) == isNullable)
}
