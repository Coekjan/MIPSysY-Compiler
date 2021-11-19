package backend;

public class Imm extends Element {
    public static final Imm ZERO_IMM = new Imm(0);

    public final int value;

    public Imm(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
