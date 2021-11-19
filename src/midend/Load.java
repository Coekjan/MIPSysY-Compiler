package midend;

import utils.Pair;

public class Load extends IntermediateCode implements IntroSpace {
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
        return temporary ? "temp " : "save " + left + " <- *" + base;
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
}
