package midend;

import java.util.Arrays;
import java.util.List;

public class Save extends IntermediateCode implements Usage<Save> {
    public final AddrValue base;
    public final Value right;

    public Save(AddrValue base, Value right) {
        this.base = base;
        this.right = right;
    }

    @Override
    String display() {
        return "*" + base + " <- " + right;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.save(machine.findVar(base), right.get(machine));
        return next;
    }

    @Override
    public List<Value> getUse() {
        return Arrays.asList(base, right);
    }

    @Override
    public Save replaceUse(List<Value> uses) {
        return new Save((AddrValue) uses.get(0), uses.get(1));
    }
}
