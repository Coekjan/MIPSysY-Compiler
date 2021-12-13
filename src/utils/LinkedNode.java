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

    // replace with a list
    public T replaceWith(T node /* head node of a list */) {
        prev.link(node);
        T p = node;
        while (p.next != null) {
            p = p.next;
        }
        if (next != null) p.link(next);
        return p;
    }

    public void remove() {
        if (next != null) prev.link(next);
        else prev.next = null;
    }
}
