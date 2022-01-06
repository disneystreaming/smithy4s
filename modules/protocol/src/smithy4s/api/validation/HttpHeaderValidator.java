/*
 *  Copyright 2021 Disney Streaming
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
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HttpHeaderValidator extends AbstractValidator {

    List<String> disallowedHeaderNames = java.util.Arrays.asList("content-type");

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapesWithTrait(HttpHeaderTrait.class).stream().flatMap(headerShape -> {
            String value = headerShape.getTrait(HttpHeaderTrait.class).get().getValue();
            if (disallowedHeaderNames.contains(value.toLowerCase())) {
                return Stream.of(warning(headerShape, String.format("Header named `%s` may be overridden in client/server implementations", value)));
            } else {
                return Stream.empty();
            }
        }).collect(Collectors.toList());
    }
}
