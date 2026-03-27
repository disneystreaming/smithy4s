package smithy4s.data

import smithy4s.capability.Covariant
import smithy4s.data.Tree.{Branch, Leaf}

/**
 * A Scala encoding of a rose tree, a tree data structure with a variable and unbounded number of branches per node.
 * @see https://en.wikipedia.org/wiki/Rose_tree
 */
sealed trait Tree[+A] {
  def map[B](f: A => B): Tree[B] = this match {
    case Branch(value, ts) => Tree.Branch(f(value),ts.map(_.map(f)))
    case Leaf(value) => Leaf(f(value))
  }

  final def foldRight[B](z: B)(f: (A, B) => B): B = this match {
    case Leaf(value) => f(value, z)
    case Branch(value, ts) =>
      ts.foldRight(f(value, z))((t, b) => t.foldRight(b)(f))
  }

  final def postOrder[B](f: (A, Vector[B]) =>  B): B = this match {
    case Leaf(value) => f(value, Vector.empty)
    case Branch(value, ts) =>
      val bs = ts.map(_.postOrder(f))
      f(value,bs)
  }

  final def postOrderWithChildren[B](f: (A, Vector[B]) => B): Vector[B] =
    this match {
      case Leaf(value) => Vector(f(value, Vector.empty))
      case Branch(value, ts) =>
        val childSet = for {
          childTres <- ts
          bs <- childTres.postOrderWithChildren(f)
        } yield bs
        childSet.appended(f(value, childSet))
    }

  final def head: A = this match {
    case Branch(value, _) => value
    case Leaf(value)       => value
  }

  final def immediateChildren: Vector[A] = this match {
    case Leaf(_)       => Vector.empty
    case Branch(_, ts) => ts.map(_.head)
  }

  def size: Int = this match {
    case Branch(_, ts) => 1 + ts.map(_.size).sum
    case Leaf(_) => 1
  }
}
object Tree {
  case class Branch[A](value: A, ts: Vector[Tree[A]]) extends Tree[A]
  case class Leaf[A](value: A) extends Tree[A]

  def draw[A](tree: Tree[A])(show: A => String): String = {
    val builder = new StringBuilder()
    def loop(t: Tree[A], indent: String, isLast: Boolean, isRoot: Boolean): Unit = {
      val prefix = if(isRoot) "────" else if(isLast) "└── " else "├── "
      val childIndent = indent + (if (isLast) "    " else "│   ")
      val value = t match {
        case Branch(value, _) => value
        case Leaf(value) => value
      }
      builder.append(indent).append(prefix).append(show(value)).append("\n")
      t match {
        case Branch(_, ts) =>
          for ((child, index) <- ts.zipWithIndex) {
            val childIsLast = index == ts.length - 1
            loop(child, childIndent, childIsLast, false)
          }
        case Leaf(_) =>
      }
    }
    loop(tree,"",true, true)
    builder.toString()
  }

  implicit val treeCovariant: Covariant[Tree] = new Covariant[Tree] {
    override def map[A, B](fa: Tree[A])(f: A => B): Tree[B] = fa.map(f)
  }
}
