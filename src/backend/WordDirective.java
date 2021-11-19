package backend;

import midend.Value;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class WordDirective extends Directive {
    public final Value name;
    public final List<Integer> values;

    public WordDirective(Value name, List<Integer> values) {
        super(name.symbol.substring(0, name.symbol.indexOf('%') >= 0 ? name.symbol.indexOf('%') : name.symbol.length()));
        this.name = name;
        this.values = Collections.unmodifiableList(values);
    }

    @Override
    String stringify() {
        final Optional<String> valueList = values.stream().map(Object::toString).reduce((x, y) -> x + ", " + y);
        assert valueList.isPresent();
        return label + ": .word " + valueList.get();
    }
}
