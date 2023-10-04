/*
 *  Copyright 2021-2023 Disney Streaming
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

import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.Trait

import java.lang.{Class => jClass}
import scala.reflect.ClassTag

private[internals] class TraitExtractor[T <: Trait](implicit T: ClassTag[T]) {
  def apply(s: Shape): Option[T] =
    s.getTrait[T](T.runtimeClass.asInstanceOf[jClass[T]]).asScala

  def unapply(s: Shape): Option[T] = apply(s)
}
