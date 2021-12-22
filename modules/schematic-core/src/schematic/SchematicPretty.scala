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

object SchematicPretty extends SchematicPretty

trait SchematicPretty
    extends Schematic.stdlib.Mixin[PrettyRepr]
    with struct.GenericAritySchematic[PrettyRepr] {

  def short: Printer = Printer.raw("short")

  def int: Printer = Printer.raw("int")

  def long: Printer = Printer.raw("long")

  def double: Printer = Printer.raw("double")

  def float: Printer = Printer.raw("float")

  def bigint: Printer = Printer.raw("bigint")

  def bigdecimal: Printer = Printer.raw("bigdecimal")

  def string: Printer = Printer.raw("string")

  def boolean: Printer = Printer.raw("boolean")

  def byte: Printer = Printer.raw("byte")

  def bytes: Printer = Printer.raw("byteArray")

  def timestamp: Printer = Printer.raw("timestamp")

  def instant: Printer = Printer.raw("instant")

  def localDate: Printer = Printer.raw("localDate")

  def offsetDateTime: Printer = Printer.raw("offsetDateTime")

  def list[S](fs: Printer): Printer = Printer { nesting =>
    s"list[${fs.print(nesting)}]"
  }

  def set[S](fs: Printer): Printer = Printer { nesting =>
    s"set[${fs.print(nesting)}]"
  }

  def vector[S](fs: Printer): Printer = Printer { nesting =>
    s"vector[${fs.print(nesting)}]"
  }

  def uuid: Printer = Printer.raw("uuid")

  def map[K, V](fk: Printer, fv: Printer): Printer = Printer { nesting =>
    s"map[${fk.print(nesting)}, ${fv.print(nesting)}]"
  }

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): Printer = {
    val labels = fromName.keySet.mkString(", ")
    Printer.raw(s"enumeration[$labels]")
  }

  def genericStruct[S](
      list: Vector[Field[PrettyRepr, S, _]]
  )(f: Vector[Any] => S): Printer =
    Printer { nesting =>
      val nn = nesting + 2
      val fields = list
        .map {
          case f if f.isOptional => s"${f.label}: ${f.instance.print(nn)}?"
          case f                 => s"${f.label}: ${f.instance.print(nn)}"
        }
        .map((" " * nn) + _)
        .mkString(",\n")

      s"struct${list.size} {\n${fields}\n${" " * nesting}}"
    }

  def union[S](
      first: Alt[PrettyRepr, S, _],
      rest: Vector[Alt[PrettyRepr, S, _]]
  )(total: S => Alt.WithValue[PrettyRepr, S, _]): Printer =
    Printer.apply { nesting =>
      val nn = nesting + 2
      val alts = (rest
        .:+(first))
        .map { alt =>
          s"${alt.label}: ${alt.instance.print(nn)}"
        }
        .map((" " * nn) + _)
        .mkString(",\n")

      s"union${rest.size + 1} {\n${alts}\n${" " * nesting}}"
    }

  def suspend[A](f: => Printer): Printer = f

  def bijection[A, B](f: Printer, to: A => B, from: B => A) = f

  override def unit: Printer = Printer.raw("unit")

}
