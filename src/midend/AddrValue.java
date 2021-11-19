package midend;

public class AddrValue extends Value {
    public AddrValue(String symbol) {
        super(symbol);
    }

    @Override
    public String toString() {
        return "&" + super.toString();
    }

    @Override
    int get(IntermediateVirtualMachine machine) {
        return machine.findVar(this);
    }
}
