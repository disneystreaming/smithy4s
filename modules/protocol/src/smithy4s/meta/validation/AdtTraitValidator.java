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

import smithy4s.meta.AdtTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unions marked with the adt trait must have at least one member. Also, the
 * structures that the union targets must NOT be used within any other union.
 *
 * Also checks that the structures targeted are not empty (they must have at
 * least one member).
 */
public final class AdtTraitValidator extends AbstractValidator {

	@Override
	public List<ValidationEvent> validate(Model model) {
		return model.getShapesWithTrait(AdtTrait.class).stream().flatMap(adtShape -> {
			Set<Shape> adtMemberShapes = adtShape.asUnionShape()
					.orElseThrow(() -> new RuntimeException("adt trait may only be used on union shapes")).members()
					.stream().map(mem -> model.expectShape(mem.getTarget())).collect(Collectors.toSet());
			List<Shape> nonStructures = adtMemberShapes.stream().filter(mem -> !mem.asStructureShape().isPresent())
					.collect(Collectors.toList());
			if (!nonStructures.isEmpty()) {
				String nonStruct = nonStructures.stream().map(s -> s.getId().toString())
						.collect(Collectors.joining(", "));
				return Stream.of(error(adtShape,
						String.format(
								"Some members of %s were found to target non-structure shapes. Instead they target %s",
								adtShape.getId(), nonStruct)));
			}
			if (adtMemberShapes.isEmpty()) {
				return Stream.of(error(adtShape, "unions with the adt trait must contain at least one member"));
			} else {
				return AdtValidatorCommon.getReferenceEvents(model, adtMemberShapes, adtShape);
			}
		}).collect(Collectors.toList());
	}
}
