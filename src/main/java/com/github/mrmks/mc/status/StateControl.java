package com.github.mrmks.mc.status;

class StateControl {

    EnumSessionType type = EnumSessionType.NONE;
    EnumSessionState state = EnumSessionState.WAITING;

    boolean beginSession() {
        if (type == EnumSessionType.NONE && state == EnumSessionState.WAITING) {
            type = EnumSessionType.MANUAL;
            return true;
        }
        return false;
    }

    boolean beginAutoSession() {
        if (state == EnumSessionState.APPLY_ATTRIBUTE || state == EnumSessionState.APPLY_BUFF) return false;

        if (type == EnumSessionType.NONE && state == EnumSessionState.WAITING) {
            type = EnumSessionType.AUTO;
        }
        return true;
    }

    boolean isInSession() {
        return type == EnumSessionType.MANUAL || type == EnumSessionType.AUTO;
    }

    boolean canFinishSession() {
        return type != EnumSessionType.NONE;
    }

    boolean canFinishAutoSession() {
        return type == EnumSessionType.AUTO;
    }

    void finishSession() {
        type = EnumSessionType.NONE;
        state = EnumSessionState.WAITING;
    }

    boolean canCallEvent() {
        return type != EnumSessionType.NONE
                && state != EnumSessionState.APPLY_ATTRIBUTE
                && state != EnumSessionState.APPLY_BUFF;
    }

    boolean canCallEventDirect() {
        return canCallEvent() && (state == EnumSessionState.WAITING || state == EnumSessionState.WAITING_APPLY);
    }

    void beginHandler() {
        if (canCallEventDirect()) state = EnumSessionState.HANDLER;
    }

    void beginModifier() {
        if (state == EnumSessionState.HANDLER) state = EnumSessionState.MODIFIER;
    }

    void finishEventCall() {
        if (state == EnumSessionState.HANDLER || state == EnumSessionState.MODIFIER) {
            state = EnumSessionState.WAITING_APPLY;
        }
    }

    void applyAttribute() {
        if (type != EnumSessionType.NONE) {
            state = EnumSessionState.APPLY_ATTRIBUTE;
        }
    }

    void applyBuff() {
        if (type != EnumSessionType.NONE) state = EnumSessionState.APPLY_BUFF;
    }

    boolean shouldDenyEntityModify() {
        return type != EnumSessionType.NONE;
    }

    private enum EnumSessionState {
        WAITING, HANDLER, MODIFIER, WAITING_APPLY, APPLY_BUFF, APPLY_ATTRIBUTE
    }

    private enum EnumSessionType {
        NONE, AUTO, MANUAL
    }
}
