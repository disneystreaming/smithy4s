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

package smithy4s
package kinds

trait FunctorK[Alg[_[_]]]{
  def mapK[F[_], G[_]](alg: Alg[F], function: PolyFunction[F, G]): Alg[G]
}
object FunctorK {
  @inline def apply[Alg[_[_]]](implicit ev: FunctorK[Alg]) : FunctorK[Alg] = ev

  implicit def polyfunctionFunctorK[F[_]]: FunctorK[PolyFunction[F, *[_]]] = new FunctorK[PolyFunction[F, *[_]]] {
    def mapK[G[_], H[_]](fa: PolyFunction[F, G], fk: PolyFunction[G, H]): PolyFunction[F, H] = fa.andThen(fk)
  }
}

trait FunctorK2[Alg[_[_, _]]]{
  def mapK2[F[_, _], G[_, _]](alg: Alg[F], function: PolyFunction2[F, G]): Alg[G]
}
object FunctorK2 {
  @inline def apply[Alg[_[_, _]]](implicit ev: FunctorK2[Alg]) : FunctorK2[Alg] = ev

  implicit def polyfunctionFunctorK2[F[_, _]]: FunctorK2[PolyFunction2[F, *[_, _]]] = new FunctorK2[PolyFunction2[F, *[_, _]]] {
    def mapK2[G[_, _], H[_, _]](fa: PolyFunction2[F, G], fk: PolyFunction2[G, H]): PolyFunction2[F, H] = fa.andThen(fk)
  }
}

trait FunctorK5[Alg[_[_, _, _, _, _]]]{
  def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: Alg[F], function: PolyFunction5[F, G]): Alg[G]
}
object FunctorK5 {
  @inline def apply[Alg[_[_, _, _, _, _]]](implicit ev: FunctorK5[Alg]) : FunctorK5[Alg] = ev

  implicit def polyfunctionFunctorK5[F[_, _, _, _, _]]: FunctorK5[PolyFunction5[F, *[_, _, _, _, _]]] = new FunctorK5[PolyFunction5[F, *[_, _, _, _, _]]] {
    def mapK5[G[_, _, _, _, _], H[_, _, _, _, _]](fa: PolyFunction5[F, G], fk: PolyFunction5[G, H]): PolyFunction5[F, H] = fa.andThen(fk)
  }
}
