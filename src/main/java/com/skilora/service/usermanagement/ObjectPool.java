package com.skilora.service.usermanagement;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ObjectPool<T> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();

    public T acquire() { return pool.poll(); }
    public void release(T obj) { if (obj != null) pool.offer(obj); }
}
