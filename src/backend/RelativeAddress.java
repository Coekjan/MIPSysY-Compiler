package backend;

public class RelativeAddress extends Address {
    public final Reg base;
    public final int offset;

    RelativeAddress(Reg base, int offset) {
        this.base = base;
        this.offset = offset;
    }

    @Override
    String display() {
        return offset + "(" + base + ")";
    }
}
