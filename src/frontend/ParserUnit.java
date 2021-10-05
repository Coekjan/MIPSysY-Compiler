package frontend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ParserUnit {
    public static final List<ParserUnit> NIL = new LinkedList<>();

    private final String name;
    private final List<ParserUnit> derivations;

    public ParserUnit(String name, List<ParserUnit> units) {
        this.name = name;
        this.derivations = new ArrayList<>(units);
    }

    public ParserUnit(String name, ParserUnit... units) {
        this(name, Arrays.asList(units));
    }

    public List<ParserUnit> getDerivations() {
        return derivations;
    }

    @Override
    public String toString() {
        return derivations.stream()
                .map(ParserUnit::toString)
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("") + "\n<" + name + ">";
    }
}
