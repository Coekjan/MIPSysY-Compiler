package midend;

public class CallFunction extends IntermediateCode {
    public final String label;

    public CallFunction(String label) {
        this.label = label;
    }

    @Override
    String display() {
        return "call " + label;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        final IntermediateCode fun = labelTable.find(label);
        machine.callFuncWithLink(next);
        return fun;
    }
}
