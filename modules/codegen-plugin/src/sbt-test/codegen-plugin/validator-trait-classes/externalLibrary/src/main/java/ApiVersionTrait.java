import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

public class ApiVersionTrait extends StringTrait {

	public static ShapeId ID = ShapeId.from("my.input#apiVersion");

	public ApiVersionTrait(String value, SourceLocation sourceLocation) {
		super(ID, value, sourceLocation);
	}

	public ApiVersionTrait(String value) {
		this(value, SourceLocation.NONE);
	}

	public static final class Provider extends StringTrait.Provider<ApiVersionTrait> {
		public Provider() {
			super(ID, ApiVersionTrait::new);
		}
	}

}
