package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Group model class
 * Represents a group in the application
 */
public class Group {
    private String groupId;
    private String groupName;
    private String description;
    private String groupImageUrl;
    private String createdBy;
    private long createdAt;
    private List<String> members = new ArrayList<>();
    private List<String> admins = new ArrayList<>();
    private GroupSettings settings = new GroupSettings();
    private int maxMembers = 100;
    private boolean isPublic = false;
    private String inviteCode;
    private boolean isActive = true;
    private long lastActivity;

    // Default constructor
    public Group() {
    }

    // Constructor with essential fields
    public Group(String groupId, String groupName, String createdBy) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = this.createdAt;

        // Add creator as member and admin
        this.members.add(createdBy);
        this.admins.add(createdBy);

        // Generate initial invite code
        this.inviteCode = generateInviteCode();
    }

    // Getters and setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupImageUrl() {
        return groupImageUrl;
    }

    public void setGroupImageUrl(String groupImageUrl) {
        this.groupImageUrl = groupImageUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members != null ? members : new ArrayList<>();
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins != null ? admins : new ArrayList<>();
    }

    public GroupSettings getSettings() {
        return settings;
    }

    public void setSettings(GroupSettings settings) {
        this.settings = settings != null ? settings : new GroupSettings();
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    // Utility methods

    /**
     * Check if a user is a member of the group
     * @param userId User ID
     * @return true if the user is a member
     */
    public boolean isMember(String userId) {
        return members != null && members.contains(userId);
    }

    /**
     * Check if a user is an admin of the group
     * @param userId User ID
     * @return true if the user is an admin
     */
    public boolean isAdmin(String userId) {
        return (admins != null && admins.contains(userId)) || isCreator(userId);
    }

    /**
     * Check if a user is the creator of the group
     * @param userId User ID
     * @return true if the user is the creator
     */
    public boolean isCreator(String userId) {
        return createdBy != null && createdBy.equals(userId);
    }

    /**
     * Check if a user can edit the group
     * @param userId User ID
     * @return true if the user can edit the group
     */
    public boolean canEditGroup(String userId) {
        return isAdmin(userId);
    }

    /**
     * Add a member to the group
     * @param newMemberId ID of user to add
     * @param addedBy ID of user performing the action
     * @return true if member was added successfully
     */
    public boolean addMember(String newMemberId, String addedBy) {
        // Check if user is already a member
        if (isMember(newMemberId)) {
            return false;
        }

        // Check if group has reached max members
        if (members.size() >= maxMembers) {
            return false;
        }

        // Check if the user adding has permissions
        if (settings.isOnlyAdminsCanAdd() && !isAdmin(addedBy)) {
            return false;
        }

        // Add the member
        members.add(newMemberId);
        return true;
    }

    /**
     * Remove a member from the group
     * @param memberId ID of member to remove
     * @param removedBy ID of user performing the action
     * @return true if member was removed successfully
     */
    public boolean removeMember(String memberId, String removedBy) {
        // Check if user is a member
        if (!isMember(memberId)) {
            return false;
        }

        // Check permissions
        if (!isAdmin(removedBy) && !memberId.equals(removedBy)) {
            return false; // Only admins can remove others, or users can remove themselves
        }

        // Creator cannot be removed except by themselves
        if (isCreator(memberId) && !memberId.equals(removedBy)) {
            return false;
        }

        // Remove from members list
        members.remove(memberId);

        // If they were an admin, remove from admins list too
        if (admins.contains(memberId)) {
            admins.remove(memberId);
        }

        return true;
    }

    /**
     * Promote a member to admin
     * @param memberId ID of member to promote
     * @param promotedBy ID of user performing the action
     * @return true if member was promoted successfully
     */
    public boolean promoteToAdmin(String memberId, String promotedBy) {
        // Check if user is a member but not already an admin
        if (!isMember(memberId) || isAdmin(memberId)) {
            return false;
        }

        // Check if promoter is an admin
        if (!isAdmin(promotedBy)) {
            return false;
        }

        // Add to admins list
        admins.add(memberId);
        return true;
    }

    /**
     * Demote an admin to regular member
     * @param adminId ID of admin to demote
     * @param demotedBy ID of user performing the action
     * @return true if admin was demoted successfully
     */
    public boolean demoteFromAdmin(String adminId, String demotedBy) {
        // Check if user is an admin
        if (!isAdmin(adminId) || !admins.contains(adminId)) {
            return false;
        }

        // Creator cannot be demoted
        if (isCreator(adminId)) {
            return false;
        }

        // Check if demoter is an admin
        if (!isAdmin(demotedBy)) {
            return false;
        }

        // Remove from admins list
        admins.remove(adminId);
        return true;
    }

    /**
     * Check if user can send messages in the group
     * @param userId User ID
     * @return true if user can send messages
     */
    public boolean canSendMessages(String userId) {
        // Check if user is a member
        if (!isMember(userId)) {
            return false;
        }

        // Check if only admins can message
        if (settings.isOnlyAdminsCanMessage() && !isAdmin(userId)) {
            return false;
        }

        return true;
    }

    /**
     * Get user's role in the group
     * @param userId User ID
     * @return The user's role (CREATOR, ADMIN, MEMBER, or NONE)
     */
    public GroupRole getUserRole(String userId) {
        if (isCreator(userId)) {
            return GroupRole.CREATOR;
        } else if (isAdmin(userId)) {
            return GroupRole.ADMIN;
        } else if (isMember(userId)) {
            return GroupRole.MEMBER;
        } else {
            return GroupRole.NONE;
        }
    }

    /**
     * Generate a new invite code
     * @return The new invite code
     */
    public String generateInviteCode() {
        // Generate a random 8-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        this.inviteCode = code.toString();
        return this.inviteCode;
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", members=" + (members != null ? members.size() : 0) +
                ", isActive=" + isActive +
                '}';
    }
}