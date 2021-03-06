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
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All structures annotated with `@adtMember(SomeUnion)` are targeted in EXACTLY
 * ONE place: as a member of the union they reference in their idRef (SomeUnion
 * in this case)
 *
 * Also checks that structure is not empty (must have at least one member)
 */
public final class AdtMemberTraitValidator extends AbstractValidator {

	private static final class Reference {
		private final boolean isValid;
		private final ShapeId referencer;

		Reference(boolean isValid, ShapeId referencer) {
			this.isValid = isValid;
			this.referencer = referencer;
		}

		boolean getIsValid() {
			return isValid;
		}

		boolean getIsInvalid() {
			return !isValid;
		}

		ShapeId getReferencer() {
			return referencer;
		}
	}

	private List<Reference> getReferences(Model model, Shape adtMemberShape, AdtMemberTrait adtMemberTrait) {
		return model.getMemberShapes().stream().flatMap(memberShape -> {
			boolean doesMemberTargetAdtShape = memberShape.getTarget() == adtMemberShape.getId();
			boolean isMemberShapeInDesiredTarget = memberShape.getContainer().equals(adtMemberTrait.getValue());
			if (doesMemberTargetAdtShape) {
				return Stream.of(new Reference(isMemberShapeInDesiredTarget, memberShape.getContainer()));
			} else {
				return Stream.empty();
			}
		}).collect(Collectors.toList());
	}

	private List<ValidationEvent> getReferenceEvents(Model model, Set<Shape> adtMemberShapes) {
		return adtMemberShapes.stream().flatMap(adtMemberShape -> {
			AdtMemberTrait adtMemberTrait = adtMemberShape.expectTrait(AdtMemberTrait.class);
			List<Reference> references = getReferences(model, adtMemberShape, adtMemberTrait);
			List<ShapeId> illegalReferencers = references.stream().filter(Reference::getIsInvalid)
					.map(Reference::getReferencer).collect(Collectors.toList());
			List<Reference> legalReferences = references.stream().filter(Reference::getIsValid)
					.collect(Collectors.toList());
			List<ValidationEvent> validationEvents = new ArrayList<>();
			if (illegalReferencers.size() > 0) {
				illegalReferencers.forEach(illegalReferencerId -> {
					Shape illegalReferencer = model.expectShape(illegalReferencerId);
					validationEvents
							.add(error(illegalReferencer, String.format("ADT member %s must not be referenced in any other shape but %s",
									adtMemberShape.getId(), adtMemberTrait.getValue())));
				});
			}
			if (legalReferences.size() < 1) {
				validationEvents.add(error(adtMemberShape, String.format("%s must have exactly one member targeting %s",
						adtMemberTrait.getValue(), adtMemberShape.getId())));
			}
			return validationEvents.stream();
		}).collect(Collectors.toList());
	}

	@Override
	public List<ValidationEvent> validate(Model model) {
		Set<Shape> adtMemberShapes = model.getShapesWithTrait(AdtMemberTrait.class);
		return getReferenceEvents(model, adtMemberShapes);
	}
}
