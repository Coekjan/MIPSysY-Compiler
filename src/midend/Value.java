package midend;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value = (Value) o;
        return Objects.equals(symbol, value.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    public boolean isGlobal() {
        return symbol.endsWith("%1");
    }

    public boolean isTemp() {
        return Character.isDigit(symbol.charAt(0));
    }
}
