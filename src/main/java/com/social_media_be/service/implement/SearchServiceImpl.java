package com.social_media_be.service.implement;

import com.social_media_be.dto.friend.FriendUserDTO;
import com.social_media_be.dto.user.UserSearchProjection;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FriendUserDTO> searchUsers(String query, UserPrincipal userPrincipal, Integer lastExactMatch, Integer lastMutualCount, Long lastId, int limit) {
        Long currentUserId = userPrincipal != null ? userPrincipal.getId() : -1L; // Fallback or handle differently if guest is allowed

        List<UserSearchProjection> projections = userRepository.searchUsers(query, currentUserId, lastExactMatch, lastMutualCount, lastId, limit);

        return projections.stream().map(p -> FriendUserDTO.builder()
                .id(p.getId())
                .username(p.getUsername())
                .fullName(p.getFullName())
                .avatarUrl(p.getAvatarUrl())
                // Set mutual friend count. The query already calculated it.
                .mutualFriendsCount(p.getMutualCount() != null ? p.getMutualCount() : 0)
                .exactMatch(p.getExactMatch() != null ? p.getExactMatch() : 0)
                .build()).collect(Collectors.toList());
    }
}
