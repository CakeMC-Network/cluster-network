package job;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CachedArrayList<E>
extends ArrayList<E> {
    private static final long serialVersionUID = 1;
    private long nextUpdate = Long.MAX_VALUE;
    private int defautTime;
    private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
    private CopyOnWriteMap<E, Long> times = new CopyOnWriteMap();
    private ArrayList<UnloadListener<E>> listener = new ArrayList();

    public CachedArrayList(int defautTime, TimeUnit defaultTimeUnit) {
        this.defautTime = defautTime;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    @Override
    public boolean add(E e) {
        return this.add(e, this.defautTime, this.defaultTimeUnit);
    }

    @Override
    public void add(int index, E element) {
        this.add(index, element, this.defautTime, this.defaultTimeUnit);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return this.addAll(c, this.defautTime, this.defaultTimeUnit);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return this.addAll(index, c, this.defautTime, this.defaultTimeUnit);
    }

    public boolean add(E e, int time, TimeUnit unit) {
        if (time == 0) {
            return false;
        }
        boolean add = super.add(e);
        long t = System.currentTimeMillis() + unit.toMillis(time);
        this.times.put(e, t);
        if (this.nextUpdate > t) {
            this.nextUpdate = t;
        }
        return add;
    }

    public void add(int index, E e, int time, TimeUnit unit) {
        if (time == 0) {
            return;
        }
        super.add(index, e);
        long t = System.currentTimeMillis() + unit.toMillis(time);
        this.times.put(e, t);
        if (this.nextUpdate > t) {
            this.nextUpdate = t;
        }
    }

    public boolean addAll(int index, Collection<? extends E> c, int time, TimeUnit unit) {
        if (time == 0) {
            return false;
        }
        for (E e : c) {
            long t = System.currentTimeMillis() + unit.toMillis(time);
            this.times.put(e, t);
            if (this.nextUpdate <= t) continue;
            this.nextUpdate = t;
        }
        return super.addAll(index, c);
    }

    public boolean addAll(Collection<? extends E> c, int time, TimeUnit unit) {
        if (time == 0) {
            return false;
        }
        for (E e : c) {
            long t = System.currentTimeMillis() + unit.toMillis(time);
            this.times.put(e, t);
            if (this.nextUpdate <= t) continue;
            this.nextUpdate = t;
        }
        return super.addAll(c);
    }

    @Override
    public E remove(int index) {
        Object obj = super.remove(index);
        long t = this.times.remove(obj);
        if (t == this.nextUpdate) {
            this.updateTimes();
        }
        return (E) obj;
    }

    @Override
    public E get(int index) {
        this.update();
        return super.get(index);
    }

    @Override
    public int size() {
        this.update();
        return super.size();
    }

    @Override
    public Iterator<E> iterator() {
        this.update();
        return super.iterator();
    }

    @Override
    public void clear() {
        this.nextUpdate = Long.MAX_VALUE;
        this.times.clear();
        super.clear();
    }

    public void update() {
        if (System.currentTimeMillis() > this.nextUpdate) {
            this.updateTimes();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!this.times.containsKey(o)) {
            return false;
        }
        long r = this.times.get(o);
        this.times.remove(o);
        if (r != 0 && this.nextUpdate == r) {
            this.update();
        }
        return super.remove(o);
    }

    public void resetTime(E element) {
        this.times.put(element, System.currentTimeMillis() + this.defaultTimeUnit.toMillis(this.defautTime));
    }

    @Override
    public boolean contains(Object o) {
        this.update();
        return super.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        this.update();
        return super.containsAll(c);
    }

    @Override
    public int indexOf(Object o) {
        this.update();
        return super.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        this.update();
        return super.isEmpty();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        this.update();
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public int lastIndexOf(Object o) {
        this.update();
        return super.lastIndexOf(o);
    }

    @Override
    public E set(int index, E element) {
        return this.set(index, element, this.defautTime, this.defaultTimeUnit);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        this.update();
        return super.toArray(a);
    }

    @Override
    public Object[] toArray() {
        this.update();
        return super.toArray();
    }

    public E set(int index, E element, int time, TimeUnit unit) {
        this.update();
        long l = unit.toMillis(time);
        this.times.put(element, l);
        if (l < this.nextUpdate) {
            this.nextUpdate = l;
        }
        E old = super.set(index, element);
        long r = this.times.get(old);
        this.times.remove(old);
        if (r != 0 && this.nextUpdate == r) {
            this.update();
        }
        return old;
    }

    private void updateTimes() {
        try {
            long min = Long.MAX_VALUE;
            long time = System.currentTimeMillis();
            HashMap<E, Long> ctimes = new HashMap<E, Long>(this.times);
            for (E e : ctimes.keySet()) {
                long l = ctimes.get(e);
                if (time > l) {
                    boolean alowed = true;
                    for (UnloadListener<E> listener : new ArrayList<UnloadListener<E>>(this.listener)) {
                        if (listener == null || listener.canUnload(e)) continue;
                        alowed = false;
                    }
                    if (alowed) {
                        super.remove(e);
                        this.times.remove(e);
                        continue;
                    }
                    this.resetTime(e);
                    l = ctimes.get(e);
                    continue;
                }
                if (l >= min) continue;
                min = l;
            }
            this.nextUpdate = min;
        }
        catch (Exception ex) {
            try {
                System.out.println("Times: " + this.times);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            ex.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public void addUnloadListener(UnloadListener<E> listener) {
        this.listener.add(listener);
    }

    public void removeUnloadListener(UnloadListener<E> listener) {
        this.listener.remove(listener);
    }

    @FunctionalInterface
    public static interface UnloadListener<E> {
        public boolean canUnload(E var1);
    }

}

