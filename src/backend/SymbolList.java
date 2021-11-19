package backend;

import java.util.ArrayList;
import java.util.List;

public class SymbolList {
    private final List<Directive> globalDirectives = new ArrayList<>();
    public final TagTable tagTable = new TagTable();

    public void insertDirective(Directive directive) {
        globalDirectives.add(directive);
    }

    public String displayDirectives() {
        return ".data\n" + globalDirectives.stream().map(Directive::toString)
                .reduce((a, b) -> a + "\n" + b).orElse("");
    }

    public List<Directive> getGlobalDirectives() {
        return globalDirectives;
    }
}
