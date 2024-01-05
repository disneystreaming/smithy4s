/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.codegen.internals

// Binding value and instance to recover from existentials

private[internals] case class WithValue[TC[_], A](value: A, typeclass: TC[A])

private[internals] object WithValue {
  implicit def to[TC[_], A](value: A)(implicit
      instance: TC[A]
  ): WithValue[TC, A] =
    WithValue(value, instance)

  type ToLinesWithValue[A] = WithValue[ToLines, A]
  type ToLineWithValue[A] = WithValue[ToLine, A]
}
