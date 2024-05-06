/*
 * SoulFire
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
package com.soulfiremc.server.pathfinding.execution;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.soulfiremc.server.pathfinding.BotEntityState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import com.soulfiremc.server.util.BlockTypeHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class BlockPlaceAction implements WorldAction {
  @Getter
  private final SFVec3i blockPosition;
  private final BotActionManager.BlockPlaceAgainstData blockPlaceAgainstData;
  private boolean putOnHotbar = false;
  private boolean finishedPlacing = false;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.dataManager().currentLevel();

    return BlockTypeHelper.isFullBlock(level.getBlockState(blockPosition));
  }

  @Override
  public SFVec3i targetPosition(BotConnection connection) {
    return SFVec3i.fromDouble(connection.dataManager().clientEntity().pos());
  }

  @Override
  public void tick(BotConnection connection) {
    var dataManager = connection.dataManager();
    dataManager.controlState().resetAll();

    if (!putOnHotbar) {
      if (ItemPlaceHelper.placeBestBlockInHand(dataManager)) {
        putOnHotbar = true;
      }

      return;
    }

    if (finishedPlacing) {
      return;
    }

    connection.dataManager().botActionManager().placeBlock(Hand.MAIN_HAND, blockPlaceAgainstData);
    finishedPlacing = true;
  }

  @Override
  public int getAllowedTicks() {
    // 3-seconds max to place a block
    return 3 * 20;
  }

  @Override
  public BotEntityState simulate(BotEntityState state) {
    return new BotEntityState(state.blockPosition(),
      state.level().withChangeToSolidBlock(blockPosition),
      state.inventory().withOneLessBlock());
  }

  @Override
  public String toString() {
    return "BlockPlaceAction -> " + blockPosition.formatXYZ();
  }
}
