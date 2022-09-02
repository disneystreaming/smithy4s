package smithy4s.api;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

public class HostTrait extends StringTrait {

    public static final ShapeId ID = ShapeId.from("dslib#host");

    public HostTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public HostTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<HostTrait> {
        public Provider() {
            super(ID, HostTrait::new);
        }
    }
}
