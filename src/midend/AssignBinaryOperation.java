package midend;

import utils.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class AssignBinaryOperation extends IntermediateCode implements IntroSpace {
    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(left, temporary ? 1 : 0);
    }

    public enum BinaryOperation {
        ADD, SUB, MUL, DIV, MOD, AND, OR, GT, GE, LT, LE, EQ, NE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    @FunctionalInterface
    private interface Operator {
        int calc(int x, int y);
    }

    public final boolean temporary;
    public final Value left;
    public final BinaryOperation operation;
    public final Value op1;
    public final Value op2;

    public AssignBinaryOperation(boolean temporary, Value left,
                                 BinaryOperation operation, Value op1, Value op2) {
        this.temporary = temporary;
        this.left = left;
        this.operation = operation;
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    String display() {
        final StringJoiner sj = new StringJoiner(" ");
        sj.add(temporary ? "temp" : "save");
        sj.add(left.toString());
        sj.add("<-");
        sj.add(op1.toString());
        sj.add(operation.name().toLowerCase());
        sj.add(op2.toString());
        return sj.toString();
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        final Map<BinaryOperation, Operator> operations =
                Collections.unmodifiableMap(new HashMap<BinaryOperation, Operator>() {{
                    put(BinaryOperation.ADD, Integer::sum);
                    put(BinaryOperation.SUB, (x, y) -> x - y);
                    put(BinaryOperation.MUL, (x, y) -> x * y);
                    put(BinaryOperation.DIV, (x, y) -> x / y);
                    put(BinaryOperation.MOD, (x, y) -> x % y);
                    put(BinaryOperation.AND, (x, y) -> x != 0 && y != 0 ? 1 : 0);
                    put(BinaryOperation.OR, (x, y) -> x != 0 || y != 0 ? 1 : 0);
                    put(BinaryOperation.GT, (x, y) -> x > y ? 1 : 0);
                    put(BinaryOperation.GE, (x, y) -> x >= y ? 1 : 0);
                    put(BinaryOperation.LT, (x, y) -> x < y ? 1 : 0);
                    put(BinaryOperation.LE, (x, y) -> x <= y ? 1 : 0);
                    put(BinaryOperation.EQ, (x, y) -> x == y ? 1 : 0);
                    put(BinaryOperation.NE, (x, y) -> x != y ? 1 : 0);
                }});
        if (temporary) machine.createVar(left);
        final int val1 = op1.get(machine);
        final int val2 = op2.get(machine);
        machine.updateVar(left, operations.get(operation).calc(val1, val2));
        return next;
    }
}
