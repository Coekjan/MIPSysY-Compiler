package midend;

import utils.Pair;

import java.util.*;

import static midend.AssignUnaryOperation.UnaryOperation.NEG;

public class AssignBinaryOperation extends IntermediateCode implements IntroSpace, Definite,
        Usage<AssignBinaryOperation>, ProbablyConst {
    private static final Map<BinaryOperation, Operator> OPERATIONS =
            Collections.unmodifiableMap(new HashMap<BinaryOperation, Operator>() {{
                put(BinaryOperation.ADD, Integer::sum);
                put(BinaryOperation.SUB, (x, y) -> x - y);
                put(BinaryOperation.MUL, (x, y) -> x * y);
                put(BinaryOperation.DIV, (x, y) -> x / y);
                put(BinaryOperation.MOD, (x, y) -> x % y);
                put(BinaryOperation.AND, (x, y) -> x & y);
                put(BinaryOperation.OR, (x, y) -> x | y);
                put(BinaryOperation.GT, (x, y) -> x > y ? 1 : 0);
                put(BinaryOperation.GE, (x, y) -> x >= y ? 1 : 0);
                put(BinaryOperation.LT, (x, y) -> x < y ? 1 : 0);
                put(BinaryOperation.LE, (x, y) -> x <= y ? 1 : 0);
                put(BinaryOperation.EQ, (x, y) -> x == y ? 1 : 0);
                put(BinaryOperation.NE, (x, y) -> x != y ? 1 : 0);
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
        return Arrays.asList(op1, op2);
    }

    @Override
    public AssignBinaryOperation replaceUse(List<Value> uses) {
        return new AssignBinaryOperation(temporary, left, operation, uses.get(0), uses.get(1));
    }

    @Override
    public IntermediateCode simplify() {
        if (op1 instanceof ImmValue && op2 instanceof ImmValue) {
            return new Move(temporary, left,
                    new ImmValue(OPERATIONS.get(operation).calc(((ImmValue) op1).value, ((ImmValue) op2).value)));
        } else if (op1 instanceof ImmValue || op2 instanceof ImmValue) {
            switch (operation) {
                case ADD:
                    if (op1 instanceof ImmValue && ((ImmValue) op1).value == 0) {
                        return new Move(temporary, left, op2);
                    } else if (op2 instanceof ImmValue && ((ImmValue) op2).value == 0) {
                        return new Move(temporary, left, op1);
                    }
                    break;
                case SUB:
                    if (op1 instanceof ImmValue && ((ImmValue) op1).value == 0) {
                        return new AssignUnaryOperation(temporary, left, NEG, op2);
                    } else if (op2 instanceof ImmValue && ((ImmValue) op2).value == 0) {
                        return new Move(temporary, left, op1);
                    }
                    break;
                case MUL:
                    if (op1 instanceof ImmValue) {
                        final int op1Value = ((ImmValue) op1).value;
                        if (op1Value == 0) {
                            return new Move(temporary, left, new ImmValue(0));
                        } else if (op1Value == 1) {
                            return new Move(temporary, left, op2);
                        } else if (op1Value == -1) {
                            return new AssignUnaryOperation(temporary, left, NEG, op2);
                        }
                    } else {
                        return new AssignBinaryOperation(temporary, left, BinaryOperation.MUL, op2, op1).simplify();
                    }
                    return new AssignBinaryOperation(temporary, left, operation, op2, op1);
                case DIV: // TODO: Div Optimization
                    if (op1 instanceof ImmValue && ((ImmValue) op1).value == 0) {
                        return new Move(temporary, left, new ImmValue(0));
                    } else if (op2 instanceof ImmValue) {
                        final int op2Value = ((ImmValue) op2).value;
                        if (op2Value == 1) {
                            return new Move(temporary, left, op1);
                        } else if (op2Value == -1) {
                            return new AssignUnaryOperation(temporary, left, NEG, op1);
                        }
                    }
                    break;
                case MOD:
                    if (op1 instanceof ImmValue && ((ImmValue) op1).value == 0) {
                        return new Move(temporary, left, new ImmValue(0));
                    } else if (op2 instanceof ImmValue && (((ImmValue) op2).value == 1 || ((ImmValue) op2).value == -1)) {
                        return new Move(temporary, left, new ImmValue(0));
                    }
                    break;
                case AND:
                    if (op1 instanceof ImmValue) {
                        if (((ImmValue) op1).value == 0) {
                            return new Move(temporary, left, new ImmValue(0));
                        } else {
                            return new Move(temporary, left, op2);
                        }
                    } else {
                        return new AssignBinaryOperation(temporary, left, BinaryOperation.AND, op2, op1).simplify();
                    }
                case OR:
                    if (op1 instanceof ImmValue) {
                        if (((ImmValue) op1).value == 0) {
                            return new Move(temporary, left, op2);
                        } else {
                            return new Move(temporary, left, new ImmValue(1));
                        }
                    } else {
                        return new AssignBinaryOperation(temporary, left, BinaryOperation.OR, op2, op1).simplify();
                    }
                default:
                    break;
            }
        } else {
            if (op1.equals(op2)) {
                if (operation == BinaryOperation.EQ) {
                    return new Move(temporary, left, new ImmValue(1));
                } else if (operation == BinaryOperation.NE) {
                    return new Move(temporary, left, new ImmValue(0));
                }
            }
        }
        return new AssignBinaryOperation(temporary, left, operation, op1, op2);
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
        if (temporary) machine.createVar(left);
        final int val1 = op1.get(machine);
        final int val2 = op2.get(machine);
        machine.updateVar(left, OPERATIONS.get(operation).calc(val1, val2));
        return next;
    }
}
