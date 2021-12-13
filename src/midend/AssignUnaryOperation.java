package midend;

import utils.Pair;

import java.util.*;

public class AssignUnaryOperation extends IntermediateCode implements IntroSpace, Definite, Usage<AssignUnaryOperation>, ProbablyConst {
    private static final Map<UnaryOperation, Operator> OPERATIONS =
            Collections.unmodifiableMap(new HashMap<UnaryOperation, Operator>() {{
                put(UnaryOperation.POS, x -> x);
                put(UnaryOperation.NEG, x -> -x);
                put(UnaryOperation.NOT, x -> x != 0 ? 0 : 1);
            }});

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
        return Collections.singletonList(op);
    }

    @Override
    public AssignUnaryOperation replaceUse(List<Value> uses) {
        return new AssignUnaryOperation(temporary, left, operation, uses.get(0));
    }

    @Override
    public IntermediateCode simplify() {
        if (op instanceof ImmValue) {
            return new Move(temporary, left, new ImmValue(OPERATIONS.get(operation).calc(((ImmValue) op).value)));
        } else if (operation == UnaryOperation.POS) {
            return new Move(temporary, left, op);
        }
        return new AssignUnaryOperation(temporary, left, operation, op);
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
        if (temporary) machine.createVar(left);
        machine.updateVar(left, OPERATIONS.get(operation).calc(op.get(machine)));
        return next;
    }
}
