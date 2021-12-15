package midend;

import java.util.Collections;
import java.util.List;

public class GetInt extends IntermediateCode implements Definite, Assignment {
    @Override
    String display() {
        return "call getint";
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.setReturnValue(machine.getInt());
        return next;
    }

    @Override
    public Value getDef() {
        return new WordValue(Return.RET_SYM);
    }

    @Override
    public Value left() {
        return getDef();
    }

    @Override
    public List<Value> right() {
        return Collections.emptyList();
    }
}
