/*
 *  Copyright 2021-2024 Disney Streaming
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

public final class RefinementTrait extends AbstractTrait implements ToSmithyBuilder<RefinementTrait> {

	public static final ShapeId ID = ShapeId.from("smithy4s.meta#refinement");

	private final String targetType;
	private final Optional<String> providerImport;
	private final boolean parameterised;

	private RefinementTrait(RefinementTrait.Builder builder) {
		super(ID, builder.getSourceLocation());
		this.targetType = builder.targetType;
		this.providerImport = builder.providerImport;
		this.parameterised = builder.parameterised;

		if (targetType == null) {
			throw new SourceException("A targetType must be provided.", getSourceLocation());
		}
	}

	public String getTargetType() {
		return this.targetType;
	}

	public Optional<String> getProviderImport() {
		return this.providerImport;
	}

	public boolean isParameterised() {
		return this.parameterised;
	}

	@Override
	protected Node createNode() {
		ObjectNode.Builder builder = Node.objectNodeBuilder();
		builder.withMember("targetType", getTargetType());
		Optional<String> maybeImport = getProviderImport();
		if (maybeImport.isPresent()) {
			builder.withMember("providerImport", maybeImport.get());
		}
		if (parameterised) {
			builder.withMember("parameterised", parameterised);
		}
		return builder.build();
	}

	@Override
	public SmithyBuilder<RefinementTrait> toBuilder() {
		return builder().targetType(targetType).providerImport(providerImport).sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new RefinedTrait builder.
	 */
	public static RefinementTrait.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<RefinementTrait, RefinementTrait.Builder> {

		private String targetType;
		private Optional<String> providerImport = Optional.empty();
		private boolean parameterised;

		public RefinementTrait.Builder providerImport(String providerImport) {
			this.providerImport = Optional.ofNullable(providerImport);
			return this;
		}

		public RefinementTrait.Builder providerImport(Optional<String> providerImport) {
			this.providerImport = providerImport;
			return this;
		}

		public RefinementTrait.Builder targetType(String targetType) {
			this.targetType = targetType;
			return this;
		}

		public RefinementTrait.Builder parameterised(boolean parameterised) {
			this.parameterised = parameterised;
			return this;
		}

		@Override
		public RefinementTrait build() {
			return new RefinementTrait(this);
		}
	}

	public static final class Provider implements TraitService {

		@Override
		public ShapeId getShapeId() {
			return ID;
		}

		@Override
		public RefinementTrait createTrait(ShapeId target, Node value) {
			ObjectNode objectNode = value.expectObjectNode();
			String targetType = objectNode.getMember("targetType").map(node -> node.expectStringNode().getValue())
					.orElse(null);
			Optional<String> providerImport = objectNode.getMember("providerImport")
					.map(node -> node.expectStringNode().getValue());
			Boolean parameterised = objectNode.getMember("parameterised")
					.map(node -> node.expectBooleanNode().getValue()).orElse(false);
			return builder().sourceLocation(value).targetType(targetType).parameterised(parameterised)
					.providerImport(providerImport).build();
		}
	}
}
