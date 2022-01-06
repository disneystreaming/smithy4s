/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s.api;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class DiscriminatedUnionTrait extends AbstractTrait implements ToSmithyBuilder<DiscriminatedUnionTrait> {

    public static final ShapeId ID = ShapeId.from("smithy4s.api#discriminated");

    private final String propertyName;

    private DiscriminatedUnionTrait(DiscriminatedUnionTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        this.propertyName = builder.propertyName;
        if (propertyName == null) {
            throw new SourceException("A propertyName must be provided.", getSourceLocation());
        }
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.withMember("propertyName", getPropertyName());
        return builder.build();
    }

    @Override
    public SmithyBuilder<DiscriminatedUnionTrait> toBuilder() {
        return builder().propertyName(propertyName).sourceLocation(getSourceLocation());
    }

    /**
     * @return Returns a new DiscriminatedUnionTrait builder.
     */
    public static DiscriminatedUnionTrait.Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<DiscriminatedUnionTrait, DiscriminatedUnionTrait.Builder> {

        private String propertyName;

        public DiscriminatedUnionTrait.Builder propertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        @Override
        public DiscriminatedUnionTrait build() {
            return new DiscriminatedUnionTrait(this);
        }
    }

    public static final class Provider implements TraitService {

        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public DiscriminatedUnionTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            String propertyName = objectNode.getMember("propertyName")
                    .map(node -> node.expectStringNode().getValue()).orElse(null);
            return builder().sourceLocation(value).propertyName(propertyName).build();
        }
    }
}
