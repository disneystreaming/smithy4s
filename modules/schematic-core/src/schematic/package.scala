/*
 *  Copyright 2021 Disney Streaming
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

package object schematic extends ExistentialsPlatformCompat {

  type Static[A] = A with Static.Tag

  type NumericSchematic[F[_]] =
    short.Schematic[F]
      with int.Schematic[F]
      with long.Schematic[F]
      with double.Schematic[F]
      with float.Schematic[F]

  type PrimitiveSchematic[F[_]] =
    NumericSchematic[F]
      with string.Schematic[F]
      with boolean.Schematic[F]
      with bytes.Schematic[F]

  type CollectionSchematic[F[_]] =
    list.Schematic[F]
      with set.Schematic[F]
      with vector.Schematic[F]
      with map.Schematic[F]

  type Repr[A] = String
  type PrettyRepr[A] = Printer

  type Wrapped[F[_], G[_], A] = F[G[A]]

}
