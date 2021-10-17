package frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParserUnit {
    public final String name;
    public final List<ParserUnit> derivations;

    public ParserUnit(String name, List<ParserUnit> units) {
        this.name = name;
        this.derivations = Collections.unmodifiableList(new ArrayList<>(units));
    }

    @Override
    public String toString() {
        return derivations.stream()
                .map(ParserUnit::toString)
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("") + "\n<" + name + ">";
    }
}
