package net.pistonmaster.serverwrecker.data;

import lombok.AccessLevel;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record BlockType(int id, String name, float destroyTime, float explosionResistance,
                        boolean air, boolean fallingBlock, boolean replaceable,
                        boolean requiresCorrectToolForDrops, boolean fluidSource,
                        OffsetData offsetData, List<BlockShapeType> blockShapeTypes) {
    public static final List<BlockType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static BlockType register(String name) {
        var blockType = GsonDataHelper.fromJson("/minecraft/blocks.json", name, BlockType.class)
                .withBlockShapeTypes(BlockStateLoader.getBlockShapes(name));
        VALUES.add(blockType);
        return blockType;
    }

    public static BlockType getById(int id) {
        for (var blockType : VALUES) {
            if (blockType.id() == id) {
                return blockType;
            }
        }

        return null;
    }

    public static BlockType getByName(String name) {
        for (var blockType : VALUES) {
            if (blockType.name().equals(name) || ("minecraft:" + blockType.name()).equals(name)) {
                return blockType;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockType blockType)) return false;
        return id == blockType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public record OffsetData(float maxHorizontalOffset, float maxVerticalOffset, OffsetType offsetType) {
        public enum OffsetType {
            XZ,
            XYZ
        }
    }
}
