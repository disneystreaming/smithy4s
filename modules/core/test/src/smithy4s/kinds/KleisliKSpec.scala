package smithy4s.kinds

import smithy4s.{Lazy, ~>}
import weaver.SimpleIOSuite
import weaver.scalacheck._
import smithy4s.capability.MonadThrowLike
import smithy4s.kinds.KleisliK.{FGA, Unwrapped}

import scala.util.Try

object KleisliKSpec extends SimpleIOSuite with Checkers with KleisliKSpecTestInstances {
  val optionToListOfTry: KleisliK[List,Option,Try] = KleisliK(new (Option ~> FGA[List,Try, *]) {
    override def apply[A](fa: Option[A]): FGA[List, Try, A] = fa.toList.map(Try(_))
  })

  val lazyToListOfLazy: KleisliK[List, Lazy, Option] = KleisliK( new (Lazy ~> FGA[List, Option, *]){
    override def apply[A](fa: Lazy[A]): FGA[List, Option, A] = List(Option(fa.value))
  })

  val tryToListOfVector: KleisliK[List,Try,Vector] = KleisliK( new (Try ~> FGA[List,Vector, *]) {
    override def apply[A](fa: Try[A]): FGA[List, Vector, A] = List(fa.toOption.toVector)
  })

  val tryToKleisliUnwrapped: Try ~> Unwrapped[List, Option, Vector, *] = new (Try ~> Unwrapped[List, Option, Vector, *]) {
    override def apply[A](fa: Try[A]): Unwrapped[List, Option, Vector, A] = (v1: Option[A]) => List(fa.toOption.toVector ++ v1.toVector)
  }

  test("compose") {
    forall { (n: Int) =>
      val composed = optionToListOfTry.compose(lazyToListOfLazy)
      val result = composed.run(Lazy(n))
      expect(result == List(Try(n)))
    }
  }
  test("flatMap unwrapped") {
    forall { (n: Int) =>
      val flattened = optionToListOfTry.flatMap(tryToKleisliUnwrapped)
      val result = flattened.run(Some(n))
      expect(result == List(Vector(n, n)))
    }
  }
}


trait KleisliKSpecTestInstances {
  implicit val listMonadThrowLike: MonadThrowLike[List] = new MonadThrowLike[List] {
    override def flatMap[A, B](fa: List[A])(f: A => List[B]): List[B] = fa.flatMap(f)

    override def raiseError[A](e: Throwable): List[A] = ???

    override def handleErrorWith[A](fa: List[A])(f: Throwable => List[A]): List[A] = ???

    override def pure[A](a: A): List[A] = List(a)

    override def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)

    override def zipMapAll[A](seq: IndexedSeq[List[Any]])(f: IndexedSeq[Any] => A): List[A] = ???
  }

}