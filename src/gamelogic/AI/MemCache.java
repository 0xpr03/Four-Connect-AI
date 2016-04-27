package gamelogic.AI;

import java.util.ArrayList;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Adopted and upgraded version of the MemCache from Crunchify.com
 * 
 * @author Aron Heinecke
 */
@SuppressWarnings("rawtypes")
public class MemCache<K, T> {
	private long timeToLive;
	private long adjustmentTime;
	private LRUMap cacheMap;
	private Logger logger = LogManager.getLogger();

	protected class CacheObject {
		public long lastAccessed = System.currentTimeMillis();
		public T value;

		protected CacheObject(T value) {
			this.value = value;
		}
	}
	
	/**
	 * Memory cache object<br>
	 * If the flush time should be too high or low to meet the min/max values, it'll be adjusted.<br>
	 * In case of an higher item count, the time will be decreased by 5 seconds. In case of an lower count,
	 * the time will be increased by 10 seconds.
	 * @param lifetimeSec max. lifetime of an object
	 * @param timerIntervall time interval between cleanup cycles
	 * @param maxItems max items to be stored till the first one will be overridden<br>
	 * additionally the adjustment time will decrease, creating earlier flushes
	 * @param minItems minimal items, under this value the cleanup interval will increase<br>
	 */
	public MemCache(long lifetimeSec, final long timerIntervall, int maxItems, int minItems, int max_target) {
		this.timeToLive = lifetimeSec * 1000;
		adjustmentTime = 0;
		cacheMap = new LRUMap(maxItems);

		if (timeToLive > 0 && timerIntervall > 0) {

			Thread t = new Thread(new Runnable() {
				public void run() {
					while (true) {
						try {
							Thread.sleep((timerIntervall + adjustmentTime) * 1000);
						} catch (InterruptedException ex) {
						}
						int size = size();
						logger.debug("Size: {}",size);
						if(size < minItems){
							adjustmentTime += 10;
						}else if(size > max_target){
							adjustmentTime -= 5;
						}
						cleanup();
					}
				}
			});

			t.setDaemon(true);
			t.start();
		}
	}

	@SuppressWarnings("unchecked")
	public void put(K key, T value) {
		synchronized (cacheMap) {
			cacheMap.put(key, new CacheObject(value));
		}
	}

	@SuppressWarnings("unchecked")
	public T get(K key) {
		synchronized (cacheMap) {
			CacheObject c = (CacheObject) cacheMap.get(key);

			if (c == null)
				return null;
			else {
				c.lastAccessed = System.currentTimeMillis();
				return c.value;
			}
		}
	}

	public void remove(K key) {
		synchronized (cacheMap) {
			cacheMap.remove(key);
		}
	}

	public int size() {
		synchronized (cacheMap) {
			return cacheMap.size();
		}
	}
	
	/**
	 * Use only on debug, very intense operation O(n) where n = maxitems & lock
	 * @return
	 */
	public String debug(){
		StringBuilder sb = new StringBuilder();
		synchronized (cacheMap) {
			for(Object i: cacheMap.keySet()){
				sb.append(i);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public void cleanup() {

		long now = System.currentTimeMillis();
		ArrayList<K> deleteKey = null;

		synchronized (cacheMap) {
			MapIterator<K, T> itr = cacheMap.mapIterator();

			deleteKey = new ArrayList<K>((cacheMap.size() / 2) + 1);
			K key = null;
			CacheObject c = null;

			while (itr.hasNext()) {
				key = itr.next();
				c = (CacheObject) itr.getValue();

				if (c != null && (now > (timeToLive+adjustmentTime + c.lastAccessed))) {
					deleteKey.add(key);
				}
			}
		}

		for (K key : deleteKey) {
			synchronized (cacheMap) {
				cacheMap.remove(key);
			}

			Thread.yield();
		}
	}
}