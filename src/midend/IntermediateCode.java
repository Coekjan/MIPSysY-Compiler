package midend;

import utils.LinkedNode;

public abstract class IntermediateCode extends LinkedNode<IntermediateCode> {
    abstract String display();

    abstract IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable);

    @Override
    public String toString() {
        return display();
    }
}
