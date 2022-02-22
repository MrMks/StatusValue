package com.github.mrmks.status;

import com.github.mrmks.status.api.IHandler;
import com.github.mrmks.status.api.ModificationEvent;

import java.util.Arrays;

class WrappedHandler {

    final String name;
    final IHandler handler;

    short[] aIds;
    short[] mIds;
    short[] mCvt;
    int paramIndex;

    WrappedHandler(String n, IHandler h) {
        this.handler = h;
        this.name = n;
    }

    void handle(short modifierId, ModificationEvent event, int[] params) {
        int i = Arrays.binarySearch(mIds, modifierId);
        if (i >= 0) handler.handle(mCvt[i], event, aIds, params);
    }
}
