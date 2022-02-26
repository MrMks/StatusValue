package com.github.mrmks.status.api;

import com.github.mrmks.status.Transaction;

public interface ModificationEvent {

    boolean isSrcSystem();
    boolean isSelfModify();

    int[] value();
    int[] initValue();

    int getSrc(int id);
    int getTar(int id);

    void modify(int index, int v);

    Transaction beginTransaction(boolean reserve);

    void setCanceled(boolean canceled);
    boolean isCanceled();
}
