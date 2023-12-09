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

import munit._
import SmithyToIR._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId

final class SmithyToIRSpec extends FunSuite {

  test("prettifyName: sdkId takes precedence") {
    assertEquals(
      prettifyName(Some("Example"), "unused"),
      "Example"
    )
  }
  test("prettifyName: shapeName is used as a fallback") {
    assertEquals(
      prettifyName(None, "Example"),
      "Example"
    )
  }

  test("prettifyName removes whitespace in sdkId") {
    assertEquals(
      prettifyName(Some("QuickDB \t\nStreams"), "unused"),
      "QuickDBStreams"
    )
  }

  // Not a feature, just verifying the name is unaffected
  test("prettifyName ignores whitespace in shape name") {
    assertEquals(
      prettifyName(None, "This Has Spaces"),
      "This Has Spaces"
    )
  }

  locally {
    val modelText = """
    namespace smithy4s.example.traits

    /// A trait with no recursive references to itself
    @trait
    @anotherNormalTrait
    structure nonRecursiveTrait {}

    @trait
    structure anotherNormalTrait {}

    /// A trait with a direct recursive reference
    @trait
    @directRecursiveTrait
    structure directRecursiveTrait {}

    /// A trait with an indirect recursive reference
    @trait
    @indirectRecursiveTrait1
    structure indirectRecursiveTrait0 {}

    // trait that completes the recursion of the above, also recursive because it has to loop back through the above
    @trait
    @indirectRecursiveTrait0
    structure indirectRecursiveTrait1 {}

    /// A trait with no recursion in itself, referencing recursive traits
    @trait
    @nonRecursiveTrait
    @directRecursiveTrait
    @indirectRecursiveTrait0
    @indirectRecursiveTrait1
    structure nonRecursiveTraitReferencingOthers {}

    /// A trait that has direct recursive references to itself via members.
    @trait
    structure directRecursiveViaMembersTrait {
        @directRecursiveViaMembersTrait
        member: String
    }

    /// A trait that has indirect recursive references to it via members.
    @trait
    structure indirectRecursiveViaMembersTrait {
        member: RecursiveMember
    }

    @indirectRecursiveViaMembersTrait
    structure RecursiveMember {}

    @trait
    @indirect1
    structure indirect0 {}

    @trait
    @indirect2
    structure indirect1 {}

    @trait
    @indirect0
    structure indirect2 {}

    @trait
    @traitWithMember(m: "foo")
    structure recursiveViaTraitMember {}

    @trait
    structure traitWithMember {
        m: M
    }

    @recursiveViaTraitMember
    string M
    """

    val model = makeModel(modelText)
    val index = new SmithyToIR.RecursionIndex(model)

    def doTest(
        from: String,
        to: String,
        expected: Boolean,
        mods: TestOptions => TestOptions = identity
    )(implicit
        loc: Location
    ) =
      test(
        mods(s"$from is ${if (expected) "" else "not "}recursive with $to")
      ) {
        assertEquals(
          isRecursiveTrait(
            model = model,
            index = index,
            fromName = from,
            toName = to
          ),
          expected
        )(loc, implicitly[Boolean <:< Boolean])
      }

    // format: off
    doTest("nonRecursiveTrait", "anotherNormalTrait", false)
    doTest("directRecursiveTrait", "directRecursiveTrait", true)
    doTest("indirectRecursiveTrait0", "indirectRecursiveTrait1", true)
    doTest("indirectRecursiveTrait1", "indirectRecursiveTrait0", true)
    doTest("nonRecursiveTraitReferencingOthers", "directRecursiveTrait", false)
    doTest("nonRecursiveTraitReferencingOthers", "indirectRecursiveTrait0", false)
    doTest("nonRecursiveTraitReferencingOthers", "indirectRecursiveTrait1", false)
    doTest("indirect0", "indirect1", true)
    doTest("indirect1", "indirect2", true)
    doTest("indirect2", "indirect0", true)
    doTest("recursiveViaTraitMember", "traitWithMember", true)
    // format: on
  }

  def isRecursiveTrait(
      model: Model,
      index: SmithyToIR.RecursionIndex,
      fromName: String,
      toName: String
  ): Boolean = {
    val ns = "smithy4s.example.traits"
    val from = model.expectShape(ShapeId.fromParts(ns, fromName))
    val to = model.expectShape(ShapeId.fromParts(ns, toName))

    index.isRecursiveTraitOf(from, to)
  }

  def makeModel(modelText: String): Model = {
    Model
      .assembler()
      .addUnparsedModel("model.smithy", modelText)
      .assemble()
      .unwrap()

  }
}
