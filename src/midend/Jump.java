package midend;

public class Jump extends IntermediateCode {
    public final String label;

    public Jump(String label) {
        this.label = label;
    }

    @Override
    String display() {
        return "jump " + label;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        return labelTable.find(label);
    }
}
