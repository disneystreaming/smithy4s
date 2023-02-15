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

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class AdtValidatorCommon {
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

	private static List<Reference> getReferences(Model model, Shape adtMemberShape, Shape adtParent) {
		return model.getMemberShapes().stream().flatMap(memberShape -> {
			boolean doesMemberTargetAdtShape = memberShape.getTarget() == adtMemberShape.getId();
			boolean isMemberShapeInDesiredTarget = memberShape.getContainer().equals(adtParent.toShapeId());
			if (doesMemberTargetAdtShape) {
				return Stream.of(new Reference(isMemberShapeInDesiredTarget, memberShape.getContainer()));
			} else {
				return Stream.empty();
			}
		}).collect(Collectors.toList());
	}

	private static ValidationEvent error(Shape shape, String message) {
		return ValidationEvent.builder().id("AdtValidator").sourceLocation(shape.getSourceLocation()).shape(shape)
				.severity(Severity.ERROR).message(message).build();
	}

	public static Stream<ValidationEvent> getReferenceEvents(Model model, Collection<Shape> adtMemberShapes,
			Shape adtParent) {
		return adtMemberShapes.stream().flatMap(adtMemberShape -> {
			List<Reference> references = getReferences(model, adtMemberShape, adtParent);
			List<ShapeId> illegalReferencers = references.stream().filter(Reference::getIsInvalid)
					.map(Reference::getReferencer).collect(Collectors.toList());
			List<Reference> legalReferences = references.stream().filter(Reference::getIsValid)
					.collect(Collectors.toList());
			List<ValidationEvent> validationEvents = new ArrayList<>();
			if (illegalReferencers.size() > 0) {
				illegalReferencers.forEach(illegalReferencerId -> {
					Shape illegalReferencer = model.expectShape(illegalReferencerId);
					validationEvents.add(error(illegalReferencer,
							String.format("ADT member %s must not be referenced in any other shape but %s",
									adtMemberShape.getId(), adtParent.getId())));
				});
			}
			if (legalReferences.size() < 1) {
				validationEvents.add(error(adtMemberShape, String.format("%s must have exactly one member targeting %s",
						adtParent.getId(), adtMemberShape.getId())));
			}
			return validationEvents.stream();
		});
	}
}
