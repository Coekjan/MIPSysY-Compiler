package utils;

import java.util.Objects;

public class Pair<T extends Comparable<T>> implements Comparable<Pair<T>> {
    public static <S extends Comparable<S>> Pair<S> of(S f, S s) {
        return new Pair<>(f, s);
    }

    public final T first;
    public final T second;

    public Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int compareTo(Pair<T> o) {
        int f = first.compareTo(o.first);
        return f != 0 ? f : second.compareTo(o.second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pair<?> pair = (Pair<?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "<" + first.toString() + ", " + second.toString() + ">";
    }
}
