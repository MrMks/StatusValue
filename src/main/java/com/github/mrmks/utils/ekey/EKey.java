package com.github.mrmks.utils.ekey;

public abstract class EKey {
    protected final String name;
    protected final int ordinal;

    protected EKey(String str, int index){
        this.name = str;
        this.ordinal = index;
    }

    public final String name() {
        return name;
    }

    public final int ordinal() {
        return ordinal;
    }

    public final boolean match(String k) {
        return name.equals(k);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EKey)) return false;

        EKey e = (EKey) o;
        if (e.getClass() != getClass()) return false;
        return name.equals(e.name) && ordinal == e.ordinal;
    }

    public int hashCode() {
        return name.hashCode() * 31 + ordinal;
    }

    @Override
    public String toString() {
        return "EKey{" +
                "name='" + name + '\'' +
                ", ordinal=" + ordinal +
                '}';
    }
}
