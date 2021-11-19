package backend;

public class AbsoluteAddress extends Address {
    public final String label;

    public AbsoluteAddress(String label) {
        this.label = label;
    }

    @Override
    String display() {
        return label;
    }
}
