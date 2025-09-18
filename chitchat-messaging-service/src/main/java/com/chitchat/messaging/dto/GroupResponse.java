package com.chitchat.messaging.dto;

import com.chitchat.messaging.document.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for group response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    
    private String id;
    private String name;
    private String description;
    private String avatarUrl;
    private Long adminId;
    private List<GroupMemberResponse> members;
    private GroupSettingsResponse settings;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberResponse {
        private Long userId;
        private Group.GroupRole role;
        private LocalDateTime joinedAt;
        private LocalDateTime lastSeen;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSettingsResponse {
        private boolean allowMembersToInvite;
        private boolean allowMembersToChangeGroupInfo;
        private boolean allowMembersToSendMessages;
        private boolean allowMembersToSendMedia;
        private String groupDescription;
    }
}
