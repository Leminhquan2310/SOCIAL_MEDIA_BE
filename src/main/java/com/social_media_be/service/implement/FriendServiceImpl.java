package com.social_media_be.service.implement;

import com.social_media_be.dto.friend.FriendStatusDTO;
import com.social_media_be.dto.friend.FriendUserDTO;
import com.social_media_be.entity.Friendship;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.entity.enums.DisplayFriendsStatus;
import com.social_media_be.entity.enums.FriendStatus;
import com.social_media_be.entity.enums.NotificationType;
import com.social_media_be.exception.BadRequestException;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.exception.UnauthorizedException;
import com.social_media_be.repository.FriendshipRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final com.social_media_be.service.NotificationService notificationService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private FriendUserDTO mapToFriendUserDTO(User user, Long currentUserId) {
        long mutualCount = currentUserId != null
                ? friendshipRepository.countFriends(user.getId()) // simplified; full mutual count via repo
                : 0;
        return FriendUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .mutualFriendsCount(mutualCount)
                .build();
    }

    private FriendUserDTO mapFriendshipToDTO(Friendship friendship, Long currentUserId) {
        User friend = friendship.getRequester().getId().equals(currentUserId)
                ? friendship.getReceiver()
                : friendship.getRequester();
        return mapToFriendUserDTO(friend, currentUserId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public Page<FriendUserDTO> getFriends(UserPrincipal uPrincipal, String username, Pageable pageable) {
        Optional<User> user = userRepository.findByUsername(username);

        Page<FriendUserDTO> result = friendshipRepository.findFriendsByUserId(user.get().getId(), FriendStatus.ACCEPTED, pageable)
                .map(u -> mapToFriendUserDTO(u, user.get().getId()));

        boolean isOwner = user.get().getId().equals(uPrincipal.getId());
        if (!isOwner) {
            if (user.get().getDisplayFriendsStatus() == DisplayFriendsStatus.ONLY_ME)
                throw new AccessDeniedException("This profile is private");

            Friendship friendship = friendshipRepository.findFriendshipBetween(uPrincipal.getId(), user.get().getId())
                    .orElseThrow(() -> new AccessDeniedException("This profile is private"));
            if (user.get().getDisplayFriendsStatus() == DisplayFriendsStatus.FRIEND_ONLY
                    && friendship.getStatus() != FriendStatus.ACCEPTED)
                throw new AccessDeniedException("This profile is private");

        }
        return result;
    }

    @Override
    public Page<FriendUserDTO> getFriendRequests(Long currentUserId, Pageable pageable) {
        return friendshipRepository.findPendingRequestsByReceiver(currentUserId, FriendStatus.PENDING, pageable)
                .map(f -> mapFriendshipToDTO(f, currentUserId));
    }

    @Override
    public Page<FriendUserDTO> getSuggestions(Long currentUserId, Pageable pageable) {
        // Suggest users who are friends of friends but not yet friends with currentUser
        // Simple approach: return users that are friends of currently accepted friends
        // and not already connected
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        return userRepository.findFriendSuggestions(currentUserId, PageRequest.of(page, size))
                .map(u -> mapToFriendUserDTO(u, currentUserId));
    }

    @Override
    public FriendStatusDTO getRelationshipStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId == null) {
            return FriendStatusDTO.builder().status("NONE").build();
        }
        return friendshipRepository.findFriendshipBetween(currentUserId, targetUserId)
                .map(f -> {
                    String status;
                    if (f.getStatus() == FriendStatus.ACCEPTED) {
                        status = "ACCEPTED";
                    } else if (f.getStatus() == FriendStatus.PENDING) {
                        status = f.getRequester().getId().equals(currentUserId)
                                ? "PENDING_SENT"
                                : "PENDING_RECEIVED";
                    } else {
                        status = f.getStatus().name();
                    }
                    return FriendStatusDTO.builder()
                            .status(status)
                            .friendshipId(f.getId())
                            .requesterId(f.getRequester().getId())
                            .build();
                })
                .orElse(FriendStatusDTO.builder().status("NONE").build());
    }

    @Override
    public Page<FriendUserDTO> getMutualFriends(Long userId1, Long userId2, Pageable pageable) {
        return friendshipRepository.findMutualFriends(userId1, userId2, pageable)
                .map(u -> mapToFriendUserDTO(u, userId1));
    }

    @Override
    public int getMutualFriendsCount(Long userId1, Long userId2) {
        return (int) friendshipRepository.countManualFriends(userId1, userId2);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void sendFriendRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        User requester = getUser(requesterId);
        User receiver = getUser(receiverId);

        friendshipRepository.findFriendshipBetween(requesterId, receiverId).ifPresent(f -> {
            if (f.getStatus() == FriendStatus.ACCEPTED) {
                throw new BadRequestException("Already friends");
            }
            if (f.getStatus() == FriendStatus.PENDING) {
                throw new BadRequestException("Friend request already pending");
            }
        });

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);
        log.info("Friend request sent from {} to {}", requesterId, receiverId);

        // Tạo thông báo realtime
        notificationService.createAndSendNotification(receiver, requester, NotificationType.FRIEND_REQUEST,
                saved.getId());
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long requesterId, Long receiverId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(requesterId, receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendStatus.PENDING) {
            throw new BadRequestException("No pending friend request to cancel");
        }
        if (!friendship.getRequester().getId().equals(requesterId)) {
            throw new UnauthorizedException("Only the requester can cancel the request");
        }

        friendshipRepository.delete(friendship);
        log.info("Friend request cancelled by {} to {}", requesterId, receiverId);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long currentUserId, Long requesterId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(requesterId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendStatus.PENDING) {
            throw new BadRequestException("No pending friend request to accept");
        }
        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the receiver can accept the request");
        }

        friendship.setStatus(FriendStatus.ACCEPTED);
        Friendship saved = friendshipRepository.save(friendship);
        log.info("Friend request accepted: {} accepted {}", currentUserId, requesterId);

        // Thông báo cho người gửi là yêu cầu đã được chấp nhận
        notificationService.createAndSendNotification(
                saved.getRequester(),
                saved.getReceiver(),
                NotificationType.FRIEND_ACCEPT,
                saved.getId());
    }

    @Override
    @Transactional
    public void declineFriendRequest(Long currentUserId, Long requesterId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(requesterId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendStatus.PENDING) {
            throw new BadRequestException("No pending friend request to decline");
        }
        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the receiver can decline the request");
        }

        friendshipRepository.delete(friendship);
        log.info("Friend request declined: {} declined {}", currentUserId, requesterId);
    }

    @Override
    @Transactional
    public void removeFriend(Long currentUserId, Long friendId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(currentUserId, friendId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));

        if (friendship.getStatus() != FriendStatus.ACCEPTED) {
            throw new BadRequestException("Users are not friends");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship removed between {} and {}", currentUserId, friendId);
    }
}
