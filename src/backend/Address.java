package backend;

public abstract class Address {
    abstract String display();

    @Override
    public String toString() {
        return display();
    }
}
