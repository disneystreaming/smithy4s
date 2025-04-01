/*
 *  Copyright 2021-2025 Disney Streaming
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

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ScalaImportsTrait extends AbstractTrait implements ToSmithyBuilder<ScalaImportsTrait> {


	public static final ShapeId ID = ShapeId.from("smithy4s.meta#scalaImports");

	private final List<String> imports;

	private ScalaImportsTrait(ScalaImportsTrait.Builder builder) {
		super(ID, builder.getSourceLocation());
		this.imports = builder.imports;

		if (this.imports == null) {
			throw new SourceException("imports must be provided.", getSourceLocation());
		}
	}

	public List<String> getImports() {
		return this.imports;
	}

	@Override
	protected Node createNode() {
		ArrayNode.Builder builder = ArrayNode.builder();
    getImports().forEach(s -> builder.withValue(s));
		return builder.build();
	}

	@Override
	public SmithyBuilder<ScalaImportsTrait> toBuilder() {
		return builder().imports(imports).sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new RefinedTrait builder.
	 */
	public static ScalaImportsTrait.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<ScalaImportsTrait, ScalaImportsTrait.Builder> {

		private List<String> imports;

		public ScalaImportsTrait.Builder imports(List<String> imports) {
			this.imports = imports;
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
			ArrayNode arrayNode = value.expectArrayNode();
			List<String> imports = arrayNode.getElements().stream().map(node -> node.expectStringNode().getValue()).collect(Collectors.toList());
			return builder().sourceLocation(value).imports(imports).build();
		}

	}
}
