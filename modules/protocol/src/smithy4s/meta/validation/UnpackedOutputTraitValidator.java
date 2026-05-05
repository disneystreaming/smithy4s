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

import smithy4s.meta.UnpackedOutputTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Operations marked with `@unpackedOutput` must have an output structure
 * containing exactly one member, so that the generated method can return
 * the inner member's type.
 */
public final class UnpackedOutputTraitValidator extends AbstractValidator {
	@Override
	public List<ValidationEvent> validate(Model model) {
		List<ValidationEvent> events = new ArrayList<>();
		Set<Shape> shapes = model.getShapesWithTrait(UnpackedOutputTrait.class);
		for (Shape shape : shapes) {
			Optional<OperationShape> maybeOp = shape.asOperationShape();
			if (!maybeOp.isPresent()) {
				continue;
			}
			OperationShape op = maybeOp.get();
			Optional<StructureShape> maybeOutput =
				model.getShape(op.getOutputShape()).flatMap(Shape::asStructureShape);
			if (!maybeOutput.isPresent()) {
				events.add(error(op,
					"Operations annotated with @unpackedOutput must have a structure as output"));
				continue;
			}
			StructureShape output = maybeOutput.get();
			int memberCount = output.getAllMembers().size();
			if (memberCount != 1) {
				events.add(error(op, String.format(
					"Operations annotated with @unpackedOutput must have an output structure with exactly one member, but %s has %d",
					output.getId(), memberCount)));
			}
		}
		return events;
	}
}
