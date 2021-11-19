package midend;

import utils.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class AssignUnaryOperation extends IntermediateCode implements IntroSpace {
    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(left, temporary ? 1 : 0);
    }

    public enum UnaryOperation {
        POS, NEG, NOT;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    @FunctionalInterface
    private interface Operator {
        int calc(int x);
    }

    public final boolean temporary;
    public final Value left;
    public final UnaryOperation operation;
    public final Value op;

    public AssignUnaryOperation(boolean temporary, Value left,
                                UnaryOperation operation, Value op) {
        this.temporary = temporary;
        this.left = left;
        this.operation = operation;
        this.op = op;
    }

    @Override
    String display() {
        final StringJoiner sj = new StringJoiner(" ");
        sj.add(temporary ? "temp" : "save");
        sj.add(left.toString());
        sj.add("<-");
        sj.add(operation.name().toLowerCase());
        sj.add(op.toString());
        return sj.toString();
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        final Map<UnaryOperation, Operator> operations =
                Collections.unmodifiableMap(new HashMap<UnaryOperation, Operator>() {{
                    put(UnaryOperation.POS, x -> x);
                    put(UnaryOperation.NEG, x -> -x);
                    put(UnaryOperation.NOT, x -> x != 0 ? 0 : 1);
                }});
        if (temporary) machine.createVar(left);
        machine.updateVar(left,
                operations.get(operation).calc(op.get(machine)));
        return next;
    }
}
