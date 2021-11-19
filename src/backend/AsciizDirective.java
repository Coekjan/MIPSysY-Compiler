package backend;

public class AsciizDirective extends Directive {
    public final String string;

    public AsciizDirective(String label, String string) {
        super(label);
        this.string = string;
    }

    @Override
    String stringify() {
        return label + ": .asciiz \"" + string + "\"";
    }
}
