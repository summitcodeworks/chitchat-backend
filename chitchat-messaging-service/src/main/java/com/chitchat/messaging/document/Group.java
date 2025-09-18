package com.chitchat.messaging.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Group document for MongoDB
 */
@Document(collection = "groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    private String avatarUrl;
    private Long adminId;
    private List<GroupMember> members;
    private GroupSettings settings;
    private LocalDateTime lastActivity;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMember {
        private Long userId;
        private GroupRole role;
        private LocalDateTime joinedAt;
        private LocalDateTime lastSeen;
    }
    
    public enum GroupRole {
        ADMIN, MODERATOR, MEMBER
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSettings {
        private boolean allowMembersToInvite;
        private boolean allowMembersToChangeGroupInfo;
        private boolean allowMembersToSendMessages;
        private boolean allowMembersToSendMedia;
        private String groupDescription;
    }
}
