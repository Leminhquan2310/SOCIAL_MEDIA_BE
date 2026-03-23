package com.social_media_be.dto.post;

import com.social_media_be.entity.enums.Privacy;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostUpdateRequest {
    private String content;
    private Privacy privacy;
    private String feeling;
    private List<Long> deletedImageIds = new ArrayList<>();
    private List<MultipartFile> newImages = new ArrayList<>();
    private List<String> imageOrder = new ArrayList<>();
}
