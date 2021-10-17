package exceptions;

public class SysYException extends Exception {

    public enum Code {
        a, b, c, d, e, f, g, h, i, j, k, l, m, u
    }

    public final Code code;

    public SysYException(Code code) {
        this.code = code;
    }

    public String stringify() {
        return code.name();
    }
}
