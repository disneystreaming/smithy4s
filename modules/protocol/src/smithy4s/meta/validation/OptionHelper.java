package smithy4s.meta.validation;

import java.util.stream.Stream;
import java.util.Optional;

public class OptionHelper {
	public static <T> Stream<T> toStream(Optional<T> opt) {
		if (opt.isPresent())
			return Stream.of(opt.get());
		else
			return Stream.empty();
	}
}
