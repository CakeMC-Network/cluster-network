package job;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CachedHashMap<K, V>
extends HashMap<K, V> {
    private CachedArrayList<K> keys;
    private boolean locked = false;
    private ArrayList<CachedArrayList.UnloadListener<Entry<K, V>>> listener = new ArrayList();
    private int maxSize = -1;

    public CachedHashMap(int defautTime, TimeUnit defaultTimeUnit) {
        this.keys = new CachedArrayList(defautTime, defaultTimeUnit);

        this.keys.addUnloadListener(new CachedArrayList.UnloadListener<K>(){

            @Override
            public boolean canUnload(final K element) {
                boolean alowed = true;
                for (CachedArrayList.UnloadListener<Entry<K, V>> listener : new ArrayList<CachedArrayList.UnloadListener<Entry<K, V>>>(CachedHashMap.this.listener)) {
                    if (listener == null || listener.canUnload(new Entry<K, V>(){

                        @Override
                        public K getKey() {
                            return (K)element;
                        }

                        @Override
                        public V getValue() {
                            return CachedHashMap.this.get0(element);
                        }

                        @Override
                        public V setValue(V value) {
                            return this.getValue();
                        }
                    })) continue;
                    alowed = false;
                }
                return alowed;
            }

        });
    }

    @Override
    public V put(K key, V value) {
        this.putKey(key);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (K key : m.keySet()) {
            this.putKey(key);
        }
        super.putAll(m);
    }

    @Override
    public V get(Object key) {
        Object out = super.get(key);
        if (!this.locked && !this.keys.contains(key)) {
            out = null;
            super.remove(key);
        }
        return (V) out;
    }

    protected V get0(Object key) {
        return super.get(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V out = this.get(key);
        if (out == null) {
            return defaultValue;
        }
        return out;
    }

    @Override
    public V remove(Object key) {
        this.keys.remove(key);
        return super.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        this.keys.remove(key);
        return super.remove(key, value);
    }

    private void putKey(K key) {
        this.keys.remove(key);
        this.keys.add(key);
    }

    public V put(K key, V value, int time, TimeUnit unit) {
        this.keys.add(key, time, unit);
        return super.put(key, value);
    }

    @Override
    public int size() {
        return this.keys.size();
    }

    @Override
    public Set<K> keySet() {
        if (!this.locked) {
            this.keys.update();
        }
        return new HashSet<K>(this.keys);
    }

    @Override
    public Collection<V> values() {
        for (Object key : super.keySet()) {
            this.get(key);
        }
        return super.values();
    }

    public void lock() {
        this.locked = true;
        this.keys.update();
    }

    public void resetTime(K key) {
        this.keys.resetTime(key);
    }

    public void unlock() {
        this.locked = false;
    }

    public void addUnloadListener(CachedArrayList.UnloadListener<Entry<K, V>> listener) {
        this.listener.add(listener);
    }

    public void removeUnloadListener(CachedArrayList.UnloadListener<Entry<K, V>> listener) {
        this.listener.remove(listener);
    }

}

