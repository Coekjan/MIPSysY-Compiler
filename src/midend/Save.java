package midend;

public class Save extends IntermediateCode {
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
        machine.save(machine.findVar(base.symbol), right.get(machine));
        return next;
    }
}
