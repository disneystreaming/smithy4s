package smithy4s

import scala.deriving._
import Hints.Binding.StaticBinding

// BINCOMPAT FOR 0.18 START
trait BindingPlatform extends Mirror.Sum {
  type MirroredMonoType = Hints.Binding
}
trait StaticBindingPlatform extends Mirror.Product {
  type MirroredMonoType = StaticBinding[?]
  def fromProduct(p: Product): MirroredMonoType = {
    val (key, value) = p.asInstanceOf[(ShapeTag[Any], Any)]
    StaticBinding(key, value).asInstanceOf[MirroredMonoType]
  }

  @annotation.targetName("unapply")
  def unapply_old[A](
      binding: StaticBinding[A]
  ): StaticBinding[A] =
    binding

}
// BINCOMPAT FOR 0.18 END
