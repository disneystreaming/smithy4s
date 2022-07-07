package smithy4s
package internals

import smithy4s.example._
import munit._

class SchemaDescriptionSpec() extends FunSuite {
  def simple[A](s: Schema[A]): String = s.compile(SchemaDescription)
  def detailed[A](s: Schema[A]): String =
    s.compile(SchemaDescriptionDetailed)(Set.empty)._2

  test("simple") {
    assertEquals(simple(SomeInt.schema), "Int")
    assertEquals(simple(SomeSet.schema), "Set")
    assertEquals(simple(SomeList.schema), "List")

    assertEquals(simple(BlobBody.schema), "Structure")
    assertEquals(simple(IntList.schema), "Structure")
  }

  test("detailed") {
    assertEquals(detailed(SomeInt.schema), "Bijection { Int }")
    assertEquals(detailed(SomeSet.schema), "Bijection { Set[String] }")
    assertEquals(detailed(SomeList.schema), "Bijection { List[String] }")

    assertEquals(detailed(BlobBody.schema), "Structure { blob: Bytes }") //
    assertEquals(detailed(IntList.schema), "Structure") //
  }
}
