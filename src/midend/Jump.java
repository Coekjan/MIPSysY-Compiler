package midend;

public class Jump extends IntermediateCode implements ProbablyConst {
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

    private boolean isBranchToNext(LabelTable lt) {
        final IntermediateCode target = lt.find(label);
        IntermediateCode p = target.getPrev();
        while (p != null) {
            if (!(p instanceof Nop)) {
                break;
            }
            p = p.getPrev();
        }
        return p == this;
    }

    @Override
    public IntermediateCode simplify(LabelTable lt) {
        if (isBranchToNext(lt)) {
            return new Nop();
        } else {
            return new Jump(label);
        }
    }
}
