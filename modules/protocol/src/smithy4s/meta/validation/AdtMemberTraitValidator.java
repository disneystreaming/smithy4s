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

package smithy4s.meta.validation;

import smithy4s.meta.AdtMemberTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All structures annotated with `@adtMember(SomeUnion)` are targeted in EXACTLY
 * ONE place: as a member of the union they reference in their idRef (SomeUnion
 * in this case)
 *
 * Also checks that structure is not empty (must have at least one member)
 */
public final class AdtMemberTraitValidator extends AbstractValidator {

	@Override
	public List<ValidationEvent> validate(Model model) {
		Set<Shape> adtMemberShapes = model.getShapesWithTrait(AdtMemberTrait.class);
		Map<ShapeId, List<Shape>> grouped = adtMemberShapes.stream()
				.collect(Collectors.groupingBy(mem -> mem.expectTrait(AdtMemberTrait.class).getValue()));
		return grouped.entrySet().stream().flatMap(entry -> {
			return AdtValidatorCommon.getReferenceEvents(model, entry.getValue(), model.expectShape(entry.getKey()));
		}).collect(Collectors.toList());
	}
}
