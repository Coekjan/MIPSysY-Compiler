package midend;

import utils.Pair;

public class Move extends IntermediateCode implements IntroSpace {
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
        if (temporary) machine.createVar(left);
        machine.updateVar(left, right.get(machine));
        return next;
    }

    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(left, temporary ? 1 : 0);
    }
}
