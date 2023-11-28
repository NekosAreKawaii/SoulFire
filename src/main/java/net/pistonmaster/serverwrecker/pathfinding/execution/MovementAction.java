/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.pathfinding.execution;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3d;

@ToString
@RequiredArgsConstructor
public class MovementAction implements WorldAction {
    private final Vector3d position;
    // Corner jumps normally require you to stand closer to the block to jump
    private final boolean walkFewTicksNoJump;
    private boolean didLook = false;
    private int noJumpTicks = 0;

    @Override
    public boolean isCompleted(BotConnection connection) {
        var movementManager = connection.sessionDataManager().getBotMovementManager();
        var botPosition = movementManager.getPlayerPos();
        var levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        var blockMeta = levelState.getBlockStateAt(position.toInt());
        var insideBlock = blockMeta.isPresent() && !BlockTypeHelper.isEmpty(blockMeta.get());

        if (insideBlock) {
            // We are inside a block, so being close is good enough
            var distance = botPosition.distance(position);
            return distance <= 1;
        } else if (botPosition.getY() != position.getY()) {
            // We want to be on the same Y level
            return false;
        } else {
            var distance = botPosition.distance(position);
            // Close enough to be able to bridge up
            return distance <= 0.2;
        }
    }

    @Override
    public void tick(BotConnection connection) {
        var movementManager = connection.sessionDataManager().getBotMovementManager();
        var botPosition = movementManager.getPlayerPos();

        var previousYaw = movementManager.getYaw();
        movementManager.lookAt(RotationOrigin.EYES, position);
        movementManager.getEntity().setPitch(0);
        var newYaw = movementManager.getYaw();

        var yawDifference = Math.abs(previousYaw - newYaw);

        // We should only set the yaw once to the server to prevent the bot looking weird due to inaccuracy
        if (didLook && yawDifference > 5) {
            movementManager.getEntity().setYaw(movementManager.getYaw());
        } else {
            didLook = true;
        }

        // Don't let the bot look up or down (makes it look weird)
        movementManager.getControlState().resetAll();
        movementManager.getControlState().setForward(true);

        if (position.getY() > botPosition.getY() && shouldJump()) {
            movementManager.getControlState().setJumping(true);
        }
    }

    private boolean shouldJump() {
        if (!walkFewTicksNoJump) {
            return true;
        }

        if (noJumpTicks < 4) {
            noJumpTicks++;
            return false;
        } else {
            noJumpTicks = 0;
            return true;
        }
    }

    @Override
    public int getAllowedTicks() {
        // 5-seconds max to walk to a block
        return 5 * 20;
    }
}
