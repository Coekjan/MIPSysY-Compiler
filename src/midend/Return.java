package midend;

import java.util.Collections;
import java.util.Optional;
import java.util.List;

public class Return extends IntermediateCode implements Usage<Return> {
    public static final String RET_SYM = "%ret%";

    public final Value name;

    public Return() {
        this(null);
    }

    public Return(Value name) {
        this.name = name;
    }

    private Optional<String> getName() {
        return name == null ? Optional.empty() : Optional.of(name.toString());
    }

    @Override
    String display() {
        return getName().map(s -> "return " + s).orElse("return");
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        return machine.returnWithValue(name != null ? name.get(machine) : 0);
    }

    @Override
    public List<Value> getUse() {
        return name == null ? Collections.emptyList() : Collections.singletonList(name);
    }

    @Override
    public Return replaceUse(List<Value> uses) {
        return uses.isEmpty() ? new Return() : new Return(uses.get(0));
    }
}
