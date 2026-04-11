package com.social_media_be.service.implement;

import com.social_media_be.config.AppProperties;
import com.social_media_be.exception.ContentViolationException;
import com.social_media_be.service.ContentModerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentModerationServiceImpl implements ContentModerationService {

    private final List<String> forbiddenKeywords;
    private final List<Pattern> forbiddenPatterns;

    public ContentModerationServiceImpl(AppProperties appProperties) {
        this.forbiddenKeywords = Optional.ofNullable(appProperties.getContentModeration().getForbiddenKeywords())
                .orElseGet(ArrayList::new);

        this.forbiddenPatterns = Optional.ofNullable(appProperties.getContentModeration().getForbiddenPatterns())
                .orElseGet(ArrayList::new).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .map(pattern -> {
                    try {
                        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    } catch (Exception ex) {
                        log.error("Invalid content moderation regex: {}", pattern, ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findForbiddenTerms(String content) {
        String plainText = stripHtml(content);
        String normalized = plainText.toLowerCase(Locale.ROOT);
        Set<String> matches = new LinkedHashSet<>();

        for (String keyword : forbiddenKeywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }
            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            if (!normalizedKeyword.isEmpty() && normalized.contains(normalizedKeyword)) {
                matches.add(keyword.trim());
            }
        }

        for (Pattern pattern : forbiddenPatterns) {
            if (pattern.matcher(plainText).find()) {
                matches.add(pattern.pattern());
            }
        }

        return new ArrayList<>(matches);
    }

    @Override
    public void validateContent(String content) {
        validateContent(content, "Content contains restricted words");
    }

    @Override
    public void validateContent(String content, String violationMessage) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        List<String> forbidden = findForbiddenTerms(content);
        if (!forbidden.isEmpty()) {
            throw new ContentViolationException(violationMessage, forbidden);
        }
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("(?s)<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
