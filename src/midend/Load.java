package midend;

public class Load extends IntermediateCode {
    public final Value left;
    public final AddrValue base;

    public Load(Value left, AddrValue base) {
        this.left = left;
        this.base = base;
    }

    @Override
    String display() {
        return left + " <- *" + base;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.updateVar(left.symbol, machine.load(base.get(machine)));
        return next;
    }
}
