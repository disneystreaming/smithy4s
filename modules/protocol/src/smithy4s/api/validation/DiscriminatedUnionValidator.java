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

package smithy4s.api.validation;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import smithy4s.api.DiscriminatedUnionTrait;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DiscriminatedUnionValidator extends AbstractValidator {

	@Override
	public List<ValidationEvent> validate(Model model) {
		return model.getShapesWithTrait(DiscriminatedUnionTrait.class).stream().flatMap(unionShape -> {
			DiscriminatedUnionTrait discriminated = unionShape.getTrait(DiscriminatedUnionTrait.class).get();
			return unionShape.asUnionShape().get().getAllMembers().entrySet().stream().flatMap(entry -> {
				Optional<Shape> maybeTarget = model.getShape(entry.getValue().getTarget());
				if (maybeTarget.isPresent() && maybeTarget.get().isStructureShape()) { // if not defined then shape
																						// won't be structure
					Map<String, MemberShape> structureMembers = maybeTarget.get().asStructureShape().get()
							.getAllMembers();
					if (structureMembers.get(discriminated.getValue()) != null) {
						return Stream.of(error(entry.getValue(),
								String.format("Target of member '%s' contains discriminator '%s'", entry.getKey(),
										discriminated.getValue())));
					} else {
						return Stream.empty();
					}
				} else {
					return Stream.of(error(entry.getValue(),
							String.format("Target of member '%s' is not a structure shape", entry.getKey())));
				}
			});
		}).collect(Collectors.toList());
	}
}
