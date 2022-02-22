package com.github.mrmks.utils.ekey;

import java.util.*;

public class EKeyIntMap<K extends EKey> extends AbstractMap<K, Integer> {

    private int size;
    private EKeyHandler<K> handler;
    private int[] vals;
    private boolean[] valsF;

    public EKeyIntMap(EKeyHandler<K> handler) {
        size = 0;
        vals = new int[handler.size()];
        valsF = new boolean[handler.size()];
        Arrays.fill(valsF, false);
        this.handler = handler;
    }

    private boolean isValidKey(Object key) {
        if (key == null) return false;

        return key instanceof EKey && key.equals(handler.of(((EKey) key).ordinal()));
    }

    private boolean isValidValue(Object value) {
        return value instanceof Integer;
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
        return isValidKey(key) && valsF[((EKey)key).ordinal()];
    }

    @Override
    public boolean containsValue(Object value) {
        if (!isValidValue(value)) return false;

        return containsValueInt((Integer) value);
    }

    public boolean containsValueInt(int value) {
        for (int val : vals) if (val == value) return true;
        return false;
    }

    private boolean containsMapping(Object key, Object value) {
        if (!isValidValue(key) || !isValidValue(value)) return false;
        int index = ((EKey)key).ordinal();
        return valsF[index] && vals[index] == (Integer)value;
    }

    @Override
    public Integer get(Object key) {
        return getInt(key);
    }

    public int getInt(Object key) {
        return isValidKey(key) ? vals[((EKey)key).ordinal()] : 0;
    }

    @Override
    public Integer put(K key, Integer value) {
        return putInt(key, value);
    }

    public int putInt(K key, int value) {
        if (!isValidKey(key)) throw new ClassCastException(key.getClass() + " != " + handler.of(0).getClass());

        int index = key.ordinal();
        int old = vals[index];
        vals[index] = value;
        if (!valsF[index]) {
            size ++;
            valsF[index] = true;
        }
        return old;
    }

    @Override
    public Integer remove(Object key) {
        if (!isValidKey(key)) return null;
        return remove0((EKey) key);
    }

    public int removeInt(Object key) {
        if (!isValidKey(key)) return 0;
        return remove0((EKey) key);
    }

    private int remove0(EKey key) {
        int index = key.ordinal();
        if (valsF[index]) {
            size --;
            valsF[index] = false;
        }
        return vals[index];
    }

    private boolean removeMapping(Object key, Object value) {
        if (!isValidKey(key) || !isValidValue(value)) return false;

        int index = ((EKey) key).ordinal();
        if (valsF[index] && vals[index] == (Integer)value) {
            valsF[index] = false;
            return true;
        }
        return false;
    }

    @Override
    public void putAll(Map<? extends K, ? extends Integer> m) {
        if (m instanceof EKeyIntMap) {
            EKeyIntMap<?> em = (EKeyIntMap<?>) m;
            if (em.handler != handler) {
                if (em.isEmpty()) return;
                throw new ClassCastException(em.handler.of(0).getClass() + " != " + handler.of(0).getClass());
            }

            for (int i = 0; i < handler.size(); i++) {
                if (em.valsF[i]) {
                    vals[i] = em.vals[i];
                    if (!valsF[i]) {
                        size++;
                        valsF[i] = true;
                    }
                }
            }
        } else {
            super.putAll(m);
        }
    }

    @Override
    public void clear() {
        Arrays.fill(valsF,false);
        size = 0;
    }

