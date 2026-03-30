package com.social_media_be.service;

import com.social_media_be.dto.friend.FriendUserDTO;
import com.social_media_be.entity.UserPrincipal;
import java.util.List;

public interface SearchService {
    List<FriendUserDTO> searchUsers(String query, UserPrincipal userPrincipal, Integer lastExactMatch, Integer lastMutualCount, Long lastId, int limit);
}
