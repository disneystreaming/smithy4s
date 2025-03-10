/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.meta.validation;

import smithy4s.meta.AdtTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Severity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.selector.Selector;

/**
 * All the members of an ADT union must be structures.
 * Also, each such structure can only be referenced once in the whole model (from said union).
 */
public final class AdtTraitValidator extends AbstractValidator {

  private final Selector adtTargetedMemberSelector = Selector.parse(
    // First part of the selector: we define an allAdtMembers variable which contains all structures directly referenced by @adt unions.
    // Second part: we go back to the root of the model and select all shapes that directly refer to any of the above.
    //
    // The :root selector is necessary so that the variable correctly captures shapes starting from the root of the model, instead of the use-site of the variable.
    //
    // $foo and ${foo} are defining and using a variable, respectively (https://smithy.io/2.0/spec/selectors.html#variables)
    String.format("$allAdtMembers(:root([trait|%s] > member > structure))", AdtTrait.ID.toString()) +
                  ":test(> member > :in(${allAdtMembers}))"
  );

  private class Reference implements Comparable<Reference>{
    Shape from;
    Shape to;

    Reference(Shape from, Shape to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public int compareTo(Reference o) {
      return this.from.getId().compareTo(o.from.getId());
    }
  }

  @Override
  public List<ValidationEvent> validate(Model model) {

    Stream<ValidationEvent> nonStructTargets = model.getUnionShapesWithTrait(AdtTrait.class).stream()
      .filter(union -> !union.getAllMembers().values().stream().allMatch(mem -> model.expectShape(mem.getTarget()).isStructureShape()))
      .map(union -> error(union, "All members of an adt union must be structures"));

    Stream<ValidationEvent> dupes = adtTargetedMemberSelector.shapes(model).flatMap(parent -> {
      return parent.getAllMembers().values().stream().map(mem -> new Reference(parent, model.expectShape(mem.getTarget())));
    })
    .collect(Collectors.groupingBy(ref -> ref.to))
    .entrySet().stream()
    .filter(entry -> entry.getValue().size() > 1)
    .map(targetWithDuplicateParents -> {

      String targets =
        targetWithDuplicateParents.getValue().stream()
          .collect(Collectors.groupingBy(ref -> ref.from))
          .entrySet().stream()
          .map(entry -> {
            String countSuffix =  entry.getValue().size() == 1 ? "" : String.format(" (%d times)", entry.getValue().size());
            return entry.getKey().getId().toString() + countSuffix;
          })
          .sorted()
          .collect(Collectors.joining(", "));

      return error(targetWithDuplicateParents.getKey(), "This shape can only be referenced once and from one adt union, but it's referenced from " + targets);
    });

    return Stream.concat(nonStructTargets, dupes).collect(Collectors.toList());
  }

}
