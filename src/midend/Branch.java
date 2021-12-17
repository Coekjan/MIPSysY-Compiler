package midend;

import java.util.*;

public class Branch extends IntermediateCode implements ProbablyConst, ProbablyCombinable, Usage<Branch> {
    private static final Map<BranchOption, Operator> OPTIONS =
            Collections.unmodifiableMap(new HashMap<BranchOption, Operator>() {{
                put(BranchOption.EQ, (x, y) -> x == y);
                put(BranchOption.NE, (x, y) -> x != y);
                put(BranchOption.GT, (x, y) -> x > y);
                put(BranchOption.GE, (x, y) -> x >= y);
                put(BranchOption.LT, (x, y) -> x < y);
                put(BranchOption.LE, (x, y) -> x <= y);
            }});

    @Override
    public boolean isCombinable() {
        return false;
    }

    @Override
    public IntermediateCode combine() {
        return this;
    }

    @FunctionalInterface
    private interface Operator {
        boolean test(int x, int y);
    }

    public enum BranchOption {
        EQ, NE, GT, GE, LT, LE;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public final Value value1;
    public final Value value2;
    public final BranchOption option;
    public final String label;

    public Branch(Value condition, String label) {
        this(BranchOption.NE, condition, new ImmValue(0), label);
    }

    public Branch(BranchOption option, Value value1, Value value2, String label) {
        this.value1 = value1;
        this.value2 = value2;
        this.option = option;
        this.label = label;
    }

    @Override
    String display() {
        return "branch " + label + " if " + value1 + " " + option + " " + value2;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        final int val1 = value1.get(machine);
        final int val2 = value2.get(machine);
        if (OPTIONS.get(option).test(val1, val2)) {
            return labelTable.find(label);
        }
        return next;
    }

    @Override
    public List<Value> getUse() {
        return Arrays.asList(value1, value2);
    }

    @Override
    public Branch replaceUse(List<Value> uses) {
        return new Branch(option, uses.get(0), uses.get(1), label);
    }

    @Override
    public IntermediateCode simplify() {
        if (value1 instanceof ImmValue && value2 instanceof ImmValue) {
            if (OPTIONS.get(option).test(((ImmValue) value1).value, ((ImmValue) value2).value)) {
                return new Jump(label);
            } else {
                return new Nop();
            }
        } else if (value1 instanceof ImmValue) {
            switch (option) {
                case EQ:
                case NE:
                    return new Branch(option, value2, value1, label);
                case GE:
                    return new Branch(BranchOption.LE, value2, value1, label);
                case GT:
                    return new Branch(BranchOption.LT, value2, value1, label);
                case LE:
                    return new Branch(BranchOption.GE, value2, value1, label);
                case LT:
                    return new Branch(BranchOption.GT, value2, value1, label);
            }
        }
        return new Branch(option, value1, value2, label);
    }
}
