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

package smithy4s.meta.validation;

import smithy4s.OptionHelper;
import smithy4s.meta.ErrorMessageTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.traits.ErrorTrait;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * `@errorMessages` is required to be applied on a structure's member, per the
 * selector. It is also required that the structure itself is annotated with
 * `@error` and the member to be a string shape.
 */
public final class ErrorMessageTraitValidator extends AbstractValidator {
	@Override
	public List<ValidationEvent> validate(Model model) {
		Set<Shape> shapes = model.getShapesWithTrait(ErrorMessageTrait.class);
		return shapes.stream().flatMap(shape -> OptionHelper.toStream(shape.asMemberShape())).flatMap(mShape -> {
			final List<ValidationEvent> validation = new ArrayList<>();

			// get mShape target and validate it's a string
			if (!model.getShape(mShape.getTarget()).get().asStringShape().isPresent()) {
				validation.add(error(mShape, "@errorMessage should only be used on member that target a String shape"));
			}

			// get mShape container and validate it has the `error` annotation
			final Shape container = model.getShape(mShape.getContainer()).get();
			if (!container.hasTrait(ErrorTrait.class)) {
				validation.add(error(container,
						"the structure containing the member annotated with @errorMessage has to be annotated with @error"));
			}
			return validation.stream();
		}).collect(Collectors.toList());
	}
}
