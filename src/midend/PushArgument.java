package midend;

public class PushArgument extends IntermediateCode {
    public final Value name;

    public PushArgument(Value name) {
        this.name = name;
    }

    @Override
    String display() {
        return "push " + name;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.pushArg(name.get(machine));
        return next;
    }
}
