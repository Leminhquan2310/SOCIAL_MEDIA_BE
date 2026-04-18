package com.social_media_be.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CommentRequestDto {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;

    private MultipartFile image;
    private MultipartFile video;

    private Long parentCommentId;
}
