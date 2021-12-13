package midend;

import utils.Pair;

import java.util.Collections;
import java.util.List;

public class Load extends IntermediateCode implements IntroSpace, Definite, Usage<Load> {
    public final boolean temporary;
    public final Value left;
    public final AddrValue base;

    public Load(boolean temporary, Value left, AddrValue base) {
        this.temporary = temporary;
        this.left = left;
        this.base = base;
    }

    @Override
    String display() {
        return (temporary ? "temp " : "save ") + left + " <- *" + base;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.updateVar(left, machine.load(base.get(machine)));
        return next;
    }

    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(left, temporary ? 1 : 0);
    }

    @Override
    public Value getDef() {
        return left;
    }

    @Override
    public List<Value> getUse() {
        return Collections.singletonList(base);
    }

    @Override
    public Load replaceUse(List<Value> uses) {
        return new Load(temporary, left, (AddrValue) uses.get(0));
    }
}
