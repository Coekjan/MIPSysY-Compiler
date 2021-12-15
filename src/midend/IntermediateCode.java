package midend;

import utils.LinkedNode;

import java.util.Objects;

public abstract class IntermediateCode extends LinkedNode<IntermediateCode> {
    private static int count = 0;
    private final int id = count++;

    abstract String display();

    abstract IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable);

    @Override
    public String toString() {
        return display();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntermediateCode that = (IntermediateCode) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
