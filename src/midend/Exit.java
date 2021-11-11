package midend;

public class Exit extends IntermediateCode {
    @Override
    String display() {
        return "exit";
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        return this;
    }
}
