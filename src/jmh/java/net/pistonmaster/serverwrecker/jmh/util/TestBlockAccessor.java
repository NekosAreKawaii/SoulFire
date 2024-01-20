/*
 * ServerWrecker
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.serverwrecker.jmh.util;

import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockAccessor;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class TestBlockAccessor implements BlockAccessor {
    private final Map<Vector3i, BlockState> blocks = new HashMap<>();

    public void setBlockAt(int x, int y, int z, BlockType block) {
        blocks.put(Vector3i.from(x, y, z), BlockState.forDefaultBlockType(block));
    }

    @Override
    public BlockState getBlockStateAt(int x, int y, int z) {
        var block = blocks.get(Vector3i.from(x, y, z));
        if (block == null) {
            return BlockState.forDefaultBlockType(BlockType.VOID_AIR);
        }

        return block;
    }
}