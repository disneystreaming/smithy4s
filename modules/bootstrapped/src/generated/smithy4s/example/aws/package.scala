package smithy4s.example

package object aws {
  type MyThing[F[_]] = smithy4s.kinds.FunctorAlgebra[MyThingGen, F]
  val MyThing = MyThingGen


}