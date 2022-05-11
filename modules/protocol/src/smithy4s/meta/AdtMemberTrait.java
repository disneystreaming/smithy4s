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

package smithy4s.meta;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.SourceLocation;

public final class AdtMemberTrait extends ShapeIdTrait {
	public static final ShapeId ID = ShapeId.from("smithy4s.meta#adtMember");

	public AdtMemberTrait(ShapeId value, SourceLocation sourceLocation) {
		super(ID, value, sourceLocation);
	}

	public AdtMemberTrait(ShapeId value) {
		this(value, SourceLocation.NONE);
	}

	public static final class Provider extends ShapeIdTrait.Provider<AdtMemberTrait> {
		public Provider() {
			super(ID, AdtMemberTrait::new);
		}
	}
}
