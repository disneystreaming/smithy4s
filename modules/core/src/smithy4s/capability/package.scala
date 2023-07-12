package smithy4s

package object capability {
  type Wrapped[F[_], G[_], A] = F[G[A]]
}
