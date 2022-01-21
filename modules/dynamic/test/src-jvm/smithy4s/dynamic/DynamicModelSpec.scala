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

package smithy4s.dynamic

import model._
import weaver._
import smithy4s.Document
import smithy4s.Service
import smithy4s.Hints
import schematic.Field

object DynamicModelSpec extends SimpleIOSuite {

  val modelString =
    """|namespace foo
      |
      |service Service {
      |  operations: [Operation]
      |}
      |
      |@readonly
      |@http(method: "GET", uri: "/{name}")
      |operation Operation {
      |  input: Input,
      |  output: Output
      |}
      |
      |structure Input {
      |  @httpLabel
      |  @required
      |  name: String
      |}
      |
      |structure Output {
      |  greeting: String,
      |  someFloat: Float
      |}
      |""".stripMargin.trim

  val expected: Model = {
    Model(
      smithy = Some("1.0"),
      shapes = Map(
        IdRef("foo#Service") -> Shape.ServiceCase(
          ServiceShape(operations =
            Some(List(MemberShape(IdRef("foo#Operation"))))
          )
        ),
        IdRef("foo#Operation") -> Shape.OperationCase(
          OperationShape(
            input = Some(MemberShape(IdRef("foo#Input"))),
            output = Some(MemberShape(IdRef("foo#Output"))),
            traits = Some(
              Map(
                IdRef("smithy.api#http") -> Document.obj(
                  "method" -> Document.fromString("GET"),
                  "uri" -> Document.fromString("/{name}"),
                  "code" -> Document.fromInt(200)
                ),
                IdRef("smithy.api#readonly") -> Document.obj()
              )
            )
          )
        ),
        IdRef("foo#Input") -> Shape.StructureCase(
          StructureShape(
            members = Some(
              Map(
                "name" -> MemberShape(
                  IdRef("smithy.api#String"),
                  traits = Some(
                    Map(
                      IdRef("smithy.api#httpLabel") -> Document.obj(),
                      IdRef("smithy.api#required") -> Document.obj()
                    )
                  )
                )
              )
            )
          )
        ),
        IdRef("foo#Output") -> Shape.StructureCase(
          StructureShape(
            members = Some(
              Map(
                "greeting" -> MemberShape(
                  IdRef("smithy.api#String")
                ),
                "someFloat" -> MemberShape(
                  IdRef("smithy.api#Float")
                )
              )
            )
          )
        )
      )
    )
  }

  test("Decode json representation of models") {
    Utils
      .parse(modelString)
      .map(obtained => expect.same(obtained, expected))
  }

  test(
    "Compilation does not recurse infinitely in the case of recursive structures".only
  ) {
    val modelString =
      """|namespace foo
         |
         |structure Foo {
         |  foo: Foo
         |}
         |""".stripMargin

    Utils.compile(modelString).as(success)
  }

  object Interpreter {
    type ToFieldNames[A] = () => List[String]

    object GetFieldNames extends smithy4s.StubSchematic[ToFieldNames] {
      def default[A]: ToFieldNames[A] = () => Nil

      override def withHints[A](
          fa: ToFieldNames[A],
          hints: Hints
      ): ToFieldNames[A] = fa

      override def genericStruct[S](
          fields: Vector[Field[ToFieldNames, S, _]]
      )(const: Vector[Any] => S): ToFieldNames[S] = () =>
        fields.flatMap { f =>
          f.label :: f.instance()
        }.toList

      override def suspend[A](f: => ToFieldNames[A]): ToFieldNames[A] =
        () => f()

      // these will be needed later but are irrelevant for now
      // override def union[S](
      //     first: Alt[ToFieldNames, S, _],
      //     rest: Vector[Alt[ToFieldNames, S, _]]
      // )(total: S => Alt.WithValue[ToFieldNames, S, _]): ToFieldNames[S] =
      //   () =>
      //     first.label :: first.instance() ::: rest.flatMap { a =>
      //       a.label :: a.instance()
      //     }.toList

      // override def list[S](fs: ToFieldNames[S]): ToFieldNames[List[S]] = fs
      // override def vector[S](fs: ToFieldNames[S]): ToFieldNames[Vector[S]] = fs
      // override def map[K, V](
      //     fk: ToFieldNames[K],
      //     fv: ToFieldNames[V]
      // ): ToFieldNames[Map[K, V]] = () => fk() ++ fv()
      // override def bijection[A, B](
      //     f: ToFieldNames[A],
      //     to: A => B,
      //     from: B => A
      // ): ToFieldNames[B] = f

      // override def set[S](fs: ToFieldNames[S]): ToFieldNames[Set[S]] = fs

    }

    def toFieldNames[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
        svc: Service[Alg, Op]
    ): List[String] =
      svc.endpoints.flatMap { endpoint =>
        endpoint.input.compile(GetFieldNames)() ++
          endpoint.output.compile(GetFieldNames)()
      }
  }

  pureTest(
    "Extract field names from all structures in a service's endpoints"
  ) {
    val svc = Utils.compile(expected).allServices.head.service

    //  NoSuchElementException: key not found: smithy.api#String
    val result = Interpreter.toFieldNames(svc)
    assert(result == List("name", "greeting", "someFloat"))
  }
}
