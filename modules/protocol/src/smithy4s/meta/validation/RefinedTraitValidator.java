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
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.*;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import smithy4s.meta.RefinedTrait;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RefinedTraitValidator extends AbstractValidator {

	boolean isAllowedType(Shape shape) {
		boolean notConstrained = shape.getAllTraits().values().stream().allMatch(t -> {
			boolean isConstrained = t instanceof EnumTrait || t instanceof LengthTrait || t instanceof RangeTrait
					|| t instanceof PatternTrait;
			return !isConstrained;
		});
		boolean isSimple = shape.getType().getCategory() == ShapeType.Category.SIMPLE && notConstrained;
		boolean isCollection = shape.isListShape() || shape.isMapShape() || shape.isSetShape();
		return isSimple || isCollection;
	}

	@Override
	public List<ValidationEvent> validate(Model model) {
		return model.shapes().flatMap(s -> {
			long numRefinedTraits = s.getAllTraits().values().stream().flatMap(t -> {
				return OptionHelper.toStream(model.getShape(t.toShapeId())).flatMap(traitShape -> {
					return OptionHelper.toStream(traitShape.getTrait(RefinedTrait.class));
				});
			}).count();
			if (numRefinedTraits > 1) {
				return Stream.of(error(s, "Shapes may only be annotated with one refinement trait"));
			} else if (numRefinedTraits == 1 && !isAllowedType(s)) {
				return Stream.of(error(s,
						"refinements can only be used on simpleShapes, list, set, and map. Simple shapes must not be constrained by enum, length, range, or pattern traits"));
			} else {
				return Stream.empty();
			}
		}).collect(Collectors.toList());
	}
}
