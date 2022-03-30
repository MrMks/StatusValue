package com.github.mrmks.status.api;

public interface IModifierBuff {
    String getName();
    String[] getGroups();
    SimpleDependency[] attributeDependencies();
    void handle(ModificationTable table, int[] v, short[] ids);
}
