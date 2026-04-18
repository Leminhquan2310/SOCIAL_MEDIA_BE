package com.social_media_be.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class ContentViolationException extends BadRequestException {
    private final List<String> matches;

    public ContentViolationException(String message, List<String> matches) {
        super(message);
        this.matches = matches == null ? Collections.emptyList() : Collections.unmodifiableList(matches);
    }
}
