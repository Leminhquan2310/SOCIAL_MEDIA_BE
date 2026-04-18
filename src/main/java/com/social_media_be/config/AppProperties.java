package com.social_media_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private final OAuth2 oauth2 = new OAuth2();
    private final ContentModeration contentModeration = new ContentModeration();

    @Getter
    @Setter
    public static class OAuth2 {
        private List<String> authorizedRedirectUris = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ContentModeration {
        private List<String> forbiddenKeywords = new ArrayList<>();
        private List<String> forbiddenPatterns = new ArrayList<>();
    }
}
