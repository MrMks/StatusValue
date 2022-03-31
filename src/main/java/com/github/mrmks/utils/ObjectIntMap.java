package com.github.mrmks.utils;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class ObjectIntMap<K> implements Iterable<ToIntEntry<K>> {

    protected static final int LINK_GAP = 8;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 16;

    protected int cap;
    protected int size;
    protected int linkCount;
    private int modCount = 0;

    protected Entry<K>[] data;

    public ObjectIntMap() {
        this.cap = MINIMUM_CAPACITY;
    }

    public ObjectIntMap(int cap) {
        this.cap = tableSizeOf(cap);
    }

    static int tableSizeOf(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? MINIMUM_CAPACITY : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    protected static int indexOf(int h, int cap) {
        return h & (cap - 1);
    }

    @SuppressWarnings("unchecked")
    protected Entry<K>[] createData(int cap) {
        return new Entry[cap];
    }

    protected void copyLink(Entry<K> s, Entry<K> t) {
        t.gap = s.gap;
        t.gapNext = s.gapNext;
        s.gap = 0;
        s.gapNext = s;
    }

    protected void leftLink(Entry<K> h) {
        if (h.gapNext != h) {
            Entry<K> n;
            if (h.gap < LINK_GAP) {
                if ((n = h.gapNext.next) != null) h.gapNext = n;
            } else {
                h.gapNext = h.gapNext.prev;
                do {
                    n = h.next;
                    copyLink(n, h);
                    h = h.gapNext;
                } while (h.gap >= LINK_GAP);
                n = h.next;
                copyLink(n, h);
            }
            h.gap += 1;
        } else {
            if (h.next != null) {
                h.gapNext = h.next;
                h.gap = 1;
            }
        }
    }

    protected void rightLink(Entry<K> h) {
        if (h.gapNext != h) {
            Entry<K> n = h.gapNext.next;
            while (n != null) {
                h.gapNext = n;
                copyLink(n.prev, n);
                h = n;
                n = h.gapNext.next;
            }
            if (h.gapNext != h) h.gap --;
        }
    }

    public int put(K key, int val) {
        if (data == null) data = createData(cap);
        else {
            if (linkCount > (cap >>> 2) && size > 16 && cap < MAXIMUM_CAPACITY && size > cap && size < Integer.MAX_VALUE) {
                Entry<K>[] old = data;
                int oldCap = cap;
                cap <<= 1;
                data = createData(cap);
                linkCount = 0;
                Entry<K> e, p, l = null, r = null, lh = null, rh = null;
                for (int i = 0; i < oldCap; i++) {
                    e = old[i];
                    while (e != null) {
                        p = e.next;
                        if (indexOf(e.h, cap) == i) {
                            if (l == null) {
                                (lh = l = e.prev = e).gap = 0;
                                lh.gapNext = lh;
                            } else {
                                e.end(l.prev, l);
                                e.gap = 0;
                                e.gapNext = e;
                                lh.gap += 1;
                                lh.gapNext = e;
                                if (lh.gap == LINK_GAP) (lh = e).gap = 0;
                                linkCount += 1;
                            }
                        } else {
                            if (r == null) {
                                (rh = r = e.prev = e).gap = 0;
                                rh.gapNext = rh;
                            } else {
                                e.end(r.prev, r);
                                e.gap = 0;
                                e.gapNext = e;
                                rh.gap += 1;
                                rh.gapNext = e;
                                if (rh.gap == LINK_GAP) (rh = e).gap = 0;
                                linkCount += 1;
                            }
                        }
                        e = p;
                    }
                    if (l != null) (data[i] = l).prev.next = null;
                    if (r != null) (data[i + oldCap] = r).prev.next = null;
                    l = r = lh = rh = null;
                }
            }
        }

        int h = keyHash(key);
        int i = indexOf(h, cap), r = 0;
        Entry<K> head = data[i];

        boolean find = false, link = false;
        if (head == null) {
            data[i] = new Entry<>(key, h, val);
        } else {
            Entry<K> end = head.prev;
            int cmp = keyCompare(key, head.key);
            if (cmp == 0) {
                find = true;
                r = head.setValue(val);
            } else if (cmp < 0) {
                link = true;
                Entry<K> e = new Entry<>(key, h, val);
                e.head(head, end);

                copyLink(head, e);
                leftLink(e);
                data[i] = e;
            } else {
                if (end == head || end == null) {
                    link = true;
                    Entry<K> entry = new Entry<>(key, h, val);
                    entry.end(head, head);
                    while (head.gap >= LINK_GAP) head = head.gapNext;
                    leftLink(head);
                } else {
                    cmp = keyCompare(key, end.key);
                    if (cmp == 0) {
                        find = true;
                        r = end.setValue(val);
                    } else if (cmp > 0) {
                        link = true;
                        Entry<K> e = new Entry<>(key, h, val);
                        e.end(end, head);
                        while (head.gap >= LINK_GAP) head = head.gapNext;
                        leftLink(head);
                    } else {
                        Entry<K> po = head;
                        do {
                            head = po;
                            po = po.gapNext;
                            cmp = keyCompare(key, po.key);
                        } while (po != end && cmp > 0);
                        if (cmp == 0) {
                            find = true;
                            r = po.setValue(val);
                        } else {
                            Entry<K> pe = po;
                            po = head;
                            do {
                                po = po.next;
                                cmp = keyCompare(key, po.key);
                            } while (pe != po && cmp > 0);
                            if (cmp == 0) {
                                find = true;
                                r = po.setValue(val);
                            } else {
                                link = true;
                                Entry<K> entry = new Entry<>(key, h, val);
                                entry.insert(po.prev, po);
                                leftLink(head);
                            }
                        }
                    }
                }
            }
        }
        ++modCount;
        if (!find) {
            size++;
        }
        if (link) linkCount++;
        return r;
    }

    public int getOrDefault(K key, int def) {
        Entry<K> entry = find(key);
        return entry == null ? def : entry.val;
    }

    public int remove(K key) {
        if (data == null) return 0;
        int h = keyHash(key);
        int i = indexOf(h, cap);
        Entry<K> head = data[i];
        boolean find = false, link = false;
        int r = 0;
        if (head != null) {
            if (head.prev == head) {
                if (head.h == h && keyCompare(head.key, key) == 0) {
                    data[i] = null;
                    find = true;
                    r = head.val;
                }
            } else {
                int cmp = keyCompare(key, head.key);
                if (cmp == 0) {
                    (data[i] = head.next).prev = head.prev;
                    copyLink(head, head.next);
                    rightLink(head.next);
                    find = link = true;
                    r = head.val;
                } else if (cmp > 0) {
                    Entry<K> end = head.prev;
                    cmp = keyCompare(key, end.key);
                    if (cmp == 0) {
                        (head.prev = end.prev).next = null;
                        while (head.gapNext.gapNext != end) head = head.gapNext;
                        head.gapNext = end.prev;
                        head.gap -= 1;
                        find = link = true;
                        r = end.val;
                    } else if (cmp < 0) {
                        Entry<K> po = head;
                        do {
                            head = po;
                            po = po.gapNext;
                            cmp = keyCompare(key, po.key);
                        } while (po != end && cmp > 0);
                        if (cmp == 0) {
                            po.drop();
                            copyLink(po, po.next);
                            rightLink(po.next);
                            find = link = true;
                            r = po.val;
                        } else {
                            Entry<K> pe = po;
                            po = head;
                            do {
                                po = po.next;
                                cmp = keyCompare(key, po.key);
                            } while (pe != po && cmp > 0);
                            if (cmp == 0) {
                                po.drop();
                                rightLink(head);
                                find = link = true;
                                r = po.val;
                            }
                        }
                    }
                }
            }
        }
        ++modCount;
        if (find) size--;
        if (link) linkCount--;
        return r;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containKey(K key) {
        return find(key) != null;
    }

    public void clear() {
        size = 0;
        linkCount = 0;
        for (int i = 0; i < (data == null ? 0 : data.length); i++) {
            Entry<K> e = data[i], n;
            if (e != null) {
                data[i] = null;
                do {
                    n = e.next;
                    e.prev = e.next = e.gapNext = null;
                } while ((e = n) != null);
            }
        }
        modCount++;
    }

    protected Entry<K> find(K key) {
        int h = keyHash(key);
        int i = indexOf(h, cap);
        Entry<K> head = data == null ? null : data[i];
        if (head != null) {
            int cmp = keyCompare(key, head.key);
            if (cmp == 0) return head;
            else if (cmp > 0) {
                Entry<K> end = head.prev;
                cmp = keyCompare(key, end.key);
                if (cmp == 0) return end;
                else if (cmp < 0) {
                    Entry<K> po = head;
                    do {
                        head = po;
                        po = po.gapNext;
                        cmp = keyCompare(key, po.key);
                    } while (po != end && cmp > 0);
                    if (cmp == 0) return po;
                    else {
                        Entry<K> pe = po;
                        po = head;
                        do {
                            po = po.next;
                            cmp = keyCompare(key, po.key);
                        } while (pe != po && cmp > 0);
                        if (cmp == 0) return po;
                    }
                }
            }
        }
        return null;
    }

    public Iterator<K> keyIterator() {
        return new KeyIterator();
    }

    public IntIterator valueIterator() {
        return new ValueIterator();
    }

    public Iterator<ToIntEntry<K>> iterator() {
        return new EntryIterator();
    }

    public ObjectIntRoMap<K> readMap() {
        int len = 0;

        for (Entry<K> e : data) if (e != null) len++;
        if (len > 0) {
            int[] cvt = new int[(len << 1) + 1];
            cvt[len << 1] = size;

            int h = 0, c = 0;
            K[] ks = createArray(size);
            int[] vs = new int[size];
            for (int i = 0; i < data.length; i++) {
                Entry<K> e = data[i];
                if (e != null) {
                    cvt[h] = i;
                    cvt[len + h] = c;
                    h++;
                    do {
                        ks[c] = e.key;
                        vs[c] = e.val;
                        c++;
                    } while ((e = e.next) != null);
                }
            }
            return createNonEmpty(cap, size, cvt, len, ks, vs);
        } else {
            return new EmptyReadonly<>();
        }
    }

    protected abstract K[] createArray(int size);
    protected abstract ObjectIntRoMap<K> createNonEmpty(int cap, int size, int[] cvt, int len, K[] ks, int[] vs);

    protected abstract int keyHash(K key);
    protected abstract int keyCompare(K key, K old);

    protected static final class Entry<K> {
        Entry<K> prev, next, gapNext;
        final K key;
        final int h;
        int val, gap;

        public Entry(K k, int h, int v) {
            this.key = k;
            this.h = h;
            this.val = v;
            this.prev = this.gapNext = this;
            this.next = null;
        }

        private void head(Entry<K> h, Entry<K> e) {
            this.next = h;
            this.prev = e;
            h.prev = this;
        }

        private void insert(Entry<K> p, Entry<K> n) {
            this.prev = p;
            this.next = n;
            if (p != null) p.next = this;
            if (n != null) n.prev = this;
        }

        private void end(Entry<K> p, Entry<K> h) {
            this.prev = p;
            h.prev = p.next = this;
        }

        private int setValue(int val) {
            int r = this.val;
            this.val = val;
            return r;
        }

        private void drop() {
            this.prev.next = this.next;
            this.next.prev = this.prev;
        }
    }

    private abstract class AbstractIterator {
        protected int expectedModCount;
        protected Entry<K> current, head, next;
        protected int index;

        protected AbstractIterator() {
            this.expectedModCount = modCount;
            this.index = 0;
            this.head = this.current = this.next = null;

            Entry<K>[] t = data;
            if (t != null && size > 0) {
                //noinspection StatementWithEmptyBody
                while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K> nextNode() {
            Entry<K>[] t;
            Entry<K> e = next;
            if (expectedModCount != modCount) throw new ConcurrentModificationException();
            if (e == null) throw new NoSuchElementException();
            if (current != null && current.gap > 0) head = current;
            if ((next = (current = e).next) == null && (t = data) != null) {
                //noinspection StatementWithEmptyBody
                while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Entry<K> p = current;
            if (p == null) throw new IllegalStateException();
            if (expectedModCount != modCount) throw new ConcurrentModificationException();
            if (p == p.prev) data[indexOf(p.h, cap)] = null;
            else {
                if (head == null) {
                    next.prev = p.prev;
                    p.next = null;
                    copyLink(p, next);
                    rightLink(next);
                    data[index] = next;
                } else {
                    p.drop();
                    if (p.gap > 0) copyLink(p, next);
                    rightLink(head);
                }
                linkCount--;
            }
            current = null;
            size--;
            expectedModCount = ++modCount;
        }
    }

    private class KeyIterator extends AbstractIterator implements Iterator<K> {
        @Override
        public K next() {
            return nextNode().key;
        }
    }

    private class ValueIterator extends AbstractIterator implements IntIterator {
        @Override
        public int next() {
            return nextNode().val;
        }
    }

    private class EntryIterator extends AbstractIterator implements Iterator<ToIntEntry<K>> {
        @Override
        public ToIntEntry<K> next() {
            return new EntryImpl<>(nextNode());
        }
    }

    private static class EntryImpl<K> implements ToIntEntry<K> {

        private final Entry<K> entry;
        EntryImpl(Entry<K> e) {
            this.entry = e;
        }

        @Override
        public K getKey() {
            return entry.key;
        }

        @Override
        public int getValue() {
            return entry.val;
        }

        @Override
        public int setValue(int value) {
            return entry.setValue(value);
        }
    }

    protected static abstract class Readonly<K> implements ObjectIntRoMap<K> {

        private final int cap, size, len;
        private final int[] cvt;
        private final K[] ks;
        private final int[] vs;

        protected Readonly(int cap, int size, int[] cvt, int len, K[] ks, int[] vs) {
            this.cap = cap;
            this.size = size;
            this.cvt = cvt;
            this.len = len;
            this.ks = ks;
            this.vs = vs;
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
        public int getOrDefault(K key, int def) {
            int h = hash(key);
            int i = indexOf(h, cap);
            int j = Arrays.binarySearch(cvt, 0, len, i);
            if (j >= 0) {
                j = Arrays.binarySearch(ks, cvt[j + len], cvt[j + len + 1], key, this::compare);
                return j < 0 ? def : vs[j];
            }
            return def;
        }

        protected abstract int hash(K key);
        protected abstract int compare(K key, K old);

        @Override
        public Iterator<K> keyIterator() {
            return new KeyIterator();
        }

        @Override
        public IntIterator valueIterator() {
            return new ValueIterator();
        }

        @Override
        public Iterator<ToIntEntry<K>> iterator() {
            return new EntryIterator();
        }

        private class KeyIterator implements Iterator<K> {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public K next() {
                return ks[i++];
            }
        }

        private class ValueIterator implements IntIterator {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public int next() {
                return vs[i++];
            }
        }

        private class EntryIterator implements Iterator<ToIntEntry<K>> {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public ToIntEntry<K> next() {
                return new ReadonlyEntryImpl(i++);
            }
        }

        private class ReadonlyEntryImpl implements ToIntEntry<K> {
            private final int i;
            private ReadonlyEntryImpl(int i) {
                this.i = i;
            }
            @Override
            public K getKey() {
                return ks[i];
            }

            @Override
            public int getValue() {
                return vs[i];
            }
        }
    }

    private static class EmptyReadonly<K> implements ObjectIntRoMap<K> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int getOrDefault(K key, int def) {
            return def;
        }

        @Override
        public Iterator<K> keyIterator() {
            return new Iterator<K>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public K next() {
                    throw new NoSuchElementException();
                }
            };
        }

        @Override
        public IntIterator valueIterator() {
            return new IntIterator() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public int next() {
                    throw new NoSuchElementException();
                }
            };
        }

        @Override
        public Iterator<ToIntEntry<K>> iterator() {
            return new Iterator<ToIntEntry<K>>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public ToIntEntry<K> next() {
                    throw new NoSuchElementException();
                }
            };
        }
    }

}
