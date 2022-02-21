package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.api.BuffType;

class BuffData {
    String key, tag, icon;
    boolean canRemove, anySrc;
    BuffType type;

    BuffData(BuffType type, String key, String tag, String icon, boolean canRemove, boolean anySrc) {
        this.type = type;
        this.key = key;
        this.tag = tag;
        this.icon = icon;
        this.canRemove = canRemove;
        this.anySrc = anySrc;
    }
}
