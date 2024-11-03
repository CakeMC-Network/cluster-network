package job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CopyOnWriteMap<K, V>
	 implements Map<K, V>,
	            Cloneable {
	private volatile Map<K, V> internalMap;

	public CopyOnWriteMap() {
		this.internalMap = new HashMap();
	}

	public CopyOnWriteMap(int initialCapacity) {
		this.internalMap = new HashMap(initialCapacity);
	}

	public CopyOnWriteMap(Map<K, V> data) {
		this.internalMap = new HashMap<K, V>(data);
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public V put(K key, V value) {
		CopyOnWriteMap copyOnWriteMap = this;
		synchronized (copyOnWriteMap) {
			HashMap<K, V> newMap = new HashMap<K, V>(this.internalMap);
			V val = newMap.put(key, value);
			this.internalMap = newMap;
			return val;
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public V remove(Object key) {
		CopyOnWriteMap copyOnWriteMap = this;
		synchronized (copyOnWriteMap) {
			HashMap<K, V> newMap = new HashMap<K, V>(this.internalMap);
			V val = newMap.remove(key);
			this.internalMap = newMap;
			return val;
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> newData) {
		CopyOnWriteMap copyOnWriteMap = this;
		synchronized (copyOnWriteMap) {
			HashMap<K, V> newMap = new HashMap<K, V>(this.internalMap);
			newMap.putAll(newData);
			this.internalMap = newMap;
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public void clear() {
		CopyOnWriteMap copyOnWriteMap = this;
		synchronized (copyOnWriteMap) {
			this.internalMap = new HashMap();
		}
	}

	@Override
	public int size() {
		return this.internalMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.internalMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.internalMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.internalMap.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return this.internalMap.get(key);
	}

	@Override
	public Set<K> keySet() {
		return this.internalMap.keySet();
	}

	@Override
	public Collection<V> values() {
		return this.internalMap.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return this.internalMap.entrySet();
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
}

