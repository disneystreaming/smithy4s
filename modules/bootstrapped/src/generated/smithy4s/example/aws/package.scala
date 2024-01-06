package smithy4s.example

package object aws {
  type MyThing[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[MyThingGen, F]
  val MyThing = MyThingGen


}