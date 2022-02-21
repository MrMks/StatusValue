package com.github.mrmks.mc.status;

public class Constants {

    public static final int TICKS_PER_SEC = 120;
    public static final int DEFAULT_ARRAY_INIT_CAPACITY_FACTOR = 8;
    public static final int DEFAULT_ARRAY_INIT_CAPACITY = 1 << DEFAULT_ARRAY_INIT_CAPACITY_FACTOR;

    public static final char SPLITTER = ':';
    public static final String SPLITTER_STR = String.valueOf(SPLITTER);

    public static final String SUFFIX_STEP = ":s";
    public static final String SUFFIX_BOUND = ":b";

    // attribute flags
    @Deprecated public static final byte FLAG_SINGLE = 0x10;
    @Deprecated public static final byte FLAG_STORE = 0x08;
    public static final byte FLAG_ATTRIBUTE = 0x0;
    public static final byte FLAG_RESOURCE = 0x01;
    public static final byte FLAG_SPECIAL = 0x02;
    public static final byte FLAG_STEP = 0x04;

    public static final short[] EMPTY_ARY_SHORT = new short[0];
    public static final int[] EMPTY_ARY_INT = new int[0];

}
