package midend;

import java.util.Optional;

public class Return extends IntermediateCode {
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
}
