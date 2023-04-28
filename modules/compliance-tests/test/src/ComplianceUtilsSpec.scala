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

package smithy4s.compliancetests

import smithy4s.compliancetests.internals.parseList
import weaverimpl._
object ComplianceUtilsSpec extends FunSuite {

  test(
    "when the string starts with comma ,the comma is not used as delimiter  "
  ) {
    val s = ",a,b,c"
    expect(parseList(s) == List("a", "b", "c"))
  }

  test("string ends with comma , comma is not used as delimiter  ") {
    val s = "a,b,c,"
    expect(parseList(s) == List("a", "b", "c"))
  }

  test(
    "string has two commas in a row , comma is not used as delimiter and empty string is not added to the list"
  ) {
    val s = "a,,b,c"
    expect(parseList(s) == List("a", "b", "c"))
  }
  test(
    "comma between quotes is not used as delimiter and comma is included to the entry"
  ) {
    val s = "a,\",b\" ,c"
    expect(parseList(s) == List("a", "\",b\" ", "c"))
  }

  test("more complex scenario with quotes and commas") {
    val s = "a,\",b\",c,,,\"d,e,f\",g"
    expect(parseList(s) == List("a", "\",b\"", "c", "\"d,e,f\"", "g"))
  }

}
