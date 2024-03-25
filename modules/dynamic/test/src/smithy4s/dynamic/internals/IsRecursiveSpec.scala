package smithy4s.dynamic.internals

import smithy4s.dynamic.internals.recursiveVertices

class IsRecursiveSpec() extends munit.FunSuite {

  test("detect recursive vertices - example 1") {
    val map = Map[Int, Set[Int]](
      1 -> Set(2),
      2 -> Set(3, 4, 5),
      3 -> Set(4),
      5 -> Set(6, 7),
      6 -> Set(2),
      7 -> Set(2)
    )
    val result = recursiveVertices(map)
    assertEquals(result, Set(2, 5, 6, 7))
  }

  test("detect recursive vertices - example 2") {
    val map = Map(
      2 -> Set(1, 3),
      1 -> Set(3),
      3 -> Set(4, 5),
      4 -> Set(2),
      5 -> Set(2)
    )
    val result = recursiveVertices(map)
    assertEquals(result, Set(1, 2, 3, 4, 5))
  }

  test("detect recursive vertices - example 3") {
    val map = Map(
      2 -> Set(1),
      1 -> Set(3),
      3 -> Set(1, 2, 3, 4, 5),
      4 -> Set(3, 5),
      5 -> Set(3, 4)
    )
    val result = recursiveVertices(map)
    assertEquals(result, Set(1, 2, 3, 4, 5))
  }

}
