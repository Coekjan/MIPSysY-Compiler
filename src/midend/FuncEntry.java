package midend;

public class FuncEntry extends IntermediateCode {
    public final String label;
    public final int paraNum;

    public FuncEntry(String label, int paraNum) {
        this.label = label;
        this.paraNum = paraNum;
    }

    @Override
    String display() {
        return label + "(para_num: " + paraNum + ")";
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        return next;
    }
}
