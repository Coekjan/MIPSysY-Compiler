package midend;

public class GetInt extends IntermediateCode {
    @Override
    String display() {
        return "call getint";
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.setReturnValue(machine.getInt());
        return next;
    }
}
