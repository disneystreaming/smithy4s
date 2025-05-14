import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ApiVersioningValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        model.getShapesWithTrait(ApiVersionTrait.ID).stream().forEach(shape -> {
            System.out.println("validating shape: " + shape);
            shape.expectTrait(ApiVersionTrait.class);
        });

        return List.of();
    }
}
