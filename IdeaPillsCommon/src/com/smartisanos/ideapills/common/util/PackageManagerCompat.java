package com.smartisanos.ideapills.common.util;

public class PackageManagerCompat {
    public static final int MATCH_ALL = 0x00020000;
    public static final int MATCH_DIRECT_BOOT_UNAWARE = 0x00040000;
    public static final int MATCH_DIRECT_BOOT_AWARE = 0x00080000;
    public static final int DEFAULT_QUERY_FLAG = MATCH_DIRECT_BOOT_AWARE | MATCH_DIRECT_BOOT_UNAWARE;
}
