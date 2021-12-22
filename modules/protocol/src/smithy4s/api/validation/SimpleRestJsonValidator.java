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

import smithy4s.api.SimpleRestJsonTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SimpleRestJsonValidator extends AbstractValidator {

  @Override
  public List<ValidationEvent> validate(Model model) {
    return model.getShapesWithTrait(SimpleRestJsonTrait.class).stream().flatMap(restJson -> {
      return restJson.asServiceShape().get().getAllOperations().stream().flatMap(operationShapeId -> {
        Optional<Shape> maybeOperation = model.getShape(operationShapeId);
        Stream<ValidationEvent> emptyStream = Stream.empty();
        Optional<Stream<ValidationEvent>> result = maybeOperation.map(op -> {
          if (op.getTrait(HttpTrait.class).isPresent()) {
            return emptyStream;
          } else {
            String id = SimpleRestJsonTrait.ID.toString();
            return Stream
                .of(error(op, "Operations tied to " + id + " services must be annotated with the @http trait"));
          }
        });
        return result.orElseGet(() -> emptyStream);
      });
    }).collect(Collectors.toList());
  }
}
