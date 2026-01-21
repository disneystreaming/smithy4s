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

package smithy4s.meta.validation;

import smithy4s.meta.AdtMemberTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.selector.Selector;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * All structures annotated with `@adtMember(SomeUnion)` are targeted in EXACTLY
 * ONE place: as a member of the union they reference in their idRef (SomeUnion
 * in this case)
 *
 * Doesn't check if the container is a union because the idRef on adtMember
 * enforces that.
 */
public final class AdtMemberTraitValidator extends AbstractValidator {

	private final Selector adtTargettingContainersSelector = Selector.parse(
			// All shapes that contain at least one member that is an adtMember.
			String.format(":test(> member > [trait|%s])", AdtMemberTrait.ID.toString()));

	private Boolean targetIsAdtMember(MemberShape shape, Model model) {
		return model.getShape(shape.getTarget()).filter(mem -> mem.hasTrait(AdtMemberTrait.class)).isPresent();
	}

	@Override
	public List<ValidationEvent> validate(Model model) {

		Stream<ValidationEvent> invalidCountErrors = model.getShapesWithTrait(AdtMemberTrait.class).stream()
				.flatMap(target -> {
					// we simply check if the shape contained in the adtMember trait actually refers
					// to the annotated shape exactly once.
					// that covers all of the "not referenced from anywhere else", "no duplicate
					// references", and "no reference" rules.

					Shape expectedContainer = model.expectShape(target.expectTrait(AdtMemberTrait.class).getValue());

					List<MemberShape> referencesToTargetInContainer = expectedContainer.getAllMembers().values()
							.stream()
							.filter(mem -> mem.getTarget().equals(target.getId()))
							.collect(Collectors.toList());

					switch (referencesToTargetInContainer.size()) {
						case 0:
							// note: this may seem like a duplicate of the invalidReferenceErrors check
							// below, but it's not:
							// this checks for "not referenced anywhere", and the other one checks for
							// "referenced in the wrong place".
							return Stream.of(error(target,
									String.format("This shape must be referenced by %s because of its %s trait",
											expectedContainer.getId(), AdtMemberTrait.ID)));
						case 1:
							return Stream.empty(); // perfect - it's only referenced in the shape that actually should
													// reference it
						default:
							return Stream.of(error(referencesToTargetInContainer.get(0),
									String.format(
											"Duplicate reference to shape %s in container %s - only one is allowed",
											target.getId(), expectedContainer.getId())));
					}
				});

		Stream<Shape> shapesTargettingAdtMembers = adtTargettingContainersSelector.shapes(model);

		Stream<ValidationEvent> invalidReferenceErrors = shapesTargettingAdtMembers.flatMap(parent -> {
			List<MemberShape> adtMembersInParent = parent.getAllMembers()
					.values()
					.stream()
					.filter(mem -> targetIsAdtMember(mem, model))
					.collect(Collectors.toList());

			Stream<MemberShape> invalidMembers = adtMembersInParent.stream().filter(mem -> !model
					.expectShape(mem.getTarget()).expectTrait(AdtMemberTrait.class).getValue().equals(parent.getId()));

			return invalidMembers.map(mem -> {
				ShapeId expectedContainer = model.expectShape(mem.getTarget()).expectTrait(AdtMemberTrait.class)
						.getValue();
				return error(mem,
						String.format("Invalid reference to %s - due to its %s trait, only %s can reference it",
								mem.getTarget(), AdtMemberTrait.ID, expectedContainer));
			});
		});

		return Stream.concat(invalidCountErrors, invalidReferenceErrors).collect(Collectors.toList());
	}
}
