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

  /** Key is the name (like my.package.name.SomeFile) and the value is the contents of that file */
  def generateScalaCode(smithySpec: String): Map[String, String] = {
    val model = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("foo.smithy", smithySpec)
      .assemble()
      .unwrap()
    generateScalaCode(model)
  }

  def generateScalaCode(model: Model): Map[String, String] = {
    CodegenImpl
      .generate(model, None, None)
      .map { case (_, result) =>
        s"${result.namespace}.${result.name}" -> result.content
      }
      .toMap
  }

  def runTest(
      smithySpec: String,
      expectedScalaCode: String
  )(implicit
      loc: Location
  ): Unit = {
    val scalaResults = generateScalaCode(smithySpec).values.toList
    Assertions.assertEquals(
      scalaResults.map(_.trim()),
      List(expectedScalaCode.trim())
    )
  }

  /**
    * Finds a section starting with a given string and extracts a code section from it (based on expected line count)
    * Removes empty lines too.
    * It then asserts on expected output
    */
  def assertContainsSection(fileContent: String, startsWith: String)(
      expectedSection: String
  )(implicit loc: Location) = {
    val lines =
      fileContent.linesIterator.filter(_.trim.nonEmpty).zipWithIndex.toList
    val lineMatches = lines.filter { case (l, _) =>
      l.trim.startsWith(startsWith)
    }
    lineMatches match {
      case (line, index) :: Nil =>
        val indent = line.takeWhile(_ == ' ')
        val expectedLineCount =
          expectedSection.split(System.lineSeparator()).size
        val section = lines
          .drop(index)
          .take(expectedLineCount)
          .collect { case (line, _) =>
            line.drop(indent.size)
          }
          .mkString(System.lineSeparator())
        Assertions.assertEquals(section, expectedSection)
      case _ :: _ :: _ =>
        Assertions.fail("Multiple lines match the code section pattern")
      case Nil => Assertions.fail("No line matches the code section pattern")
    }
  }

  def loadModel(namespaces: String*): Model = {
    val assembler = Model
      .assembler()
      .disableValidation()
      .discoverModels()

    namespaces
      .foldLeft(assembler) { case (a, model) =>
        a.addUnparsedModel(s"test-${model.hashCode}.smithy", model)
      }
      .assemble()
      .unwrap()
  }

}
