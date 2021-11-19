package backend;

public abstract class Directive extends MIPSCode {
    public final String label;

    public Directive(String label) {
        this.label = label;
    }
}
