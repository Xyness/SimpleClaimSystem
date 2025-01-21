package fr.xyness.SCS.Types;

import java.util.Collection;
import java.util.HashSet;

public class CustomSet<T> extends HashSet<T> {
	
    private static final long serialVersionUID = 1L;
    
    // Constructor accepting a Collection
    public CustomSet(Collection<? extends T> c) {
        super(c);
    }
    
    public CustomSet() {
    	
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof String && this.iterator().next() instanceof String) {
            String str = (String) o;
            return this.stream().anyMatch(s -> ((String) s).equalsIgnoreCase(str));
        }
        return super.contains(o);
    }
}
