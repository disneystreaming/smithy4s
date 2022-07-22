/*
 *  Copyright 2021-2022 Disney Streaming
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

import smithy4s.Lazy
import smithy4s.Service
import smithy4s.Hints
import smithy4s.schema.SchemaVisitor
import Fixtures._
import smithy4s.ShapeId
import smithy4s.schema.SchemaField
import smithy4s.schema.Schema

class FieldsSpec() extends munit.FunSuite {

  test(
    "Extract field names from all structures in a service's endpoints"
  ) {
    val svc = Utils.compile(pizzaModel).allServices.head.service

    //  NoSuchElementException: key not found: smithy.api#String
    val result = Interpreter.toFieldNames(svc)
    assert(result == List("name", "greeting", "someFloat"))
  }

  object Interpreter {
    type ToFieldNames[A] = () => List[String]

    object GetFieldNames extends SchemaVisitor.Default[ToFieldNames] {
      def default[A]: ToFieldNames[A] = () => Nil

      override def struct[S](
          shapeId: ShapeId,
          hints: Hints,
          fields: Vector[SchemaField[S, _]],
          make: IndexedSeq[Any] => S
      ): ToFieldNames[S] = { () =>
        fields.flatMap { f =>
          f.label :: apply(f.instance)()
        }.toList
      }

      override def lazily[A](suspend: Lazy[Schema[A]]): ToFieldNames[A] =
        () => apply(suspend.value)()

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

}
