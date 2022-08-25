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

public class DefaultRenderTrait extends AbstractTrait implements ToSmithyBuilder<DefaultRenderTrait> {

	public enum DefaultRenderMode {
		FULL, OPTION_ONLY, NONE
	}

	public static final ShapeId ID = ShapeId.from("smithy4s.meta#defaultRender");

	private final DefaultRenderMode defaultRenderMode;

	private DefaultRenderTrait(Builder builder) {
		super(ID, builder.getSourceLocation());
		this.defaultRenderMode = builder.defaultRenderMode;
		if (defaultRenderMode == null) {
			throw new SourceException("A defaultRenderMode must be provided.", getSourceLocation());
		}
	}

	public DefaultRenderMode getDefaultRenderMode() {
		return this.defaultRenderMode;
	}

	@Override
	protected Node createNode() {
		ObjectNode.Builder builder = Node.objectNodeBuilder();
		builder.withMember("mode", getDefaultRenderMode().name());
		return builder.build();
	}

	@Override
	public SmithyBuilder<DefaultRenderTrait> toBuilder() {
		return builder().defaultRenderMode(defaultRenderMode).sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new DefaultRenderTrait builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<DefaultRenderTrait, Builder> {

		private DefaultRenderMode defaultRenderMode;

		public Builder defaultRenderMode(DefaultRenderMode defaultRenderMode) {
			this.defaultRenderMode = defaultRenderMode;
			return this;
		}

		@Override
		public DefaultRenderTrait build() {
			return new DefaultRenderTrait(this);
		}
	}

	public static final class Provider implements TraitService {

		@Override
		public ShapeId getShapeId() {
			return ID;
		}

		@Override
		public DefaultRenderTrait createTrait(ShapeId target, Node value) {
			ObjectNode objectNode = value.expectObjectNode();
			DefaultRenderMode defaultRenderMode = objectNode.getMember("mode")
					.map(node -> DefaultRenderMode.valueOf(node.expectStringNode().getValue())).orElse(null);
			return builder().sourceLocation(value).defaultRenderMode(defaultRenderMode).build();
		}
	}
}
