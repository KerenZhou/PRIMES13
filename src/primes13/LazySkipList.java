package primes13;

import java.util.concurrent.locks.Lock;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class LazySkipList {
    private static class HackMemoryAllocator {
        private static class NPointer {
            Node currNode;
            NPointer next;

            public NPointer(Node n) {
                currNode = n;
            }
        }

        private NPointer head;
        private NPointer tail;
        private NPointer uHead;
        private NPointer uTail;

        public HackMemoryAllocator() {
            tail = head = new NPointer(new Node(0));
            for(int i = 0; i < 19; i++) {
                tail = (tail.next = new NPointer(new Node(0)));
            }
        }

        public Node newNode() {
            NPointer nn = head;
            if(uTail != null) {
                uTail = (uTail.next = head);
            } else {
                uHead = uTail = head;
            }
            uTail.currNode = null;
            head = head.next;
            return nn.currNode;
        }

        public void returnNode(Node t) {
            tail.next = uHead;
            if(tail.next == null) tail.next = new NPointer(t);
            else {
                uHead = uHead.next;
                tail.next.currNode = t;
            }
            tail = tail.next;
            for(int i = 1; i <= MAX_LEVEL; i++) tail.currNode.next[i] = null;
            tail.currNode.key = 0;
        }
    }

    static final int MAX_LEVEL = 32;
    final Node head = new Node(Long.MIN_VALUE);
    final Node tail = new Node(Long.MAX_VALUE);
    private RandomSource rs = new RandomSource();
    private static final ThreadLocal<HackMemoryAllocator> nodeCache =
        new ThreadLocal<HackMemoryAllocator>() {
            @Override protected HackMemoryAllocator initialValue() {
                return new HackMemoryAllocator();
            }
        };
    public static boolean useAllocHack = false;
    
    public LazySkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = tail;
        }
    }

    public static class Node {
        final Lock lock = new ReentrantLock();
        long key;
        final Node next[];
        volatile boolean marked = false;
        volatile boolean fullyLinked = false;
        int topLevel;

        public Node(long key) {
            this.key = key;
            next = new Node[MAX_LEVEL + 1];
            topLevel = MAX_LEVEL;
        }

        public Node(long x, int height) {
            key = x;
            next = new Node[MAX_LEVEL + 1];
            topLevel = height;
        }
    }

    public int find(long key, Node[] elems) {
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
            elems[level] = pred;
            elems[MAX_LEVEL + 1 + level] = curr;
        }
        return found;
    }

    public boolean add(long x, Node[] elems) {
        int topLevel = randomLevel();
        while (true) {
            int found = find(x, elems);
            if (found != -1) {
                Node nodeFound = elems[MAX_LEVEL + 1 + found];
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
                    pred = elems[level];
                    succ = elems[MAX_LEVEL + 1 + level];
                    pred.lock.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level] == succ;
                }
                if (!valid) {
                    continue;
                }
                Node newNode;
                if(useAllocHack) newNode = nodeCache.get().newNode();
                else newNode = new Node(x, topLevel);
                for (int level = 0; level <= topLevel; level++) {
                    newNode.next[level] = elems[MAX_LEVEL + 1 + level];
                    elems[level].next[level] = newNode;
                }
                newNode.fullyLinked = true;
                return true;
            } finally {
                for (int level = 0; level <= highestLocked; level++) {
                    elems[level].lock.unlock();
                }
            }
        }
    }

    public boolean remove(long x, Node[] elems) {
        Node victim = null;
        boolean isMarked = false;
        int topLevel = -1;
        while (true) {
            int lFound = find(x, elems);
            if (lFound != -1) {
                victim = elems[MAX_LEVEL + 1 + lFound];
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
                        pred = elems[level];
                        pred.lock.lock();
                        highestLocked = level;
                        valid = !pred.marked && pred.next[level] == victim;
                    }
                    if (!valid) {
                        continue;
                    }
                    for (int level = topLevel; level >= 0; level--) {
                        elems[level].next[level] = victim.next[level];
                    }
                    victim.lock.unlock();
                    nodeCache.get().returnNode(victim);
                    return true;
                } finally {
                    for (int i = 0;
                            i <= highestLocked;
                            i++) {
                        elems[i].lock.unlock();
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
    
    private int randomSeed = new Random().nextInt();
    private double P = 0.5;
    private int randomLevel() {
        int x = randomSeed;
        x ^= x << 13;
        x ^= x >>> 17;
        randomSeed = x ^= x << 5;
        if ((x & 1) != 0) // test highest and lowest bits
            return 0;
        int level = 1;
        while (((x >>>= 1) & 1) != 0) ++level;
        return Math.min(level, MAX_LEVEL);
        //int lvl = (int) (Math.log(1. - rs.nextDouble()) / Math.log(1. - P));
        //return Math.min(lvl, MAX_LEVEL);
    }
}