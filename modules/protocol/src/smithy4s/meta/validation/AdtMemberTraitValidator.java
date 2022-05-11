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
			AdtMemberTrait adtMemberTrait = adtMemberShape.getTrait(AdtMemberTrait.class).get();
			List<Reference> references = getReferences(model, adtMemberShape, adtMemberTrait);
			List<ShapeId> illegalReferencers = references.stream().filter(Reference::getIsInvalid)
					.map(Reference::getReferencer).collect(Collectors.toList());
			List<Reference> legalReferences = references.stream().filter(Reference::getIsValid)
					.collect(Collectors.toList());
			List<ValidationEvent> validationEvents = new ArrayList<>();
			if (illegalReferencers.size() > 0) {
				validationEvents.add(error(adtMemberShape, String.format("%s is improperly referenced from %s",
						adtMemberShape.getId(), illegalReferencers)));
			}
			if (legalReferences.size() < 1) {
				validationEvents.add(error(adtMemberShape, String.format("%s does not target %s in any of its members",
						adtMemberTrait.getValue(), adtMemberShape.getId())));
			}
			Optional<StructureShape> maybeStruct = adtMemberShape.asStructureShape();
			Stream<StructureShape> structStream = maybeStruct.isPresent() ? Stream.of(maybeStruct.get())
					: Stream.empty();
			if (structStream.mapToLong(s -> s.members().size()).sum() == 0) {
				validationEvents.add(error(adtMemberShape, String.format(
						"%s must contain at least 1 member to use the adtMember trait", adtMemberShape.getId())));
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
