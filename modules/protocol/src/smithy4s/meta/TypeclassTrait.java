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

public final class TypeclassTrait extends AbstractTrait implements ToSmithyBuilder<TypeclassTrait> {

	public static final ShapeId ID = ShapeId.from("smithy4s.meta#typeclass");

	private final String targetType;
	private final String interpreter;

	private TypeclassTrait(TypeclassTrait.Builder builder) {
		super(ID, builder.getSourceLocation());
		this.targetType = builder.targetType;
		this.interpreter = builder.interpreter;

		if (targetType == null) {
			throw new SourceException("A targetType must be provided.", getSourceLocation());
		}

		if (interpreter == null) {
			throw new SourceException("An interpreter must be provided.", getSourceLocation());
		}
	}

	public String getTargetType() {
		return this.targetType;
	}

	public String getInterpreter() {
		return this.interpreter;
	}

	@Override
	protected Node createNode() {
		ObjectNode.Builder builder = Node.objectNodeBuilder();
		builder.withMember("targetType", getTargetType());
		builder.withMember("interpreter", getInterpreter());
		return builder.build();
	}

	@Override
	public SmithyBuilder<TypeclassTrait> toBuilder() {
		return builder().targetType(targetType).interpreter(interpreter).sourceLocation(getSourceLocation());
	}

	/**
	 * @return Returns a new TypeclassTrait builder.
	 */
	public static TypeclassTrait.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<TypeclassTrait, TypeclassTrait.Builder> {

		private String targetType;
		private String interpreter;

		public TypeclassTrait.Builder targetType(String targetType) {
			this.targetType = targetType;
			return this;
		}

		public TypeclassTrait.Builder interpreter(String interpreter) {
			this.interpreter = interpreter;
			return this;
		}

		@Override
		public TypeclassTrait build() {
			return new TypeclassTrait(this);
		}
	}

	public static final class Provider implements TraitService {

		@Override
		public ShapeId getShapeId() {
			return ID;
		}

		@Override
		public TypeclassTrait createTrait(ShapeId target, Node value) {
			ObjectNode objectNode = value.expectObjectNode();
			String targetType = objectNode.getMember("targetType").map(node -> node.expectStringNode().getValue())
					.orElse(null);
			String interpreter = objectNode.getMember("interpreter").map(node -> node.expectStringNode().getValue())
					.orElse(null);
			return builder().sourceLocation(value).targetType(targetType).interpreter(interpreter).build();
		}
	}
}