    private Set<K> keySet;
    private Collection<Integer> values;
    private Set<Map.Entry<K, Integer>> entrySet;

    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new KeySet();
        }
        return keySet;
    }

    private class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            int oldSize = size;
            EKeyIntMap.this.remove(o);
            return size != oldSize;
        }

        public void clear() {
            EKeyIntMap.this.clear();
        }
    }

    @Override
    public Collection<Integer> values() {
        if (values == null) values = new Values();
        return values;
    }

    private class Values extends AbstractCollection<Integer> {
        @Override
        public Iterator<Integer> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return size;
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public boolean remove(Object o) {
            if (isValidValue(o)) {
                int iVal = (Integer) o;
                for (int i = 0; i < vals.length; i++) {
                    if (valsF[i] && iVal == vals[i]) {
                        valsF[i] = false;
                        size--;
                        return true;
                    }
                }
            }
            return false;
        }

        public void clear() {
            EKeyIntMap.this.clear();
        }
    }

    @Override
    public Set<Entry<K, Integer>> entrySet() {
        if (entrySet == null) entrySet = new EntrySet();
        return entrySet;
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, Integer>> {
        @Override
        public Iterator<Entry<K, Integer>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return size;
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;

            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            return containsMapping(entry.getKey(), entry.getValue());
        }
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            return removeMapping(entry.getKey(), entry.getValue());
        }
        public void clear() {
            EKeyIntMap.this.clear();
        }
        public Object[] toArray() {return fillEntryArray(new Object[size]);}
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
                if (valsF[i])
                    a[j++] = new AbstractMap.SimpleEntry<>(
                            handler.of(i), vals[i]);
            return a;
        }
    }

    private abstract class EKeyIntMapIterator<V> implements Iterator<V> {
        int index = 0;
        int lastIndex = -1;

        @Override
        public boolean hasNext() {
            while (index < vals.length && !valsF[index]) index++;
            return index != vals.length;
        }

        @Override
        public void remove() {
            if (lastIndex < 0) throw new IllegalStateException();

            if (valsF[lastIndex]) {
                valsF[lastIndex] = false;
                size--;
            }
            lastIndex = -1;
        }
    }

    private class KeyIterator extends EKeyIntMapIterator<K> {
        @Override
        public K next() {
            if (!hasNext()) throw new NoSuchElementException();

            lastIndex = index++;
            return handler.of(lastIndex);
        }
    }

    private class ValueIterator extends EKeyIntMapIterator<Integer> {
        @Override
        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();

            lastIndex = index++;
            return vals[lastIndex];
        }
    }

    private class EntryIterator extends EKeyIntMapIterator<Map.Entry<K, Integer>> {
        private Entry lastEntry;

        @Override
        public Map.Entry<K, Integer> next() {
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

        private class Entry implements Map.Entry<K, Integer> {
            private int index;
            private Entry(int index) {
                this.index = index;
            }

            private void checkIndexForEntryUse() {
                if (index < 0)
                    throw new IllegalStateException("Entry was removed");
            }
            public K getKey() {
                checkIndexForEntryUse();
                return handler.of(index);
            }
            public Integer getValue() {
                checkIndexForEntryUse();
                return vals[index];
            }
            public Integer setValue(Integer value) {
                checkIndexForEntryUse();
                Integer old = vals[index];
                vals[index] = value;
                return old;
            }
            public boolean equals(Object o) {
                if (index < 0) return o == this;
                if (!(o instanceof Map.Entry)) return false;

                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                if (!isValidValue(e.getValue())) return false;
                int ourV = vals[index];
                int hisV = (Integer) e.getValue();
                return (e.getKey() == handler.of(index) && (ourV == hisV));
            }
            public int hashCode() {
                if (index < 0) return super.hashCode();
                return entryHashCode(index);
            }

            public String toString() {
                if (index < 0) return super.toString();
                return handler.of(index) + "=" + vals[index];
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof EKeyIntMap) {
            return equals((EKeyIntMap<?>) o);
        }
        if (!(o instanceof Map)) return false;

        Map<?,?> m = (Map<?, ?>) o;
        if (size != m.size()) return false;
        for (int i = 0; i < handler.size(); i++) {
            if (valsF[i]) {
                K key = handler.of(i);
                Integer value = vals[i];
                if (!value.equals(m.get(key)))
                    return false;
            }
        }

        return true;
    }

    private boolean equals(EKeyIntMap<?> em) {
        if (em.handler != handler) return size == 0 && em.size == 0;

        for (int i = 0; i < handler.size(); i++) {
            if (valsF[i] && em.valsF[i]) {
                if (vals[i] != em.vals[i]) return false;
            } else if (valsF[i] || em.valsF[i]) return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        for (int i = 0; i < handler.size(); i++) if (valsF[i]) h+= entryHashCode(i);
        return h;
    }

    private int entryHashCode(int i) {
        return (handler.of(i).hashCode() ^ vals[i]);
    }
}
