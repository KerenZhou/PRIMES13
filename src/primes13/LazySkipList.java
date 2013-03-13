package primes13;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LazySkipList {

    static final int MAX_LEVEL = 32;
    final Node head = new Node(Long.MIN_VALUE);
    final Node tail = new Node(Long.MAX_VALUE);
    
    public Iterator<Long> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LazySkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = tail;
        }
    }

    public static class Node {

        final Lock lock = new ReentrantLock();
        final long key;
        final Node next[];
        volatile boolean marked = false;
        volatile boolean fullyLinked = false;
        private int topLevel;

        public Node(long key) {
            this.key = key;
            next = new Node[MAX_LEVEL + 1];
            topLevel = MAX_LEVEL;
        }

        public Node(long x, int height) {
            key = x;
            next = new Node[height + 1];
            topLevel = height;
        }
    }

    public int find(long key, Node[] preds, Node[] succs) {
        int found = -1;
        Node pred = head;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            Node curr = pred.next[level];
            while (key > curr.key) {
                pred = curr;
                curr = pred.next[level];
            }
            if (found == -1 && key == curr.key) {
                found = level;
            }
            preds[level] = pred;
            succs[level] = curr;
        }
        return found;
    }

    public boolean add(long x) {
        int topLevel = randomLevel();
        Node[] preds = new Node[MAX_LEVEL + 1];
        Node[] succs = new Node[MAX_LEVEL + 1];
        while (true) {
            int found = find(x, preds, succs);
            if (found != -1) {
                Node nodeFound = succs[found];
                if (!nodeFound.marked) {
                    while (!nodeFound.fullyLinked) {
                    }
                    return false;
                }
                continue;
            }
            int highestLocked = -1;
            try {
                Node pred, succ;
                boolean valid = true;
                for (int level = 0; valid && (level <= topLevel); level++) {
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level] == succ;
                }
                if (!valid) {
                    continue;
                }
                Node newNode = new Node(x, topLevel);
                for (int level = 0; level <= topLevel; level++) {
                    newNode.next[level] = succs[level];
                    preds[level].next[level] = newNode;
                }
                newNode.fullyLinked = true;
                return true;
            } finally {
                for (int level = 0; level <= highestLocked; level++) {
                    preds[level].lock.unlock();
                }
            }
        }
    }

    public boolean remove(long x) {
        Node victim = null;
        boolean isMarked = false;
        int topLevel = -1;
        Node[] preds = (Node[]) new Node[MAX_LEVEL + 1];
        Node[] succs = (Node[]) new Node[MAX_LEVEL + 1];
        while (true) {
            int lFound = find((Long) x, preds, succs);
            if (lFound != -1) {
                victim = succs[lFound];
            }
            if (isMarked
                    || (lFound != -1
                    && (victim.fullyLinked
                    && victim.topLevel == lFound
                    && !victim.marked))) {
                if (!isMarked) {
                    topLevel = victim.topLevel;
                    victim.lock.lock();
                    if (victim.marked) {
                        victim.lock.unlock();
                        return false;
                    }
                    victim.marked = true;
                    isMarked = true;
                }
                int highestLocked = -1;
                try {
                    Node pred, succ;
                    boolean valid = true;
                    for (int level = 0; valid && (level <= topLevel); level++) {
                        pred = preds[level];
                        pred.lock.lock();
                        highestLocked = level;
                        valid = !pred.marked && pred.next[level] == victim;
                    }
                    if (!valid) {
                        continue;
                    }
                    for (int level = topLevel; level >= 0; level--) {
                        preds[level].next[level] = victim.next[level];
                    }
                    victim.lock.unlock();
                    return true;
                } finally {
                    for (int i = 0;
                            i <= highestLocked;
                            i++) {
                        preds[i].lock.unlock();
                    }
                }
            } else {
                return false;
            }
        }
    }

    public boolean contains(long key) {
        Node pred = head;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            Node curr = pred.next[level];
            while (key > curr.key) {
                pred = curr;
                curr = pred.next[level];
            }
            if (key == curr.key) {
                return curr.fullyLinked && !curr.marked;
            }
        }
        return false;
    }
    private static final double P = 0.5;

    public static int randomLevel() {
        int lvl = (int) (Math.log(1. - Math.random()) / Math.log(1. - P));
        return Math.min(lvl, MAX_LEVEL);
    }
}