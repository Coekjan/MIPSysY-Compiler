package midend;

import utils.Pair;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class Declaration extends IntermediateCode implements IntroSpace {
    public final boolean global;
    public final boolean modifiable;
    public final Value symbol;
    public final int size;
    public final List<Value> initValues;

    public Declaration(boolean global, boolean modifiable, Value symbol, int size, List<Value> initValues) {
        this.global = global;
        this.modifiable = modifiable;
        this.symbol = symbol;
        this.size = size;
        this.initValues = Collections.unmodifiableList(initValues);
    }

    @Override
    String display() {
        final StringJoiner sj = new StringJoiner(" ");
        sj.add(global ? "global" : "locale");
        sj.add(modifiable ? "var" : "const");
        sj.add(symbol.toString());
        sj.add("[size = " + size +"]");
        sj.add(initValues.stream().map(Value::toString).reduce((v1, v2) -> v1 + ", " + v2).orElse(""));
        return sj.toString();
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        if (symbol instanceof AddrValue) {
            machine.createArr((AddrValue) symbol, size);
            final int baseAddress = symbol.get(machine);
            for (int i = 0; i < initValues.size(); ++i) {
                machine.save(baseAddress + i, initValues.get(i).get(machine));
            }
        } else {
            machine.createVar(symbol);
            if (!initValues.isEmpty()) machine.updateVar(symbol, initValues.get(0).get(machine));
        }
        return next;
    }

    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(symbol, size);
    }
}
