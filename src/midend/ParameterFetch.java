package midend;

public class ParameterFetch extends IntermediateCode {
    public final Value name;

    public ParameterFetch(Value name) {
        this.name = name;
    }

    @Override
    String display() {
        return "para " + name.toString();
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.updateVar(name.symbol, machine.popArg());
        return next;
    }
}
