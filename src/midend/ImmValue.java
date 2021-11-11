package midend;

public class ImmValue extends Value {
    public final int value;

    public ImmValue(int value) {
        super(String.valueOf(value));
        this.value = value;
    }

    @Override
    int get(IntermediateVirtualMachine machine) {
        return value;
    }
}
