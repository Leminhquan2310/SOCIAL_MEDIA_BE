package com.social_media_be.service.implement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.social_media_be.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public Map upload(MultipartFile file, String folder) throws IOException {
        return upload(file, folder, "auto", false);
    }

    @Override
    public Map upload(MultipartFile file, String folder, String resourceType, boolean moderate) throws IOException {
        Map params = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", resourceType
        );
        if (moderate) {
            params.put("moderation", "aws_rek");
        }
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

        // Option B: Force approve on Cloudinary side to ensure URL is always accessible for Admin review.
        // Our backend will still use the moderation labels from uploadResult to handle internal visibility.
        if (moderate && uploadResult.containsKey("public_id")) {
            try {
                String publicId = (String) uploadResult.get("public_id");
                cloudinary.api().update(publicId, ObjectUtils.asMap(
                        "moderation_status", "approved",
                        "resource_type", resourceType
                ));
            } catch (Exception e) {
                // Log and continue - we don't want to fail the upload if force-approval fails
                System.err.println("Failed to force-approve moderated content: " + e.getMessage());
            }
        }

        return uploadResult;
    }

    @Override
    public Map delete(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    @Override
    public String extractPublicId(String url) {
        if (url == null || url.isEmpty() || !url.contains("upload/")) return null;
        try {
            String[] parts = url.split("upload/");
            String publicIdWithVersion = parts[1];
            int firstSlashIndex = publicIdWithVersion.indexOf("/");
            String publicIdWithExtension = publicIdWithVersion;
            if (publicIdWithVersion.startsWith("v") && firstSlashIndex != -1) {
                publicIdWithExtension = publicIdWithVersion.substring(firstSlashIndex + 1);
            }
            int lastDotIndex = publicIdWithExtension.lastIndexOf(".");
            if (lastDotIndex != -1) {
                return publicIdWithExtension.substring(0, lastDotIndex);
            }
            return publicIdWithExtension;
        } catch (Exception e) {
            return null;
        }
    }
}
