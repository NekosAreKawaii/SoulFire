package net.pistonmaster.serverwrecker.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public enum GameVersion {

    VERSION_1_7("1.7.4"),

    VERSION_1_8("1.8.9"),

    VERSION_1_9("1.9.4"),

    VERSION_1_10("1.10.2"),

    VERSION_1_11("1.11.2"),

    VERSION_1_12("1.12.2"),

    VERSION_1_13("1.13.2"),

    VERSION_1_14("1.14.4"),

    VERSION_1_15("1.15.2"),

    VERSION_1_16("1.16.5"),

    VERSION_1_17("1.17.1");

    private final String version;

    GameVersion(String version) {
        this.version = version;
    }

    public static GameVersion findByName(String name) {
        for (GameVersion version : values()) {
            if (version.version.equals(name)) {
                return version;
            }
        }

        return null;
    }

    public static GameVersion getNewest() {
        return Arrays.stream(GameVersion.values())
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toUnmodifiableList()).get(0);
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return getVersion();
    }
}