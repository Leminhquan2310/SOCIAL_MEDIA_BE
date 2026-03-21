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
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folder));
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
