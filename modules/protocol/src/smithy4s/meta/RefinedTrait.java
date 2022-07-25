package smithy4s.meta;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;
import java.util.Optional;

public final class RefinedTrait extends AbstractTrait implements ToSmithyBuilder<RefinedTrait> {

	public static final ShapeId ID = ShapeId.from("smithy4s.meta#refined");

	private final String targetClasspath;
	private final String providerClasspath;
	private final Optional<ShapeId> canonicalShapeId;

	private RefinedTrait(RefinedTrait.Builder builder) {
		super(ID, builder.getSourceLocation());
		this.targetClasspath = builder.targetClasspath;
		this.providerClasspath = builder.providerClasspath;
		this.canonicalShapeId = builder.canonicalShapeId;

		if (targetClasspath == null) {
			throw new SourceException("A targetClasspath must be provided.", getSourceLocation());
		}

		if (providerClasspath == null) {
			throw new SourceException("A providerClasspath must be provided.", getSourceLocation());
		}
	}

	public String getTargetClasspath() {
		return this.targetClasspath;
	}

	public String getProviderClasspath() {
		return this.providerClasspath;
	}

	public Optional<ShapeId> getCanonicalShapeId() {
		return this.canonicalShapeId;
	}

	@Override
	protected Node createNode() {
		ObjectNode.Builder builder = Node.objectNodeBuilder();
		builder.withMember("targetClasspath", getTargetClasspath());
		builder.withMember("providerClasspath", getProviderClasspath());

		builder.withOptionalMember("canonicalShape", getCanonicalShapeId().map(sId -> Node.from(sId.toString())));
		return builder.build();
	}

	@Override
	public SmithyBuilder<RefinedTrait> toBuilder() {
		return builder().targetClasspath(targetClasspath).providerClasspath(providerClasspath)
				.canonicalShapeId(canonicalShapeId).sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new RefinedTrait builder.
	 */
	public static RefinedTrait.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<RefinedTrait, RefinedTrait.Builder> {

		private String targetClasspath;
		private String providerClasspath;
		private Optional<ShapeId> canonicalShapeId;

		public RefinedTrait.Builder providerClasspath(String providerClasspath) {
			this.providerClasspath = providerClasspath;
			return this;
		}

		public RefinedTrait.Builder targetClasspath(String targetClasspath) {
			this.targetClasspath = targetClasspath;
			return this;
		}

		public RefinedTrait.Builder canonicalShapeId(ShapeId canonicalShapeId) {
			this.canonicalShapeId = Optional.ofNullable(canonicalShapeId);
			return this;
		}

		public RefinedTrait.Builder canonicalShapeId(Optional<ShapeId> canonicalShapeId) {
			this.canonicalShapeId = canonicalShapeId;
			return this;
		}

		@Override
		public RefinedTrait build() {
			return new RefinedTrait(this);
		}
	}

	public static final class Provider implements TraitService {

		@Override
		public ShapeId getShapeId() {
			return ID;
		}

		@Override
		public RefinedTrait createTrait(ShapeId target, Node value) {
			ObjectNode objectNode = value.expectObjectNode();
			String targetClasspath = objectNode.getMember("targetClasspath")
					.map(node -> node.expectStringNode().getValue()).orElse(null);
			String providerClasspath = objectNode.getMember("providerClasspath")
					.map(node -> node.expectStringNode().getValue()).orElse(null);
			Optional<ShapeId> canonicalShapeId = objectNode.getMember("canonicalShape")
					.map(node -> ShapeId.from(node.expectStringNode().getValue()));
			return builder().sourceLocation(value).targetClasspath(targetClasspath).providerClasspath(providerClasspath)
					.canonicalShapeId(canonicalShapeId).build();
		}
	}
}
