package midend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class AssignUnaryOperation extends IntermediateCode {
    public enum UnaryOperation {
        POS, NEG, NOT
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
        if (temporary) machine.createVar(left.symbol);
        machine.updateVar(left.symbol,
                operations.get(operation).calc(op.get(machine)));
        return next;
    }
}
