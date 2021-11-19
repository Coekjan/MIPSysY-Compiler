package midend;

import utils.Pair;

public class ParameterFetch extends IntermediateCode implements IntroSpace {
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
        machine.createVar(name);
        machine.updateVar(name, machine.popArg());
        return next;
    }

    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(name, 1);
    }
}
