package com.github.mrmks.mc.status;

import java.util.function.ToIntFunction;

public interface Transaction {

    // for next four method, you should provide integers like this: id, val, id, val.
    // if you provide an array in odd size, we will ignore the last number.
    Transaction modifySrc(int... args);
    Transaction modifyTar(int... args);
    Transaction setSrc(int... args);
    Transaction setTar(int... args);

    // we will call your function with the same order of srcIds.
    // if any id is illegal, the value of it will be set to 0.
    Transaction setSrc(int tarId, ToIntFunction<int[]> func, int... srcIds);
    Transaction setTar(int tarId, ToIntFunction<int[]> func, int... srcIds);

    // notice the size limit of the handlerParams.
    Transaction paramHandler(int id, int... args);

    void modify(int id, int[] value);
}