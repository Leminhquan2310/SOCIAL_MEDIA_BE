package com.social_media_be.service.implement;

import com.social_media_be.entity.Comment;
import com.social_media_be.entity.PostImage;
import com.social_media_be.entity.enums.MediaStatus;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.projection.AdminMediaProjection;
import com.social_media_be.repository.CommentRepository;
import com.social_media_be.repository.PostImageRepository;
import com.social_media_be.service.AdminMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMediaServiceImpl implements AdminMediaService {

    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminMediaProjection> getMediaStream(MediaStatus status, Double minScore, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        return postImageRepository.findAllUnifiedMedia(statusStr, minScore, pageable);
    }

    @Override
    @Transactional
    public void updateStatus(String sourceType, Long id, MediaStatus status) {
        if ("POST".equalsIgnoreCase(sourceType)) {
            PostImage postImage = postImageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Post image not found with id: " + id));
            postImage.setStatus(status);
            postImageRepository.save(postImage);
        } else if ("COMMENT".equalsIgnoreCase(sourceType)) {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
            comment.setStatus(status);
            commentRepository.save(comment);
        } else {
            throw new IllegalArgumentException("Invalid source type: " + sourceType);
        }
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<Map<String, Object>> items, MediaStatus status) {
        for (Map<String, Object> item : items) {
            try {
                String sourceType = (String) item.get("sourceType");
                Long id = Long.valueOf(item.get("id").toString());
                updateStatus(sourceType, id, status);
            } catch (Exception e) {
                log.error("Failed to update status for item: {}", item, e);
            }
        }
    }
}
