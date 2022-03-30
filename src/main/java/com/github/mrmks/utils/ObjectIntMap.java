package com.github.mrmks.utils;

import java.util.Iterator;

public abstract class ObjectIntMap<K> {

    protected static final int LINK_GAP = 8;

    protected int cap;
    protected int size;
    protected int linkCount;

    protected Entry<K>[] data;

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
            if (linkCount > (cap >>> 2) && size > 16 && cap > 0 && size > cap && size < Integer.MAX_VALUE) {
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
                            } else {
                                e.end(l.prev, l);
                                lh.gap += 1;
                                lh.gapNext = e;
                                if (lh.gap == LINK_GAP) (lh = e).gap = 0;
                                linkCount += 1;
                            }
                        } else {
                            if (r == null) {
                                (rh = r = e.prev = e).gap = 0;
                            } else {
                                e.end(r.prev, r);
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
        if (!find) size++;
        if (link) linkCount++;
        return r;
    }

    public int getOrDefault(K key, int def) {
        Entry<K> entry = find(key);
        return entry == null ? def : entry.val;
    }

    public int remove(K key) {
        int h = keyHash(key);
        int i = indexOf(h, cap);
        Entry<K> head = data[i];
        boolean find = false, link = false;
        int r = 0;
        if (head.prev == head) {
            if (head.h == h && keyCompare(head.key, key) == 0) {
                data[i] = null;
                find = true;
                r =  head.val;
            }
        } else {
            int cmp = keyCompare(key, head.key);
            if (cmp == 0) {
                (data[i] = head.next).prev = head.prev;
                copyLink(head, head.next);
                rightLink(head.next);
                find = link = true;
                r = head.val;
            } else if (cmp > 0){
                Entry<K> end = head.prev;
                cmp = keyCompare(key, end.key);
                if (cmp == 0) {
                    while (head.gapNext.gapNext != end) head = head.gapNext;
                    (head.prev = end.prev).next = null;
                    rightLink(head);
                    find = link = true;
                    r = end.val;
                } else if (cmp < 0){
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

    protected Entry<K> find(K key) {
        int h = keyHash(key);
        int i = indexOf(h, cap);
        Entry<K> head = data[i];
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
        throw new UnsupportedOperationException("keyIterator");
    }

    public IntIterator valueIterator() {
        throw new UnsupportedOperationException("valueIterator");
    }

    public Iterator<ToIntEntry<K>> iterator() {
        throw new UnsupportedOperationException("entryIterator");
    }

    public ObjectIntRoMap<K> readMap() {
        throw new UnsupportedOperationException("readMap");
    }

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
}
