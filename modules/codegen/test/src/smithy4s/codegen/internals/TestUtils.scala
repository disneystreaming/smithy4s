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

package smithy4s.codegen.internals

import munit.Assertions
import munit.Location
import software.amazon.smithy.model.Model

object TestUtils {

  def generateScalaCode(smithySpec: String): List[String] = {
    val model = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("foo.smithy", smithySpec)
      .assemble()
      .unwrap()
    CodegenImpl.generate(model, None, None).map(_._2.content)
  }

  def runTest(
      smithySpec: String,
      expectedScalaCode: String
  )(implicit
      loc: Location
  ): Unit = {
    val scalaResults = generateScalaCode(smithySpec)
    Assertions.assertEquals(scalaResults, List(expectedScalaCode))
  }

  /**
    * Finds a section starting with a given string and extracts a code section from it (based on indents)
    * Removes empty lines too.
    * It then asserts on expected output
    */
  def assertContainsSection(fileContent: String, startsWith: String)(
      expectedSection: String
  )(implicit loc: Location) = {
    val lines = fileContent.lines.filter(_.trim.nonEmpty).toList.zipWithIndex
    val lineMatches = lines.filter { case (l, _) =>
      l.trim.startsWith(startsWith)
    }
    lineMatches match {
      case (line, index) :: Nil =>
        val indent = line.takeWhile(_ == ' ')
        val validLineStartPatterns =
          List(" ", ")", "}").map(s => s"$indent$s").toSet
        val fromMatch = lines.drop(index + 1).map(_._1)
        val sectionTail = fromMatch
          .takeWhile(l => validLineStartPatterns.exists(l.startsWith))
          .map(_.drop(indent.size))
        val section = (line.drop(indent.size) :: sectionTail).mkString("\n")
        Assertions.assertEquals(section, expectedSection)
      case _ :: _ :: _ =>
        Assertions.fail("Multiple lines match the code section pattern")
      case Nil => Assertions.fail("No line matches the code section pattern")
    }
  }

}
