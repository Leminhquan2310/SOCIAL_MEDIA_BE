package com.social_media_be.service.implement;

import com.social_media_be.config.AppProperties;
import com.social_media_be.exception.BadRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ContentModerationServiceImplTest {

    private ContentModerationServiceImpl buildService() {
        AppProperties appProperties = new AppProperties();
        AppProperties.ContentModeration moderation = appProperties.getContentModeration();
        moderation.setForbiddenKeywords(Arrays.asList("sex", "porn", "đồi trụy"));
        moderation.setForbiddenPatterns(Arrays.asList("\\bviagra\\b", "\\bxxx\\b"));
        return new ContentModerationServiceImpl(appProperties);
    }

    @Test
    void shouldFindForbiddenKeywordInHtmlContent() {
        ContentModerationServiceImpl service = buildService();
        Assertions.assertTrue(service.findForbiddenTerms("<p>Đây là bài viết về sex</p>").contains("sex"));
    }

    @Test
    void shouldFindForbiddenPatternMatch() {
        ContentModerationServiceImpl service = buildService();
        Assertions.assertTrue(service.findForbiddenTerms("This is viagra content").stream().anyMatch(term -> term.contains("viagra")));
    }

    @Test
    void shouldThrowWhenForbiddenContentPresent() {
        ContentModerationServiceImpl service = buildService();
        Assertions.assertThrows(BadRequestException.class, () -> service.validateContent("Xem bài viết về xxx"));
    }

    @Test
    void shouldAllowCleanContent() {
        ContentModerationServiceImpl service = buildService();
        Assertions.assertTrue(service.findForbiddenTerms("This is a clean post").isEmpty());
    }
}
