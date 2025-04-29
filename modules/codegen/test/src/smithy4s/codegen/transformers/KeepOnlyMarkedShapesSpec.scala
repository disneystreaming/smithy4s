/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.shapes.ShapeId

import scala.jdk.CollectionConverters._

final class KeepOnlyMarkedShapesSpec extends munit.FunSuite {
  import smithy4s.codegen.internals.TestUtils._

  val dummyServices =
    """
      |$version: "2"
      |
      |namespace com.amazonaws.dummy.service
      |
      |service Service1 {
      |  operations: [GetLong, GetStringLength, GetStringChecked]
      |}
      |// will be marked as @only
      |operation GetLong {
      | output: com.amazonaws.dummy#Long,
      |}
      |
      |operation GetStringChecked {
      | output: com.amazonaws.dummy#StringChecked,
      |}
      |
      |// will be marked as @only
      |operation GetStringLength {
      | output: com.amazonaws.dummy#StringLength,
      |}
      |
      |// will be marked as @only
      |operation GetBool {
      | output := {
      |     myBool: com.amazonaws.dummy#MyBool
      |  }
      |}
      |
      |operation GetByte {
      | output := {
      |     myDogDoesntBarkIt: com.amazonaws.dummy#Byte,
      |     myOtherThing: com.amazonaws.dummy#MyOtherThing
      |  }
      |}
      |
      |service Service2 {
      |  operations: [GetLong, GetBool]
      |}
      |service Service3 {
      |  operations: [GetByte]
      |}
      |""".stripMargin

  test(
    "Keeps operations marked as @only and removes other operations from a service"
  ) {
    val dummy =
      """
        |$version: "2"
        |
        |namespace com.amazonaws.dummy
        |
        |structure Long {}
        |
        |structure StringChecked {
        | myThing: MyThing
        | myOtherThing: MyOtherThing
        |}
        |
        |structure MyThing {}
        |string MyOtherThing
        |byte Byte
        |boolean MyBool
        |
        |structure StringLength {}
        |""".stripMargin

    val myCode =
      """
        |$version: "2"
        |
        |namespace my.code
        |
        |use smithy4s.meta#only
        |
        |apply com.amazonaws.dummy.service#GetLong @only
        |apply com.amazonaws.dummy.service#GetStringLength @only
        |apply com.amazonaws.dummy.service#GetBool @only
        |""".stripMargin

    val originalModel = loadAndValidateModel(dummyServices, dummy, myCode)
    val shapeIdsBefore = originalModel.getShapeIds().asScala.toSet
    val transformed = new KeepOnlyMarkedShapes().transform(
      TransformContext.builder().model(originalModel).build()
    )
    val removedShapes =
      (shapeIdsBefore -- transformed.getShapeIds().asScala.toSet)
        .map(_.toString)

    assertEquals(
      removedShapes,
      Set(
        "com.amazonaws.dummy#StringChecked$myOtherThing",
        "com.amazonaws.dummy#StringChecked",
        "com.amazonaws.dummy#StringChecked$myThing",
        "com.amazonaws.dummy.service#GetStringChecked",
        "com.amazonaws.dummy#MyThing",
        "com.amazonaws.dummy#StringChecked"
      )
    )

    def service(name: String) =
      transformed
        .getServiceShapes()
        .asScala
        .find(shape =>
          shape.toShapeId == ShapeId
            .fromRelative("com.amazonaws.dummy.service", name)
        )
        .get

    // Service1 has GetLong and GetStringLength marked as @only,
    // which triggers GetStringChecked removal
    assertEquals(
      service("Service1").getOperations().asScala.toSet,
      Set(
        ShapeId.from("com.amazonaws.dummy.service#GetLong"),
        ShapeId.from("com.amazonaws.dummy.service#GetStringLength")
      )
    )

    // Service2 has both of its operations marked as @only and
    // so both of them are kept
    assertEquals(
      service("Service2").getOperations().asScala.toSet,
      Set(
        ShapeId.from("com.amazonaws.dummy.service#GetBool"),
        ShapeId.from("com.amazonaws.dummy.service#GetLong")
      )
    )

    // Service3 doesn't have any operations annotated with @only
    // but it affected by other services through operation sharing.
    // GetStringChecked from this service is removed by virtue of
    // being part of Service1, which uses @only annotations
    assertEquals(
      service("Service3").getOperations().asScala.toSet,
      Set(
        ShapeId.from("com.amazonaws.dummy.service#GetByte")
      )
    )

  }

}
