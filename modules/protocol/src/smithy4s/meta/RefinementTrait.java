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
	private final String providerInstance;

	private RefinementTrait(RefinementTrait.Builder builder) {
		super(ID, builder.getSourceLocation());
		this.targetType = builder.targetType;
		this.providerInstance = builder.providerInstance;

		if (targetType == null) {
			throw new SourceException("A targetType must be provided.", getSourceLocation());
		}

		if (providerInstance == null) {
			throw new SourceException("A providerInstance must be provided.", getSourceLocation());
		}
	}

	public String getTargetType() {
		return this.targetType;
	}

	public String getProviderInstance() {
		return this.providerInstance;
	}

	@Override
	protected Node createNode() {
		ObjectNode.Builder builder = Node.objectNodeBuilder();
		builder.withMember("targetType", getTargetType());
		builder.withMember("providerInstance", getProviderInstance());
		return builder.build();
	}

	@Override
	public SmithyBuilder<RefinementTrait> toBuilder() {
		return builder().targetType(targetType).providerInstance(providerInstance)
				.sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new RefinedTrait builder.
	 */
	public static RefinementTrait.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<RefinementTrait, RefinementTrait.Builder> {

		private String targetType;
		private String providerInstance;

		public RefinementTrait.Builder providerInstance(String providerInstance) {
			this.providerInstance = providerInstance;
			return this;
		}

		public RefinementTrait.Builder targetType(String targetType) {
			this.targetType = targetType;
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
			String targetType = objectNode.getMember("targetType")
					.map(node -> node.expectStringNode().getValue()).orElse(null);
			String providerInstance = objectNode.getMember("providerInstance")
					.map(node -> node.expectStringNode().getValue()).orElse(null);
			return builder().sourceLocation(value).targetType(targetType).providerInstance(providerInstance)
					.build();
		}
	}
}
