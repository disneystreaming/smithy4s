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

import scala.quoted._

object Static {

  trait Tag

  inline def apply[A](inline value: A): Static[A] = ${ staticImpl('value) }

  def staticImpl[T](using Quotes, Type[T])(value: Expr[T]): Expr[Static[T]] = {
    import quotes.reflect.report
    val enclosingOwner = nonMacroOwner(quotes.reflect.Symbol.spliceOwner)
    enclosingOwner match {
      case valSym if valSym.isValDef => // good
      case _ =>
        report.errorAndAbort(
          "schematic.Static can only be used to initalise vals or lazy vals"
        )
    }
    var owner = enclosingOwner.owner

    while (!owner.isNoSymbol) {
      if (!owner.flags.is(quotes.reflect.Flags.Module)) {
        report.errorAndAbort(
          "schematic.Static can only be used to initialise static values (contained by however-many layers of static objects)"
        )
      } else owner = owner.owner
    }

    '{ ($value).asInstanceOf[T with Tag] }
  }

  private def nonMacroOwner(using
      Quotes
  )(owner: quotes.reflect.Symbol): quotes.reflect.Symbol =
    findOwner(
      owner,
      owner0 => {
        owner0.flags.is(quotes.reflect.Flags.Macro) && getName(
          owner0
        ) == "macro"
      }
    )

  private def findOwner(using Quotes)(
      owner: quotes.reflect.Symbol,
      skipIf: quotes.reflect.Symbol => Boolean
  ): quotes.reflect.Symbol = {
    var owner0 = owner
    while (skipIf(owner0)) owner0 = owner0.owner
    owner0
  }

  private def getName(using Quotes)(s: quotes.reflect.Symbol) = {
    s.name.trim
      .stripSuffix("$") // meh
  }

}

abstract class StaticImpl {}
