package com.example.laba_1.cache;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LRUMap;

import java.util.ArrayList;

public class CrunchifyInMemoryCache<K, T> {
    private final long timeToLive;
    private final LRUMap<K, CrunchifyCacheObject> crunchifyCacheMap;

    protected class CrunchifyCacheObject {
        public long lastAccessed = System.currentTimeMillis();
        public T value;

        protected CrunchifyCacheObject(T value) {
            this.value = value;
        }
    }

    public CrunchifyInMemoryCache(long crunchifyTimeToLive, final long crunchifyTimerInterval, int maxItems) {
        this.timeToLive = crunchifyTimeToLive * 1000;

        crunchifyCacheMap = new LRUMap<>(maxItems);

        if (timeToLive > 0 && crunchifyTimerInterval > 0) {

            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(crunchifyTimerInterval * 1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    crunchifyCleanup();
                }
            });

            t.setDaemon(true);
            t.start();
        }
    }

    public void put(K key, T value) {
        synchronized (crunchifyCacheMap) {
            crunchifyCacheMap.put(key, new CrunchifyCacheObject(value));
        }
    }

    public T get(K key) {
        synchronized (crunchifyCacheMap) {
            CrunchifyCacheObject c = crunchifyCacheMap.get(key);

            if (c == null)
                return null;
            else {
                c.lastAccessed = System.currentTimeMillis();
                return c.value;
            }
        }
    }

    public void remove(K key) {
        synchronized (crunchifyCacheMap) {
            crunchifyCacheMap.remove(key);
        }
    }

    public int size() {
        synchronized (crunchifyCacheMap) {
            return crunchifyCacheMap.size();
        }
    }

    public void crunchifyCleanup() {
        long now = System.currentTimeMillis();
        ArrayList<K> deleteKey;

        synchronized (crunchifyCacheMap) {
            MapIterator<K, CrunchifyCacheObject> itr = crunchifyCacheMap.mapIterator();

            deleteKey = new ArrayList<K>((crunchifyCacheMap.size() / 2) + 1);
            K key;
            CrunchifyCacheObject c;

            while (itr.hasNext()) {
                key = itr.next();
                c = itr.getValue();

                if (c != null && (now > (timeToLive + c.lastAccessed))) {
                    deleteKey.add(key);
                }
            }
        }

        for (K key : deleteKey) {
            synchronized (crunchifyCacheMap) {
                crunchifyCacheMap.remove(key);
            }
            Thread.yield();
        }
    }
}

