package com.social_media_be.service;

import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.post.PostUpdateRequest;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.PostImage;
import com.social_media_be.entity.User;
import com.social_media_be.repository.FriendshipRepository;
import com.social_media_be.repository.PostRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.implement.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceReorderTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private PostServiceImpl postService;

    private User user;
    private Post post;
    private PostImage img1;
    private PostImage img2;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).fullName("Test User").build();
        
        img1 = PostImage.builder().id(101L).imageUrl("url1").orderIndex(0).build();
        img2 = PostImage.builder().id(102L).imageUrl("url2").orderIndex(1).build();
        
        List<PostImage> images = new ArrayList<>();
        images.add(img1);
        images.add(img2);
        
        post = Post.builder()
                .id(1L)
                .user(user)
                .content("Original Content")
                .images(images)
                .build();
        
        img1.setPost(post);
        img2.setPost(post);
    }

    @Test
    void testUpdatePostReordering() {
        // Arrange
        PostUpdateRequest request = new PostUpdateRequest();
        request.setContent("Updated Content");
        // Swap order: img2 (102) comes first, then img1 (101)
        request.setImageOrder(List.of("102", "101"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        PostResponse response = postService.updatePost(1L, request, 1L);

        // Assert
        assertEquals(2, response.getImages().size());
        assertEquals(102L, response.getImages().get(0).getId());
        assertEquals(101L, response.getImages().get(1).getId());
        assertEquals(0, response.getImages().get(0).getOrderIndex());
        assertEquals(1, response.getImages().get(1).getOrderIndex());
    }

    @Test
    void testUpdatePostWithNewImageReordering() {
        // This test is harder to fully mock because it involves MultipartFile upload,
        // but we can test the logic where the list already has something without ID.
        
        // Arrange
        PostUpdateRequest request = new PostUpdateRequest();
        request.setImageOrder(List.of("new-0", "102", "101"));
        
        // Simulate a new image already added to the collection (as if uploaded)
        PostImage newImg = PostImage.builder().imageUrl("new-url").build();
        post.getImages().add(newImg);
        newImg.setPost(post);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        PostResponse response = postService.updatePost(1L, request, 1L);

        // Assert
        assertEquals(3, response.getImages().size());
        assertEquals(null, response.getImages().get(0).getId()); // The new one
        assertEquals(102L, response.getImages().get(1).getId());
        assertEquals(101L, response.getImages().get(2).getId());
        assertEquals(0, response.getImages().get(0).getOrderIndex());
        assertEquals(1, response.getImages().get(1).getOrderIndex());
        assertEquals(2, response.getImages().get(2).getOrderIndex());
    }
}
