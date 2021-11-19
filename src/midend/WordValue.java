package midend;

public class WordValue extends Value {
    public WordValue(String symbol) {
        super(symbol);
    }

    @Override
    public String toString() {
        return "$" + super.toString();
    }

    @Override
    int get(IntermediateVirtualMachine machine) {
        return machine.findVar(this);
    }
}
