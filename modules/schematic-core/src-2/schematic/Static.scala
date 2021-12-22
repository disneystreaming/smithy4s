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

package schematic

import scala.reflect.macros.blackbox

object Static extends StaticImpl {

  trait Tag

  def apply[A](value: A): Static[A] = macro staticImpl[A]

}

abstract class StaticImpl {
  def staticImpl[T](
      c: blackbox.Context
  )(value: c.Expr[T])(implicit T: c.WeakTypeTag[T]): c.Expr[Static[T]] = {
    import c.universe._
    val enclosingOwner = c.internal.enclosingOwner
    enclosingOwner match {
      case valSym
          if valSym.isTerm && (valSym.asTerm.isVal || valSym.asTerm.isLazy) => // good
      case _ =>
        c.abort(
          c.enclosingPosition,
          "Static can only be used to initalise vals or lazy vals"
        )
    }
    if (!enclosingOwner.isStatic)
      c.abort(
        c.enclosingPosition,
        "Static can only be used to initialise static values (contained by however-many layers of static objects)"
      )
    c.Expr[Static[T]](
      q"(${value.tree}).asInstanceOf[$T with _root_.schematic.Static.Tag]"
    )
  }

}
