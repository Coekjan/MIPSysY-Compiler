package midend;

import java.util.Collections;
import java.util.List;

public class PushArgument extends IntermediateCode implements Usage<PushArgument> {
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

    @Override
    public List<Value> getUse() {
        return Collections.singletonList(name);
    }

    @Override
    public PushArgument replaceUse(List<Value> uses) {
        return new PushArgument(uses.get(0));
    }
}
