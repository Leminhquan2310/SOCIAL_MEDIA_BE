package com.social_media_be.service;

import java.util.List;

public interface ContentModerationService {
    List<String> findForbiddenTerms(String content);
    void validateContent(String content);
    void validateContent(String content, String violationMessage);
}
