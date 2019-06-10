package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKConversation extends VKModel implements Serializable {

    public static int count;
    public static ArrayList<VKUser> users = new ArrayList<>();
    public static ArrayList<VKGroup> groups = new ArrayList<>();

    private int conversationsCount;
    private int readIn;
    private int readOut;
    private int lastMessageId;
    private int unread;
    private int membersCount;

    private boolean canWrite;
    private int reason;

    private boolean read;
    private boolean groupChannel;

    private VKMessage pinned, last;

    private String title;
    private Type type;
    private String state;
    private String photo50, photo100, photo200;

    //other
    private ArrayList<VKUser> conversationUsers;
    private ArrayList<VKGroup> conversationGroups;

    //acl
    private boolean canChangePin, canChangeInfo, canChangeInviteLink, canInvite, canPromoteUsers, canSeeInviteLink;

    //push_settings
    private int disabledUntil;
    private boolean disabledForever;
    private boolean noSound;

    public enum Type {
        CHAT, GROUP, USER
    }

    public enum Reason {
        KICKED, LEFT, USER_DELETED, USER_BLACKLIST, USER_PRIVACY, MESSAGES_OFF, MESSAGES_BLOCKED, NO_ACCESS_CHAT, NO_ACCESS_EMAIL, NO_ACCESS_GROUP
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }

    public VKConversation() {

    }

    public static int getReason(Reason reason) {
        switch (reason) {
            case USER_BLACKLIST:
                return 900;
            case USER_PRIVACY:
                return 902;
            case USER_DELETED:
                return 18;
            case LEFT:
                return 2;
            case KICKED:
                return 1;
            case MESSAGES_OFF:
                return 915;
            case MESSAGES_BLOCKED:
                return 916;
            case NO_ACCESS_CHAT:
                return 917;
            case NO_ACCESS_EMAIL:
                return 918;
            case NO_ACCESS_GROUP:
                return 203;
            default:
                return -1;
        }
    }

    public static Reason getReason(int reason) {
        switch (reason) {
            case 900:
                return Reason.USER_BLACKLIST;
            case 902:
                return Reason.USER_PRIVACY;
            case 18:
                return Reason.USER_DELETED;
            case 2:
                return Reason.LEFT;
            case 1:
                return Reason.KICKED;
            case 915:
                return Reason.MESSAGES_OFF;
            case 916:
                return Reason.MESSAGES_BLOCKED;
            case 917:
                return Reason.NO_ACCESS_CHAT;
            case 918:
                return Reason.NO_ACCESS_EMAIL;
            case 203:
                return Reason.NO_ACCESS_GROUP;
            default:
                return null;
        }
    }

    public VKConversation(JSONObject o, JSONObject msg) throws JSONException {
        conversationsCount = count;
        conversationGroups = groups;
        conversationUsers = users;

        groups = null;
        users = null;

        if (msg != null)
            last = new VKMessage(msg);

        JSONObject peer = o.optJSONObject("peer");
        this.type = msg != null ? getType(this.last.getPeerId()) : getType(peer.optString("type"));

        this.readIn = o.optInt("in_read");
        this.readOut = o.optInt("out_read");
        this.lastMessageId = o.optInt("last_message_id");
        this.unread = o.optInt("unread_count");

        this.read = this.last == null || (this.last.isOut() && this.readOut == this.lastMessageId || !this.last.isOut() && this.readIn == this.lastMessageId);

        JSONObject canWrite = o.optJSONObject("can_write");
        this.canWrite = canWrite.optBoolean("allowed");
        if (!this.canWrite) {
            this.reason = canWrite.optInt("reason", -1);
        }

        JSONObject push_settings = o.optJSONObject("push_settings");
        if (push_settings != null) {
            this.disabledUntil = push_settings.optInt("disabled_until", -1);
            this.disabledForever = push_settings.optBoolean("disabled_forever", false);
            this.noSound = push_settings.optBoolean("no_sound");
        }

        JSONObject ch = o.optJSONObject("chat_settings");
        if (ch != null) {
            this.title = ch.optString("title");
            this.membersCount = ch.optInt("members_count");
            this.state = ch.optString("state");
            this.groupChannel = ch.optBoolean("is_group_channel");

            JSONObject photo = ch.optJSONObject("photo");
            if (photo != null) {
                this.photo50 = photo.optString("photo_50");
                this.photo100 = photo.optString("photo_100");
                this.photo200 = photo.optString("photo_200");
            }

            JSONObject acl = ch.optJSONObject("acl");
            if (acl != null) {
                this.canInvite = acl.optBoolean("can_invite");
                this.canPromoteUsers = acl.optBoolean("can_promote_users");
                this.canSeeInviteLink = acl.optBoolean("can_see_invite_link");
                this.canChangeInviteLink = acl.optBoolean("can_change_invite_link");
                this.canChangeInfo = acl.optBoolean("can_change_info");
                this.canChangePin = acl.optBoolean("can_change_pin");
            }

            JSONObject pinned = ch.optJSONObject("pinned_message");
            if (pinned != null) {
                this.pinned = new VKMessage(pinned);
            }
        }
    }

    public boolean isGroupChannel() {
        return groupChannel;
    }

    public void setGroupChannel(boolean groupChannel) {
        this.groupChannel = groupChannel;
    }

    public static Type getType(int peerId) {
        if (VKConversation.isChatId(peerId)) return Type.CHAT;
        if (VKGroup.isGroupId(peerId)) return Type.GROUP;
        return Type.USER;
    }

    public Type getType() {
        return type;
    }

    public static Type getType(String type) {
        switch (type) {
            case "user":
                return Type.USER;
            case "group":
                return Type.GROUP;
            case "chat":
                return Type.CHAT;
            default:
                return null;
        }
    }

    public static String getType(Type type) {
        switch (type) {
            case USER:
                return "user";
            case GROUP:
                return "group";
            case CHAT:
                return "chat";
            default:
                return null;
        }
    }

    private static boolean isChatId(int peerId) {
        return peerId > 2_000_000_00;
    }

    public boolean isChat() {
        return isChatId(this.last.getPeerId());
    }

    public boolean isFromGroup() {
        return VKGroup.isGroupId(this.last.getFromId());
    }

    public boolean isGroup() {
        return this.last != null && VKGroup.isGroupId(this.last.getPeerId());
    }

    public boolean isUser() {
        return !isGroup() && !isChat() && !isGroupChannel();
    }

    public boolean isFromUser() {
        return !isFromGroup();
    }

    public boolean isNotificationsDisabled() {
        return this.disabledForever || this.disabledUntil > 0 || this.noSound;
    }

    public int getConversationsCount() {
        return conversationsCount;
    }

    public void setConversationsCount(int conversationsCount) {
        this.conversationsCount = conversationsCount;
    }

    public int getReadIn() {
        return readIn;
    }

    public void setReadIn(int readIn) {
        this.readIn = readIn;
    }

    public int getReadOut() {
        return readOut;
    }

    public void setReadOut(int readOut) {
        this.readOut = readOut;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public VKMessage getPinned() {
        return pinned;
    }

    public void setPinned(VKMessage pinned) {
        this.pinned = pinned;
    }

    public VKMessage getLast() {
        return last;
    }

    public void setLast(VKMessage last) {
        this.last = last;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPhoto50() {
        return photo50;
    }

    public void setPhoto50(String photo50) {
        this.photo50 = photo50;
    }

    public String getPhoto100() {
        return photo100;
    }

    public void setPhoto100(String photo100) {
        this.photo100 = photo100;
    }

    public String getPhoto200() {
        return photo200;
    }

    public void setPhoto200(String photo200) {
        this.photo200 = photo200;
    }

    public ArrayList<VKUser> getConversationUsers() {
        return conversationUsers;
    }

    public void setConversationUsers(ArrayList<VKUser> conversationUsers) {
        this.conversationUsers = conversationUsers;
    }

    public ArrayList<VKGroup> getConversationGroups() {
        return conversationGroups;
    }

    public void setConversationGroups(ArrayList<VKGroup> conversationGroups) {
        this.conversationGroups = conversationGroups;
    }

    public boolean isCanChangePin() {
        return canChangePin;
    }

    public void setCanChangePin(boolean canChangePin) {
        this.canChangePin = canChangePin;
    }

    public boolean isCanChangeInfo() {
        return canChangeInfo;
    }

    public void setCanChangeInfo(boolean canChangeInfo) {
        this.canChangeInfo = canChangeInfo;
    }

    public boolean isCanChangeInviteLink() {
        return canChangeInviteLink;
    }

    public void setCanChangeInviteLink(boolean canChangeInviteLink) {
        this.canChangeInviteLink = canChangeInviteLink;
    }

    public boolean isCanInvite() {
        return canInvite;
    }

    public void setCanInvite(boolean canInvite) {
        this.canInvite = canInvite;
    }

    public boolean isCanPromoteUsers() {
        return canPromoteUsers;
    }

    public void setCanPromoteUsers(boolean canPromoteUsers) {
        this.canPromoteUsers = canPromoteUsers;
    }

    public boolean isCanSeeInviteLink() {
        return canSeeInviteLink;
    }

    public void setCanSeeInviteLink(boolean canSeeInviteLink) {
        this.canSeeInviteLink = canSeeInviteLink;
    }

    public int getDisabledUntil() {
        return disabledUntil;
    }

    public void setDisabledUntil(int disabledUntil) {
        this.disabledUntil = disabledUntil;
    }

    public boolean isDisabledForever() {
        return disabledForever;
    }

    public void setDisabledForever(boolean disabledForever) {
        this.disabledForever = disabledForever;
    }

    public boolean isNoSound() {
        return noSound;
    }

    public void setNoSound(boolean noSound) {
        this.noSound = noSound;
    }
}
