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

package smithy4s.codegen

class Writer(val sb: StringBuilder) {
  private var indent = 0

  def newline = sb.append("\n")
  def line(s: String): Unit = sb.append(" " * indent).append(s).append("\n")
  def lines(s: String*): Unit = s.foreach(line)
  def block(s: String, self: Boolean = false)(b: => Unit): Unit = {
    sb.append(" " * indent)
      .append(s)
      .append(" {")
      .append(if (self) " self =>" else "")
      .append("\n")
    indent = indent + 2
    b
    indent = indent - 2
    sb.append(" " * indent + "}\n")
  }
  def args(s: String, trailingNewLine: Boolean = true)(
      a: Seq[String]*
  ): Unit = {
    sb.append(" " * indent).append(s)
    a.foreach { seq =>
      sb.append("(\n")
      indent = indent + 2
      val lastIndex = seq.size - 1
      seq.zipWithIndex.foreach {
        case (str, `lastIndex`) =>
          sb.append(" " * indent).append(str).append("\n")
        case (str, _) =>
          sb.append(" " * indent).append(str).append(",\n")
      }
      indent = indent - 2
      sb.append(" " * indent + ")")
    }
    if (trailingNewLine) sb.append("\n")
  }
  def argsBlock(s: String)(args: Seq[String])(block: => Unit) = {
    this.args(s, trailingNewLine = false)(args)
    sb.append("{\n")
    indent = indent + 2
    block
    indent = indent - 2
    sb.append(" " * indent + "}\n")
  }
}

object Writer {
  def apply(): Writer = new Writer(new StringBuilder())
}
