package midend;

public class Move extends IntermediateCode {
    public final boolean temporary;
    public final Value left;
    public final Value right;

    public Move(boolean temporary, Value left, Value right) {
        this.temporary = temporary;
        this.left = left;
        this.right = right;
    }

    @Override
    String display() {
        return (temporary ? "temp " : "save ") + left.toString() + " <- " + right.toString();
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        if (temporary) machine.createVar(left.symbol);
        machine.updateVar(left.symbol, right.get(machine));
        return next;
    }
}
