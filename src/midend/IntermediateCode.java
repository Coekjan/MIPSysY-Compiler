package midend;

public abstract class IntermediateCode {
    protected IntermediateCode next;
    protected IntermediateCode prev;

    abstract String display();

    abstract IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable);

    public void link(IntermediateCode inst) {
        inst.prev = this;
        next = inst;
    }

    public IntermediateCode getNext() {
        return next;
    }

    @Override
    public String toString() {
        return display();
    }
}
