package com.social_media_be.service.implement;

import com.social_media_be.dto.admin.AdminPostResponseDto;
import com.social_media_be.dto.admin.PostReportResponseDto;
import com.social_media_be.dto.post.PostImageDto;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.PostReport;
import com.social_media_be.entity.PostImage;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.Privacy;
import com.social_media_be.repository.PostReportRepository;
import com.social_media_be.repository.PostRepository;
import com.social_media_be.service.AdminPostService;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPostServiceImpl implements AdminPostService {

    private final PostRepository postRepository;
    private final PostReportRepository postReportRepository;

    @Override
    public Page<AdminPostResponseDto> getAllPosts(
            Pageable pageable,
            String username,
            Privacy privacy,
            Integer minReports,
            Integer maxReports) {
        Specification<Post> spec = Specification.unrestricted();

        if (username != null && !username.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<Post, User> userJoin = root.join("user");
                return cb.or(
                        cb.like(cb.lower(userJoin.get("username")), "%" + username.toLowerCase() + "%"),
                        cb.equal(userJoin.get("id").as(String.class), username));
            });
        }

        if (privacy != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("privacy"), privacy));
        }

        if (minReports != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("reportCount"), minReports));
        }

        if (maxReports != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("reportCount"), maxReports));
        }

        return postRepository.findAll(spec, pageable).map(this::mapToAdminDto);
    }

    @Override
    public List<PostReportResponseDto> getPostReports(Long postId) {
        List<PostReport> reports = postReportRepository.findAllByPostId(postId);
        return reports.stream().map(report -> PostReportResponseDto.builder()
                .id(report.getId())
                .reporterUsername(report.getReporter().getUsername())
                .reporterFullName(report.getReporter().getFullName())
                .reason(report.getReason())
                .createdAt(report.getCreatedAt())
                .build()).toList();
    }

    @Override
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return mapToPostResponse(post);
    }

    @Override
    public void hidePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setPrivacy(Privacy.HIDDEN);
        postRepository.save(post);
    }

    @Override
    public void deletePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }
        postRepository.deleteById(postId);
    }

    @Override
    public void dismissReports(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Delete all reports linked to this post
        List<PostReport> reports = postReportRepository.findAllByPostId(postId);
        postReportRepository.deleteAll(reports);

        // Reset count
        post.setReportCount(0);
        postRepository.save(post);
    }

    private AdminPostResponseDto mapToAdminDto(Post post) {
        String content = post.getContent();
        if (content != null && content.length() > 80) {
            content = content.substring(0, 77) + "...";
        }

        return AdminPostResponseDto.builder()
                .postId(post.getId())
                .userId(post.getUser().getId())
                .username(post.getUser().getUsername())
                .content(content)
                .status(post.getPrivacy())
                .reportCount(post.getReportCount() != null ? post.getReportCount() : 0)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private PostResponse mapToPostResponse(Post post) {
        UserSummary author = UserSummary.builder()
                .id(post.getUser().getId())
                .username(post.getUser().getUsername())
                .fullName(post.getUser().getFullName())
                .avatarUrl(post.getUser().getAvatarUrl())
                .build();

        List<PostImageDto> imageDtos = post.getImages().stream()
                .sorted((a, b) -> {
                    int idxA = a.getOrderIndex() != null ? a.getOrderIndex() : 0;
                    int idxB = b.getOrderIndex() != null ? b.getOrderIndex() : 0;
                    return Integer.compare(idxA, idxB);
                })
                .map(this::mapToPostImageDto)
                .collect(Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .privacy(post.getPrivacy())
                .feeling(post.getFeeling())
                .author(author)
                .images(imageDtos)
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0)
                .isLiked(false)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private PostImageDto mapToPostImageDto(PostImage image) {
        return PostImageDto.builder()
                .id(image.getId())
                .mediaUrl(image.getMediaUrl())
                .mediaType(image.getMediaType())
                .orderIndex(image.getOrderIndex())
                .build();
    }
}
