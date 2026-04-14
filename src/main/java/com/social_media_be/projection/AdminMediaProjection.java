package com.social_media_be.projection;

import java.time.LocalDateTime;

public interface AdminMediaProjection {
    Long getId();
    String getUrl();
    String getType();
    String getStatus();
    Double getViolationScore();
    String getSourceType();
    Long getSourceId();
    String getOwnerName();
    LocalDateTime getCreatedAt();
    String getContent();
}
