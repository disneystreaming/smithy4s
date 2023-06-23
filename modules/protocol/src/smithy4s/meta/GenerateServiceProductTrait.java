package smithy4s.meta;

import java.text.Annotation;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.model.traits.AbstractTrait;

public class GenerateServiceProductTrait extends AnnotationTrait {
  public static ShapeId ID = ShapeId.from("smithy4s.meta#generateServiceProduct");

	public GenerateServiceProductTrait(ObjectNode node) {
		super(ID, node);
	}

	public GenerateServiceProductTrait() {
		super(ID, Node.objectNode());
	}

	public static final class Provider extends AbstractTrait.Provider {
		public Provider() {
			super(ID);
		}

		@Override
		public GenerateServiceProductTrait createTrait(ShapeId target, Node node) {
			return new GenerateServiceProductTrait(node.expectObjectNode());
		}
	}
}
