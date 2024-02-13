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

import java.util.Objects;
import java.util.function.BiFunction;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

/**
 * Abstract trait class for traits that contain only a ShapeId value.
 */
public abstract class ShapeIdTrait extends AbstractTrait {
	private final ShapeId value;

	/**
	 * @param id             The ID of the trait being created.
	 * @param value          The shapeId value of the trait.
	 * @param sourceLocation Where the trait was defined.
	 */
	public ShapeIdTrait(ShapeId id, ShapeId value, FromSourceLocation sourceLocation) {
		super(id, sourceLocation);
		this.value = Objects.requireNonNull(value, "Trait values must not be null");
	}

	/**
	 * @return Get the trait value.
	 */
	public ShapeId getValue() {
		return value;
	}

	@Override
	protected final Node createNode() {
		return new StringNode(value.toString(), getSourceLocation());
	}

	/**
	 * Trait provider that expects a ShapeId value.
	 */
	public static class Provider<T extends ShapeIdTrait> extends AbstractTrait.Provider {
		private final BiFunction<ShapeId, SourceLocation, T> traitFactory;

		/**
		 * @param id           The name of the trait being created.
		 * @param traitFactory The factory used to create the trait.
		 */
		public Provider(ShapeId id, BiFunction<ShapeId, SourceLocation, T> traitFactory) {
			super(id);
			this.traitFactory = traitFactory;
		}

		@Override
		public T createTrait(ShapeId id, Node value) {
			T result = traitFactory.apply(ShapeId.from(value.expectStringNode().getValue()), value.getSourceLocation());
			// Reuse the node instead of creating a new one.
			result.setNodeCache(value);
			return result;
		}
	}
}
