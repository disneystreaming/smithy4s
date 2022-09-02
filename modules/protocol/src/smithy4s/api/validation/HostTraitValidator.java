package smithy4s.api.validation;

import smithy4s.api.HostTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HostTraitValidator extends AbstractValidator {

    Set<String> allowedParameterNames = new java.util.HashSet<>(Arrays.asList("env", "pool", "region"));

    private boolean hasProperBrackets(String host) {
        boolean inOpen = false;
        for (char c : host.toCharArray()) {
            if (c == '{') {
                if (inOpen) return false;
                inOpen = true;
            } else if (c == '}') {
                if (!inOpen) return false;
                inOpen = false;
            }
        }
        return !inOpen;
    }

    private List<String> getParameters(String host) {
        String open = "{";
        String close = "}";
        Pattern p = Pattern.compile(Pattern.quote(open) + "(.*?)" + Pattern.quote(close));
        Matcher matcher = p.matcher(host);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                list.add(matcher.group(i));
            }
        }
        return list;
    }

    private boolean isProperHostName(String host) {
        return host.matches("^(([a-zA-Z0-9{}-]+)\\.?)*");
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapesWithTrait(HostTrait.class).stream().flatMap(hostShape -> {
            String host = hostShape.getTrait(HostTrait.class).get().getValue();
            if (!hasProperBrackets(host)) {
                return Stream.of(error(hostShape, "Host parameter brackets are not syntactically correct"));
            }
            if (!isProperHostName(host)) {
                return Stream.of(error(hostShape, "Not a proper host name"));
            }
            List<String> invalidNames = getParameters(host).stream().filter(allowedParameterNames::contains).collect(Collectors.toList());

            if(!invalidNames.isEmpty()) {
                return Stream.of(error(hostShape, "Invalid host parameter names: " + invalidNames.stream().collect(Collectors.joining(", "))));
            } else {
                return Stream.empty();
            }
        }).collect(Collectors.toList());
    }
}

