package midend;

public abstract class Value {
    public final String symbol;

    public Value(String symbol) {
        this.symbol = symbol;
    }

    public static  Value pack(Value v1, Value v2, String val) {
        if (v1 instanceof AddrValue || v2 instanceof AddrValue) {
            return new AddrValue(val);
        }
        return new WordValue(val);
    }

    @Override
    public String toString() {
        return symbol;
    }

    abstract int get(IntermediateVirtualMachine machine);
}
