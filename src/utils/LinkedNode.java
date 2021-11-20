package utils;

public abstract class LinkedNode<T extends LinkedNode<T>> {
    protected T next;
    protected T prev;

    @SuppressWarnings("unchecked")
    public T link(T node) {
        node.prev = (T) this;
        this.next = node;
        return node;
    }

    public T getPrev() {
        return prev;
    }

    public T getNext() {
        return next;
    }
}
