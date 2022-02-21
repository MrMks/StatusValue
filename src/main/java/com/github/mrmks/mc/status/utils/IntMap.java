package com.github.mrmks.mc.status.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntMap<V> {

    private int cap;
    private int size = 0;
    private int count = 0;
    private Node[] data;

    public IntMap() {
        cap = 16;
    }

    public IntMap(int initCap) {
        if (initCap >= 0x40000000) cap = 0x40000000;
        else {
            cap = 16;
            while (cap < initCap) cap <<= 1;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containKey(int key) {
        return get(key) != null;
    }

    public V put(int key, V val) {
        int i = indexOf(key, cap);
        if (data == null) data = new Node[cap];
        if (data[i] == null) {
            data[i] = new Node(key, val);
            size++;
        } else {
            final Node n = data[i];
            if (key == n.key) {
                Object r = n.val;
                n.val = val;
                return unwrap(r);
            } else if (key < n.key) {
                data[i] = new Node(key, val);
                data[i].next = n;
                data[i].end = n.next == null ? n : n.end;
                n.end = null;
            } else {
                if (n.end == null) {
                    n.next = n.end = new Node(key, val);
                } else {
                    if (key > n.end.key) {
                        n.end.next = new Node(key, val);
                        n.end = n.end.next;
                    } else if (key == n.end.key) {
                        Object r = n.end.val;
                        n.end.val = val;
                        return unwrap(r);
                    } else {
                        Node t = n, l = t.next;
                        while (l != null) {
                            if (key < l.key) {
                                t.next = new Node(key, val);
                                t.next.next = l;
                                break;
                            } else if (key == l.key) {
                                Object r = l.val;
                                l.val = val;
                                return unwrap(r);
                            }
                            t = l;
                            l = l.next;
                        }
                    }
                }
            }
            size++;
            count++;

        }
        if ((size > cap || count > (cap >> 2)) && cap < 0x40000000) {
            cap <<= 1;
            Node[] tar = new Node[cap];
            int count = 0;
            Node l;
            for (Node e : data) {
                if (e == null) continue;
                int j = indexOf(e.key, cap);
                if (e.next == null) {
                    tar[j] = e;
                } else {
                    Node h1 = e, e1 = null, h2 = null, e2 = null;
                    int j2 = 0;
                    while ((l = e.next) != null) {
                        e.next = null;
                        int k = indexOf(l.key, cap);
                        if (k == j) {
                            (e1 == null ? h1 : e1).next = l;
                            e1 = l;
                            count++;
                        } else {
                            j2 = k;
                            if (h2 == null) h2 = l;
                            else {
                                (e2 == null ? h2 : e2).next = l;
                                e2 = l;
                                count++;
                            }
                        }
                        e = l;
                    }
                    tar[j] = h1;
                    h1.end = e1;
                    if (h2 != null) {
                        tar[j2] = h2;
                        h2.end = e2;
                    }
                }
            }
            data = tar;
            this.count = count;
        }
        return null;
    }

    public V get(int key) {
        if (data == null) return null;
        int i = indexOf(key, cap);
        Node n = data[i];
        if (n == null || (n.key > key)) return null;
        if (n.end != null && n.end.key < key) return null;
        while (n != null) {
            if (n.key < key) {
                n = n.next;
            } else if (n.key == key) {
                return unwrap(n.val);
            } else break;
        }
        return null;
    }

    public V getOrDefault(int key, V def) {
        if (data == null) return def;
        int i = indexOf(key, cap);
        Node n = data[i];
        if (n == null || (n.key > key)) return def;
        if (n.end != null && n.end.key < key) return def;
        while (n != null) {
            if (n.key < key) {
                n = n.next;
            } else if (n.key == key) {
                return unwrap(n.val);
            } else break;
        }
        return def;
    }

    public V remove(int key) {
        if (data == null) return null;
        int i = indexOf(key, cap);
        Node n = data[i], last = null;
        if (n == null || (n.key > key)) return null;
        if (n.end != null && n.end.key < key) return null;

        while (n != null) {
            if (n.key > key) {
                break;
            } else if (n.key == key) {
                if (last == null) {
                    if (n.next != null) n.next.end = n.end;
                    data[i] = n.next;
                } else {
                    last.next = n.next;
                    if (n.next == null) data[i].end = last;
                    count--;
                }
                size--;
                return unwrap(n.val);
            } else {
                last = n;
                n = n.next;
            }
        }
        return null;
    }

    public void clear() {
        if (data != null) {
            Arrays.fill(data, null);
            size = 0;
            count = 0;
        }
    }

    public Iterator<V> valueIterator() {
        return new ValueIterator();
    }

    private static int indexOf(int key, int cap) {
        return key & (cap - 1);
    }

    @SuppressWarnings("unchecked")
    private V unwrap(Object obj) {
        return obj == null ? null : (V) obj;
    }

    private static class Node {
        private Node next;
        private Node end;
        private final int key;
        private Object val;

        private Node(int key, Object v) {
            this.key = key;
            this.val = v;
        }
    }

    private class ValueIterator implements Iterator<V> {

        private int index = 0;
        private Node node = null;

        private ValueIterator() {
            findNext();
        }

        private void findNext() {
            if (data != null) {
                if (node != null) {
                    node = node.next;
                }
                if (node == null) {
                    for (; index < data.length; index++) {
                        if (data[index] != null) node = data[index];
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public V next() {
            if (node == null) throw new NoSuchElementException();

            Node self = node;
            findNext();
            return unwrap(self.val);
        }
    }
}
