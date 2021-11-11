package midend;

public class Branch extends IntermediateCode {
    public final Value condition;
    public final String label;

    public Branch(Value condition, String label) {
        this.condition = condition;
        this.label = label;
    }

    @Override
    String display() {
        return "branch " + label + " if " + condition;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        final int val = condition.get(machine);
        if (val != 0) {
            return labelTable.find(label);
        }
        return next;
    }
}
