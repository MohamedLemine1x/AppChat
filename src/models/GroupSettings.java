package models;

/**
 * Group settings model
 * Contains configuration options for group functionality
 */
public class GroupSettings {
    private boolean onlyAdminsCanAdd = false;          // Only admins can add members
    private boolean onlyAdminsCanMessage = false;      // Only admins can send messages
    private boolean allowMembersToInvite = true;       // Regular members can invite others
    private boolean showMemberList = true;             // Show member list to all members
    private boolean allowFileSharing = true;           // Allow file sharing in the group

    // Default constructor
    public GroupSettings() {
    }

    // Full constructor
    public GroupSettings(boolean onlyAdminsCanAdd, boolean onlyAdminsCanMessage,
                         boolean allowMembersToInvite, boolean showMemberList,
                         boolean allowFileSharing) {
        this.onlyAdminsCanAdd = onlyAdminsCanAdd;
        this.onlyAdminsCanMessage = onlyAdminsCanMessage;
        this.allowMembersToInvite = allowMembersToInvite;
        this.showMemberList = showMemberList;
        this.allowFileSharing = allowFileSharing;
    }

    // Getters and setters

    public boolean isOnlyAdminsCanAdd() {
        return onlyAdminsCanAdd;
    }

    public void setOnlyAdminsCanAdd(boolean onlyAdminsCanAdd) {
        this.onlyAdminsCanAdd = onlyAdminsCanAdd;
    }

    public boolean isOnlyAdminsCanMessage() {
        return onlyAdminsCanMessage;
    }

    public void setOnlyAdminsCanMessage(boolean onlyAdminsCanMessage) {
        this.onlyAdminsCanMessage = onlyAdminsCanMessage;
    }

    public boolean isAllowMembersToInvite() {
        return allowMembersToInvite;
    }

    public void setAllowMembersToInvite(boolean allowMembersToInvite) {
        this.allowMembersToInvite = allowMembersToInvite;
    }

    public boolean isShowMemberList() {
        return showMemberList;
    }

    public void setShowMemberList(boolean showMemberList) {
        this.showMemberList = showMemberList;
    }

    public boolean isAllowFileSharing() {
        return allowFileSharing;
    }

    public void setAllowFileSharing(boolean allowFileSharing) {
        this.allowFileSharing = allowFileSharing;
    }

    @Override
    public String toString() {
        return "GroupSettings{" +
                "onlyAdminsCanAdd=" + onlyAdminsCanAdd +
                ", onlyAdminsCanMessage=" + onlyAdminsCanMessage +
                ", allowMembersToInvite=" + allowMembersToInvite +
                ", showMemberList=" + showMemberList +
                ", allowFileSharing=" + allowFileSharing +
                '}';
    }
}