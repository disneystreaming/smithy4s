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

public final class ScalaImportsTrait extends AbstractTrait implements ToSmithyBuilder<ScalaImportsTrait> {


	public static final ShapeId ID = ShapeId.from("smithy4s.meta#scalaImports");

	private final String providerImport;

	private ScalaImportsTrait(ScalaImportsTrait.Builder builder) {
		super(ID, builder.getSourceLocation());
		this.providerImport = builder.providerImport;

		if (providerImport == null) {
			throw new SourceException("An providerImport must be provided.", getSourceLocation());
		}
	}

	public String getProviderImport() {
		return this.providerImport;
	}

	@Override
	protected Node createNode() {
		ObjectNode.Builder builder = Node.objectNodeBuilder();
		builder.withMember("providerImport", getProviderImport());
		return builder.build();
	}

	@Override
	public SmithyBuilder<ScalaImportsTrait> toBuilder() {
		return builder().providerImport(providerImport).sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new RefinedTrait builder.
	 */
	public static ScalaImportsTrait.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<ScalaImportsTrait, ScalaImportsTrait.Builder> {

		private String providerImport;

		public ScalaImportsTrait.Builder providerImport(String providerImport) {
			this.providerImport = providerImport;
			return this;
		}

		@Override
		public ScalaImportsTrait build() {
			return new ScalaImportsTrait(this);
		}
	}

	public static final class Provider implements TraitService {

		@Override
		public ShapeId getShapeId() {
			return ID;
		}

		@Override
		public ScalaImportsTrait createTrait(ShapeId target, Node value) {
			ObjectNode objectNode = value.expectObjectNode();
			String providerImport = objectNode.getMember("providerImport").map(node -> node.expectStringNode().getValue()).orElse(null);
			return builder().sourceLocation(value).providerImport(providerImport).build();
		}
	}
}
