package com.zoll.util;

/**
 * The cache evict policy;
 * 
 * @author crazyjohn
 *
 * @param <K>
 * @param <V>
 */
public interface IEvictPolicy<K, V> {
	
	public boolean evict(K key, V value);
}
