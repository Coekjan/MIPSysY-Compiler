package midend;

public class Nop extends IntermediateCode {
    @Override
    String display() {
        return "nop";
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        return next;
    }
}
