package com.github.mrmks.mc.status.api;

import com.github.mrmks.mc.status.Transaction;

public interface ModificationEvent {
    int[] value();
    int[] initValue();

    int getSrc(int id);
    int getTar(int id);

    void modify(int index, int v);

    Transaction beginTransaction(boolean reserve);

    void setCanceled(boolean canceled);
    boolean isCanceled();
}
