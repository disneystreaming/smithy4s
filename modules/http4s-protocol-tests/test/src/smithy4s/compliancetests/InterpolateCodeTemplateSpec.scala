/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.compliancetests

import software.amazon.smithy.utils.SimpleCodeWriter
import weaver.FunSuite

/**
  * JVM-only test that proves our pure Scala `interpolateCodeTemplate`
  * produces identical results to Smithy's `SimpleCodeWriter.format` for
  * all `$$key:L` (literal formatter) patterns used in httpMalformedRequestTests
  * parameter templates.
  *
  * SimpleCodeWriter's default formatters:
  *   - `:L` (literal) — calls `String.valueOf` on the value
  *   - `:S` (string)  — Java-string-escapes the literal
  *
  * Smithy test parameter templates exclusively use `$$key:L`.
  */
object InterpolateCodeTemplateSpec extends FunSuite {

  private def codeWriterFormat(
      template: String,
      context: Map[String, String]
  ): String = {
    val writer = new SimpleCodeWriter()
    context.foreach { case (k, v) => writer.putContext(k, v) }
    writer.format(template, Array.empty[Object]: _*)
  }

  private def scalaInterpolate(
      template: String,
      context: Map[String, String]
  ): String = {
    internals.interpolateCodeTemplate(template, context)
  }

  private def compareFormats(
      name: String,
      template: String,
      context: Map[String, String]
  ): Unit = {
    test(name) {
      val expected = codeWriterFormat(template, context)
      val actual = scalaInterpolate(template, context)
      expect.eql(expected, actual)
    }
  }

  // --- $key:L (literal formatter) ---

  compareFormats(
    "literal: single key",
    "/malformed/$value:L",
    Map("value" -> "12345")
  )

  compareFormats(
    "literal: multiple keys",
    "$key:L=$val:L",
    Map("key" -> "Content-Type", "val" -> "application/json")
  )

  compareFormats(
    "literal: URI path",
    "/MalformedBoolean/true/$value:L",
    Map("value" -> "False")
  )

  compareFormats(
    "literal: JSON body",
    """{"booleanInBody" : $value:L}""",
    Map("value" -> "\"true\"")
  )

  compareFormats(
    "literal: empty value",
    "prefix$value:Lsuffix",
    Map("value" -> "")
  )

  // --- No interpolation needed ---

  compareFormats(
    "no variables: plain string",
    "/static/path/to/resource",
    Map.empty
  )

  // --- Multiple occurrences of same key ---

  compareFormats(
    "same key used twice",
    "$val:L and $val:L",
    Map("val" -> "repeated")
  )

  // --- Realistic malformed test patterns ---

  compareFormats(
    "realistic: boolean body template",
    """{"booleanInBody" : $value:L}""",
    Map("value" -> "\"True\"")
  )

  compareFormats(
    "realistic: query param",
    "booleanInQuery=$value:L",
    Map("value" -> "True")
  )

  compareFormats(
    "realistic: path segment",
    "/MalformedTimestampPathDefault/$value:L",
    Map("value" -> "1996-12-19T16:39:57+00")
  )

  compareFormats(
    "realistic: header value",
    "$value:L",
    Map("value" -> "0")
  )

  compareFormats(
    "realistic: complex JSON body",
    """{"integerInBody" : $value:L}""",
    Map("value" -> "1.0")
  )

  compareFormats(
    "realistic: regex body assertion",
    """Expected an integer value, but found $value:L""",
    Map("value" -> "1.0")
  )

  // --- Edge cases ---

  compareFormats(
    "edge: key that is prefix of another key",
    "$val:L and $value:L",
    Map("val" -> "short", "value" -> "long")
  )

  compareFormats(
    "edge: value containing dollar sign",
    "$key:L",
    Map("key" -> "price$100")
  )

  compareFormats(
    "edge: value containing colon-L",
    "$key:L",
    Map("key" -> "some:L thing")
  )

  compareFormats(
    "edge: unicode value",
    "$key:L",
    Map("key" -> "\u00e9\u00e8\u00ea")
  )

  compareFormats(
    "edge: multiple keys in JSON",
    """{"a": $a:L, "b": $b:L, "c": $c:L}""",
    Map("a" -> "1", "b" -> "\"hello\"", "c" -> "true")
  )

  compareFormats(
    "edge: key appearing in value of another key",
    "$first:L $second:L",
    Map("first" -> "$second:L", "second" -> "real")
  )

}
