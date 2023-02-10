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
import software.amazon.smithy.model.SourceLocation;
import smithy4s.meta.RefinementTrait;

import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;

public final class RefinementTraitValidator extends AbstractValidator {

	boolean isSimple(Model model, Shape shape) {
		return shape.getType().getCategory() == ShapeType.Category.SIMPLE
				|| shape.getType().getCategory() == ShapeType.Category.MEMBER && shape.asMemberShape()
						.map(ms -> model.expectShape(ms.getTarget())).map(s -> isSimple(model, s)).orElse(false);
	}

	boolean isAllowedType(Model model, Shape shape) {
		boolean notConstrained = shape.getAllTraits().values().stream().allMatch(t -> {
			@SuppressWarnings("deprecation") // EnumTrait
			boolean isConstrained = t instanceof EnumTrait || t instanceof LengthTrait || t instanceof RangeTrait
					|| t instanceof PatternTrait;
			return !isConstrained;
		});
		boolean _isSimple = isSimple(model, shape) && notConstrained;
		@SuppressWarnings("deprecation") // isSetShape
		boolean isCollection = shape.isListShape() || shape.isMapShape() || shape.isSetShape();
		return _isSimple || isCollection;
	}

	// inspired from:
	// https://github.com/awslabs/smithy/blob/7f7669e0ee1f563488d111e9ee9c8df9179533d3/smithy-cli/src/main/java/software/amazon/smithy/cli/commands/Upgrade1to2Command.java#L256
	private boolean isSyntheticDefault(Shape shape, DefaultTrait defTrait) {
		Optional<SourceLocation> defaultLocation = shape.getTrait(DefaultTrait.class).map(Trait::getSourceLocation);
		// When Smithy injects the default trait, it sets the source
		// location equal to the shape's source location. This is
		// impossible in any other scenario, so we can use this info
		// to know whether it was injected or not.
		return shape.getSourceLocation().equals(defTrait.getSourceLocation());
	}

	// if there is a default trait, and it is not a synthetic one
	// then we can assume the user added it
	boolean hasUserDefault(Shape shape) {
		Optional<DefaultTrait> defaultTrait = shape.getTrait(DefaultTrait.class);
		return defaultTrait.filter(d -> !isSyntheticDefault(shape, d)).isPresent();
	}

	Stream<ValidationEvent> refinedTraitsNoDefault(Model model) {
		return model.getShapesWithTrait(RefinementTrait.class).stream().flatMap(refinedTrait -> {
			return model.getShapesWithTrait(refinedTrait.getId()).stream().filter(shape -> hasUserDefault(shape))
					.map(shape -> warning(shape, refinedTrait.getId() + " is a refinement trait. It is applied to "
							+ shape.getId() + " along with a @default trait. You should avoid mixing the two."));
		});
	}

	@Override
	public List<ValidationEvent> validate(Model model) {
		Stream<ValidationEvent> countErrors = model.shapes().flatMap(s -> {
			long numRefinedTraits = s.getAllTraits().values().stream().flatMap(t -> {
				return OptionHelper.toStream(model.getShape(t.toShapeId())).flatMap(traitShape -> {
					return OptionHelper.toStream(traitShape.getTrait(RefinementTrait.class));
				});
			}).count();
			if (numRefinedTraits > 1) {
				return Stream.of(error(s, "Shapes may only be annotated with one refinement trait"));
			} else if (numRefinedTraits == 1 && !isAllowedType(model, s)) {
				return Stream.of(error(s,
						"refinements can only be used on simpleShapes, list, set, and map. Simple shapes must not be constrained by enum, length, range, or pattern traits"));
			} else {
				return Stream.empty();
			}
		});
		Stream<ValidationEvent> defaultUsageWarnings = refinedTraitsNoDefault(model);
		return Stream.of(countErrors, defaultUsageWarnings).flatMap(s -> s).collect(Collectors.toList());
	}
}
