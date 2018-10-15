package com.smartisanos.ideapills.sync.share;

import android.text.TextUtils;

import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyncShareUtils {

    public static boolean canAddAnotherInvitation(long userId, List<SyncShareInvitation> syncShareInvitationList) {
        if (syncShareInvitationList == null) {
            return true;
        }
        boolean ret = true;
        for (SyncShareInvitation item : syncShareInvitationList) {
            if ((item.inviter.id == userId && item.inviteStatus == SyncShareInvitation.INVITE_START)
                    || item.inviteStatus == SyncShareInvitation.INVITE_ACCEPT) {
                ret = false;
                break;
            }
        }
        return ret;
    }

    public static boolean isWaitingParticipantRemoved(long userId, List<SyncShareInvitation> lastSyncShareInvitationList,
                                                      List<SyncShareInvitation> nowSyncShareInvitationList) {
        if (lastSyncShareInvitationList == null) {
            return false;
        }
        List<Long> waitingInviteeIds = new ArrayList<>();
        for (SyncShareInvitation syncShareInvitation : lastSyncShareInvitationList) {
            if (syncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_START) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                    if (syncShareInvitation.inviter.id == userId
                            && syncShareInvitation.invitee.id != userId) {
                        waitingInviteeIds.add(syncShareInvitation.invitee.id);
                    }
                }
            }
        }
        if (waitingInviteeIds.isEmpty()) {
            return false;
        }
        if (nowSyncShareInvitationList == null || nowSyncShareInvitationList.isEmpty()) {
            return true;
        } else {
            for (SyncShareInvitation syncShareInvitation : nowSyncShareInvitationList) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                    if (syncShareInvitation.inviter.id == userId
                            && waitingInviteeIds.contains(syncShareInvitation.invitee.id)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public static boolean hasSuccessParticipant(List<SyncShareInvitation> syncShareInvitationList) {
        if (syncShareInvitationList == null) {
            return false;
        }
        for (SyncShareInvitation syncShareInvitation : syncShareInvitationList) {
            if (syncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                   return true;
                }
            }
        }
        return false;
    }

    public static List<Long> findAllParticipantIdsCanAdd(long userId, List<SyncShareInvitation> syncShareInvitationList) {
        if (userId < 0 || syncShareInvitationList == null) {
            return new ArrayList<>();
        }
        ArrayList<Long> participantUserIds = new ArrayList<>();
        for (SyncShareInvitation syncShareInvitation : syncShareInvitationList) {
            if (syncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                    if (syncShareInvitation.inviter.id == userId
                            && syncShareInvitation.invitee.id != userId) {
                        participantUserIds.add(syncShareInvitation.invitee.id);
                    } else if (syncShareInvitation.inviter.id != userId
                            && syncShareInvitation.invitee.id == userId) {
                        participantUserIds.add(syncShareInvitation.inviter.id);
                    }
                }
            }
        }
        return participantUserIds;
    }

    public static List<Long> findParticipantIdsForSendInvitation(long userId, List<SyncShareInvitation> syncShareInvitationList) {
        if (userId < 0 || syncShareInvitationList == null) {
            return new ArrayList<>();
        }
        ArrayList<Long> participantUserIds = new ArrayList<>();
        for (SyncShareInvitation syncShareInvitation : syncShareInvitationList) {
            if (syncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_START) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                    if (syncShareInvitation.inviter.id == userId
                            && syncShareInvitation.invitee.id != userId) {
                        participantUserIds.add(syncShareInvitation.invitee.id);
                        break;
                    }
                }
            }
        }
        return participantUserIds;
    }

    public static List<Long> findParticipantIdsForAdd(long userId, List<SyncShareInvitation> syncShareInvitationList) {
        if (userId < 0 || syncShareInvitationList == null) {
            return new ArrayList<>();
        }
        ArrayList<Long> participantUserIds = new ArrayList<>();
        for (SyncShareInvitation syncShareInvitation : syncShareInvitationList) {
            if (syncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                    if (syncShareInvitation.inviter.id == userId
                            && syncShareInvitation.invitee.id != userId) {
                        participantUserIds.add(syncShareInvitation.invitee.id);
                        break;
                    } else if (syncShareInvitation.inviter.id != userId
                            && syncShareInvitation.invitee.id == userId) {
                        participantUserIds.add(syncShareInvitation.inviter.id);
                        break;
                    }
                }
            }
        }
        return participantUserIds;
    }

    public static List<Long> findParticipantIdsForRemove(long userId, boolean isShareFromOthers,
                                                            List<SyncShareInvitation> syncShareInvitationList) {
        if (userId < 0 || syncShareInvitationList == null) {
            return new ArrayList<>();
        }
        ArrayList<Long> participantUserIds = new ArrayList<>();
        for (SyncShareInvitation syncShareInvitation : syncShareInvitationList) {
            if (syncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT) {
                if (syncShareInvitation.invitee != null && syncShareInvitation.inviter != null) {
                    if (isShareFromOthers) {
                        if (syncShareInvitation.inviter.id == userId) {
                            participantUserIds.add(userId);
                            break;
                        } else if (syncShareInvitation.invitee.id == userId) {
                            participantUserIds.add(userId);
                            break;
                        }
                    } else {
                        if (syncShareInvitation.inviter.id == userId
                                && syncShareInvitation.invitee.id != userId) {
                            participantUserIds.add(syncShareInvitation.invitee.id);
                        } else if (syncShareInvitation.inviter.id != userId
                                && syncShareInvitation.invitee.id == userId) {
                            participantUserIds.add(syncShareInvitation.inviter.id);
                        }
                    }
                }
            }
        }
        return participantUserIds;
    }
}