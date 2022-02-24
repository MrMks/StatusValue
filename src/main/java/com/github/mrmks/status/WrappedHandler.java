package com.github.mrmks.status;

import com.github.mrmks.status.api.IHandler;
import com.github.mrmks.status.api.ModificationEvent;

import java.util.Arrays;

class WrappedHandler {

    final String name;
    final IHandler handler;
    final boolean handleCanceled;

    short[] aIds;
    short[] mIds;
    short[] mCvt;
    int paramIndex;

    WrappedHandler(String n, IHandler h) {
        this.name = n;
        this.handler = h;
        this.handleCanceled = h.handleCanceled();
    }

    void handle(short modifierId, ModificationEvent event, int[] params) {
        int i = Arrays.binarySearch(mIds, modifierId);
        if (i >= 0) handler.handle(mCvt[i], event, aIds, params);
    }
}
