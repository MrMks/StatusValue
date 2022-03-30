package com.github.mrmks.status.api;

public interface ModificationTable {

    boolean isSrcSystem();
    boolean isSelfModify();

    int getSrc(int id);
    int getTar(int id);

    void modifySrc(int id, int val);
    void modifyTar(int id, int val);

}
