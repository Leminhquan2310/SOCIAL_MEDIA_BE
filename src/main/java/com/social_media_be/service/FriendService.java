package com.social_media_be.service;

import com.social_media_be.dto.friend.FriendStatusDTO;
import com.social_media_be.dto.friend.FriendUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FriendService {
    Page<FriendUserDTO> getFriends(String username, Pageable pageable);
    
    Page<FriendUserDTO> getFriendRequests(Long currentUserId, Pageable pageable);
    
    Page<FriendUserDTO> getSuggestions(Long currentUserId, Pageable pageable);
    
    FriendStatusDTO getRelationshipStatus(Long currentUserId, Long targetUserId);
    
    Page<FriendUserDTO> getMutualFriends(Long userId1, Long userId2, Pageable pageable);

    int getMutualFriendsCount(Long userId1, Long userId2);
    
    void sendFriendRequest(Long requesterId, Long receiverId);
    
    void cancelFriendRequest(Long requesterId, Long receiverId);
    
    void acceptFriendRequest(Long currentUserId, Long requesterId);
    
    void declineFriendRequest(Long currentUserId, Long requesterId);
    
    void removeFriend(Long currentUserId, Long friendId);
}
