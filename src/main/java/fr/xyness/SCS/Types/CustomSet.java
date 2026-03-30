package fr.xyness.SCS.Types;

import java.util.Collection;
import java.util.HashSet;

public class CustomSet<T> extends HashSet<T> {

    private static final long serialVersionUID = 1L;

    public CustomSet(Collection<? extends T> c) {
        super(c);
    }

    public CustomSet() {
    }

    /**
     * Checks if this set contains the specified element.
     * For String elements, the comparison is case-insensitive.
     */
    @Override
    public boolean contains(Object o) {
        if (this.isEmpty()) return false;
        if (o instanceof String && this.iterator().next() instanceof String) {
            String str = (String) o;
            return this.stream().anyMatch(s -> ((String) s).equalsIgnoreCase(str));
        }
        return super.contains(o);
    }
}
