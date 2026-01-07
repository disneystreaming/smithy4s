package smithy4s.data

import smithy4s.data.Tree.{Branch, Leaf}

object TreeSpec extends weaver.FunSuite {
  def simpleTree: Tree[Int] = Branch(1,Vector(Leaf(2), Branch(3, Vector(Leaf(4)))))
  def fold2List[A](t:Tree[A]): List[A] = t.foldRight(List.empty[A])((a, xs) => xs.appended(a))
  def postOrder2List[A](t: Tree[A]): Vector[A] = t.postOrder[Vector[A]]((a, xs) => xs.flatten.appended(a))

  test("leaf -  size") {
    expect(Leaf(1).size == 1)
  }
  test("branch - size") {
    expect(Branch(1,Vector.empty).size == 1) &&
    expect(simpleTree.size == 4)
  }
  test("leaf - eq") {
    expect(Leaf(1) == Leaf(1))
  }
  test("branch - eq") {
    val t1 = simpleTree
    val t2 = simpleTree
    expect(Branch(1, Vector.empty) == Branch(1, Vector.empty)) &&
    expect(t1 == t2 && System.identityHashCode(t1) != System.identityHashCode(t2))
  }
  test("leaf - map identity") {
    val t1 = Leaf(1)
    val t2 = t1.map(identity)
    expect(t1 == t2 && System.identityHashCode(t1) != System.identityHashCode(t2))
  }
  test("branch - map identity") {
    val t1 = Branch(1, Vector.empty)
    val t2 = t1.map(identity)
    expect(t1 == t2 && System.identityHashCode(t1) != System.identityHashCode(t2))
  }
  test("leaf - foldRight") {
    expect(fold2List(Leaf(1)) == List(1))
  }
  test("branch - foldRight") {
    val l1 = fold2List(Branch(1,Vector.empty))
    val l2 = fold2List(simpleTree)
    expect(l1 == List(1)) &&
      expect(l2 == List(1,3,4,2))
  }
  test("leaf - postOrder") {
    expect(postOrder2List(Leaf(1)) == Vector(1))
  }
  test("branch - postOrder") {
    val l1 = postOrder2List(Branch(1, Vector.empty))
    val l2 = postOrder2List(simpleTree)
    expect(l1 == List(1)) &&
      expect(clue(l2) == List(2,4,3,1))
  }
  test("leaf - immediate children") {
    expect(Leaf(1).immediateChildren.isEmpty)
  }
  test("branch - immediate children") {
    expect(Branch(1,Vector.empty).immediateChildren.isEmpty) &&
      expect(simpleTree.immediateChildren == List(2,3))
  }
}
