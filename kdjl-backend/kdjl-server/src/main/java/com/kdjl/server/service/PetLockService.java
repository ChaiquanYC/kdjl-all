package com.kdjl.server.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Prevents concurrent operations on the same pet (matching PHP lockItem/unLockItem). */
@Service
public class PetLockService {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock lock(Long petId) {
        return locks.computeIfAbsent(petId, k -> new ReentrantLock());
    }

    /** Cleanup unused locks after operation. */
    public void unlock(Long petId) {
        ReentrantLock lock = locks.get(petId);
        if (lock != null && !lock.isLocked() && !lock.hasQueuedThreads()) {
            locks.remove(petId);
        }
    }
}
