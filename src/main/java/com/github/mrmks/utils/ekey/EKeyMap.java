package com.github.mrmks.utils.ekey;

import java.util.*;

public class EKeyMap<T extends EKey, K> extends AbstractMap<T, K> {

    private int size;
    private EKeyHandler<T> handler;
    private Object[] vals;

    private static final Object NULL = new Object() {
        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "com.github.mrmks.utils.ekey.EKeyMap.NULL";
        }
    };

    public EKeyMap(EKeyHandler<T> handler) {
        size = 0;
        vals = new Object[handler.size()];
        this.handler = handler;
    }

    private boolean isValidKey(Object key) {
        if (key == null) return false;

        return key instanceof EKey && key.equals(handler.of(((EKey) key).ordinal()));
    }

    private boolean checkType(T key) {
        return key.equals(handler.of(key.ordinal()));
    }

    private Object maskNull(Object value) {
        return (value == null ? NULL : value);
    }

    @SuppressWarnings("unchecked")
    private K unmaskNull(Object value) {
        return (K)(value == NULL ? null : value);
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return isValidKey(key) && vals[((EKey)key).ordinal()] != null;
    }

    @Override
    public boolean containsValue(Object value) {
        value = maskNull(value);

        for (Object val : vals) if (value.equals(val)) return true;
        return false;
    }

    private boolean containsMapping(Object key, Object value) {
        return isValidKey(key) &&
                maskNull(value).equals(vals[((EKey)key).ordinal()]);
    }

    @Override
    public K get(Object key) {
        return (isValidKey(key) ?
                unmaskNull(vals[((EKey)key).ordinal()]) : null);
    }

    @Override
    public K put(T key, K value) {
        if (!checkType(key)) throw new ClassCastException(key.getClass() + " != " + handler.of(0).getClass());

        int index = key.ordinal();
        Object old = vals[index];
        vals[index] = value;
        if (old == null) size ++;
        return unmaskNull(old);
    }

    @Override
    public K remove(Object key) {
        if (!isValidKey(key)) return null;

        int index = ((EKey)key).ordinal();
        Object old = vals[index];
        vals[index] = null;
        if (old != null) size--;
        return unmaskNull(old);
    }

    private boolean removeMapping(Object key, Object value) {
        if (!isValidKey(key)) return false;

        int index = ((EKey)key).ordinal();
        if (maskNull(value).equals(vals[index])) {
            vals[index] = null;
            size --;
            return true;
        }
        return false;
    }

    @Override
    public void putAll(Map<? extends T, ? extends K> m) {
        if (m instanceof EKeyMap) {
            EKeyMap<?, ?> em = (EKeyMap<?, ?>) m;
            if (em.handler != handler) {
                if (em.isEmpty()) return;
                throw new ClassCastException(em.handler.of(0).getClass() + " != " + handler.of(0).getClass());
            }

            for (int i = 0; i < handler.size(); i++) {
                Object emValue = em.vals[i];
                if (emValue != null) {
                    if (vals[i] == null) size++;
                    vals[i] = emValue;
                }
            }
        } else {
            super.putAll(m);
        }
    }

    @Override
    public void clear() {
        Arrays.fill(vals, null);
        size = 0;
    }

    private Set<T> keySet;
    private Collection<K> values;
    private Set<Map.Entry<T, K>> entrySet;

    @Override
    public Set<T> keySet() {
        Set<T> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    private class KeySet extends AbstractSet<T> {
        public Iterator<T> iterator() {
            return new KeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            int oldSize = size;
            EKeyMap.this.remove(o);
            return size != oldSize;
        }
        public void clear() {
            EKeyMap.this.clear();
        }
    }

    @Override
    public Collection<K> values() {
        Collection<K> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    private class Values extends AbstractCollection<K> {
        public Iterator<K> iterator() {
            return new ValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public boolean remove(Object o) {
            o = maskNull(o);

            for (int i = 0; i < vals.length; i++) {
                if (o.equals(vals[i])) {
                    vals[i] = null;
                    size--;
                    return true;
                }
            }
            return false;
        }
        public void clear() {
            EKeyMap.this.clear();
        }
    }

    @Override
    public Set<Entry<T,K>> entrySet() {
        Set<Map.Entry<T,K>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    private class EntrySet extends AbstractSet<Map.Entry<T,K>> {
        public Iterator<Map.Entry<T,K>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
            return containsMapping(entry.getKey(), entry.getValue());
        }
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
            return removeMapping(entry.getKey(), entry.getValue());
        }
        public int size() {
            return size;
        }
        public void clear() {
            EKeyMap.this.clear();
        }
        public Object[] toArray() {
            return fillEntryArray(new Object[size]);
        }
        @SuppressWarnings("unchecked")
        public <V> V[] toArray(V[] a) {
            int size = size();
            if (a.length < size)
                a = (V[])java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), size);
            if (a.length > size)
                a[size] = null;
            return (V[]) fillEntryArray(a);
        }
        private Object[] fillEntryArray(Object[] a) {
            int j = 0;
            for (int i = 0; i < vals.length; i++)
                if (vals[i] != null)
                    a[j++] = new AbstractMap.SimpleEntry<>(
                            handler.of(i), unmaskNull(vals[i]));
            return a;
        }
    }

    private abstract class EKeyMapIterator<V> implements Iterator<V> {
        int index = 0;
        int lastIndex = -1;

        @Override
        public boolean hasNext() {
            while (index < vals.length && vals[index] == null) index++;
            return index != vals.length;
        }

        @Override
        public void remove() {
            if (lastIndex < 0) throw new IllegalStateException();

            if (vals[lastIndex] != null) {
                vals[lastIndex] = null;
                size--;
            }
            lastIndex = -1;
        }
    }

    private class KeyIterator extends EKeyMapIterator<T> {
        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();

            lastIndex = index++;
            return handler.of(lastIndex);
        }
    }

    private class ValueIterator extends EKeyMapIterator<K> {
        @Override
        public K next() {
            if (!hasNext()) throw new NoSuchElementException();

            lastIndex = index++;
            return unmaskNull(vals[lastIndex]);
        }
    }

    private class EntryIterator extends EKeyMapIterator<Map.Entry<T, K>> {
        private Entry lastEntry;

        public Map.Entry<T,K> next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastEntry = new Entry(index++);
            return lastEntry;
        }

        public void remove() {
            lastIndex = (null == lastEntry ? -1 : lastEntry.index);
            super.remove();
            lastEntry.index = lastIndex;
            lastEntry = null;
        }

        private class Entry implements Map.Entry<T,K> {
            private int index;
            private Entry(int index) {this.index = index;}

            private void checkIndexForEntryUse(){
                if (index < 0)
                    throw new IllegalStateException("Entry was removed");
            }
            public T getKey() {
                checkIndexForEntryUse();
                return handler.of(index);
            }

            public K getValue() {
                checkIndexForEntryUse();
                return unmaskNull(vals[index]);
            }

            public K setValue(K value) {
                checkIndexForEntryUse();
                K old = unmaskNull(vals[index]);
                vals[index] = maskNull(value);
                return old;
            }

            public boolean equals(Object o) {
                if (index < 0) return o == this;

                if (!(o instanceof Map.Entry)) return false;

                Map.Entry<?,?> e = (Map.Entry<?, ?>) o;
                K selfV = unmaskNull(vals[index]);
                Object hisValue = e.getValue();
                return (e.getKey() == handler.of(index) && (
                        Objects.equals(selfV, hisValue)
                        ));
            }

            public int hashCode() {
                if (index < 0) return super.hashCode();

                return entryHashCode(index);
            }

            public String toString() {
                if (index < 0) return super.toString();

                return handler.of(index) + "=" + unmaskNull(vals[index]);
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof EKeyMap)
            return equals((EKeyMap<?, ?>) o);
        if (!(o instanceof Map)) return false;

        Map<?,?> m = (Map<?,?>)o;
        if (size != m.size())
            return false;

        for (int i = 0; i < handler.size(); i++) {
            if (null != vals[i]) {
                T key = handler.of(i);
                K value = unmaskNull(vals[i]);
                if (null == value) {
                    if (!((null == m.get(key)) && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        }

        return true;
    }

    private boolean equals(EKeyMap<?, ?> em) {
        if (em.handler != handler)
            return size == 0 && em.size == 0;

        for (int i = 0; i < handler.size(); i++) {
            Object ourValue = vals[i];
            Object hisValue  = em.vals[i];
            if (!Objects.equals(hisValue, ourValue))
                return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 0;

        for (int i = 0; i < handler.size(); i++) {
            if (null != vals[i]) h += entryHashCode(i);
        }

        return h;
    }

    private int entryHashCode(int index) {
        return (handler.of(index).hashCode() ^ vals[index].hashCode());
    }
}
