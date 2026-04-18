package com.social_media_be.buffer;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LikeCommentCountBuffer {
  private final ConcurrentHashMap<Long, AtomicLong> buffer = new ConcurrentHashMap<>();

  // Cộng thêm 1, trả về giá trị mới
  public long increment(Long postId) {
    return buffer.computeIfAbsent(postId, k -> new AtomicLong(0))
      .incrementAndGet();
  }

  public long decrement(Long postId) {
    return buffer.computeIfAbsent(postId, k -> new AtomicLong(0))
      .decrementAndGet();
  }

  // Lấy toàn bộ, reset về 0
  public Map<Long, Long> drainAll() {
    Map<Long, Long> snapshot = new HashMap<>();
    buffer.forEach((postId, counter) -> {
      long delta = counter.getAndSet(0);
      if (delta != 0) snapshot.put(postId, delta);
    });
    return snapshot;
  }

  // Lấy giá trị hiện tại (không reset)
  public long get(Long postId) {
    AtomicLong counter = buffer.get(postId);
    return counter != null ? counter.get() : 0;
  }
}
