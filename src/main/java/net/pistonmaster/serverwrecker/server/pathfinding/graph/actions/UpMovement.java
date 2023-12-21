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
package net.pistonmaster.serverwrecker.server.pathfinding.graph.actions;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.server.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.server.pathfinding.Costs;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.server.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.server.pathfinding.execution.JumpAndPlaceBelowAction;
import net.pistonmaster.serverwrecker.server.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.GraphInstructions;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.actions.movement.BlockDirection;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.actions.movement.BlockSafetyData;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.actions.movement.MovementMiningCost;
import net.pistonmaster.serverwrecker.server.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.server.util.VectorHelper;

import java.util.List;

@Slf4j
public final class UpMovement implements GraphAction, Cloneable {
    private static final SWVec3i FEET_POSITION_RELATIVE_BLOCK = SWVec3i.ZERO;
    private final SWVec3i targetFeetBlock;
    @Getter
    private MovementMiningCost[] blockBreakCosts;
    @Getter
    private boolean[] unsafeToBreak;
    @Getter
    private boolean[] noNeedToBreak;
    @Setter
    @Getter
    private boolean impossible = false;

    public UpMovement() {
        this.targetFeetBlock = FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0);

        this.blockBreakCosts = new MovementMiningCost[freeCapacity()];
        this.unsafeToBreak = new boolean[freeCapacity()];
        this.noNeedToBreak = new boolean[freeCapacity()];
    }

    private int freeCapacity() {
        return 1;
    }

    public List<SWVec3i> listRequiredFreeBlocks() {
        List<SWVec3i> requiredFreeBlocks = new ObjectArrayList<>(freeCapacity());

        // The one above the head to jump
        requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0));

        return requiredFreeBlocks;
    }

    public BlockSafetyData[][] listCheckSafeMineBlocks() {
        var requiredFreeBlocks = listRequiredFreeBlocks();
        var results = new BlockSafetyData[requiredFreeBlocks.size()][];

        var firstDirection = BlockDirection.NORTH;
        var oppositeDirection = firstDirection.opposite();
        var leftDirectionSide = firstDirection.leftSide();
        var rightDirectionSide = firstDirection.rightSide();

        var aboveHead = FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0);
        results[requiredFreeBlocks.indexOf(aboveHead)] = new BlockSafetyData[]{
                new BlockSafetyData(aboveHead.add(0, 1, 0), BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS),
                new BlockSafetyData(oppositeDirection.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
                new BlockSafetyData(leftDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
                new BlockSafetyData(rightDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS)
        };

        return results;
    }

    @Override
    public boolean impossibleToComplete() {
        return impossible;
    }

    @Override
    public GraphInstructions getInstructions(BotEntityState previousEntityState) {
        var actions = new ObjectArrayList<WorldAction>();
        var inventory = previousEntityState.inventory();
        var levelState = previousEntityState.levelState();
        var cost = Costs.JUMP_UP_AND_PLACE_BELOW;

        for (var breakCost : blockBreakCosts) {
            if (breakCost == null) {
                continue;
            }

            cost += breakCost.miningCost();
            actions.add(new BlockBreakAction(breakCost.block()));
            if (breakCost.willDrop()) {
                inventory = inventory.withOneMoreBlock();
            }

            levelState = levelState.withChangeToAir(breakCost.block());
        }

        // Change values for block we're going to place and stand on
        inventory = inventory.withOneLessBlock();
        levelState = levelState.withChangeToSolidBlock(previousEntityState.positionBlock());

        var absoluteTargetFeetBlock = previousEntityState.positionBlock().add(targetFeetBlock);
        var targetFeetDoublePosition = VectorHelper.middleOfBlockNormalize(absoluteTargetFeetBlock.toVector3d());

        // Where we are standing right now, we'll place the target block below us after jumping
        actions.add(new JumpAndPlaceBelowAction(previousEntityState.positionBlock(), new BotActionManager.BlockPlaceData(
                previousEntityState.positionBlock().sub(0, 1, 0),
                Direction.UP
        )));

        return new GraphInstructions(new BotEntityState(
                targetFeetDoublePosition,
                absoluteTargetFeetBlock,
                levelState,
                inventory
        ), cost, actions);
    }

    @Override
    public UpMovement copy(BotEntityState previousEntityState) {
        var upMovement = this.clone();
        upMovement.impossible = !previousEntityState.inventory().hasBlockToPlace();
        return upMovement;
    }

    @Override
    public UpMovement clone() {
        try {
            var c = (UpMovement) super.clone();

            c.blockBreakCosts = this.blockBreakCosts == null ? null : new MovementMiningCost[this.blockBreakCosts.length];
            c.unsafeToBreak = this.unsafeToBreak == null ? null : new boolean[this.unsafeToBreak.length];
            c.noNeedToBreak = this.noNeedToBreak == null ? null : new boolean[this.noNeedToBreak.length];

            return c;
        } catch (CloneNotSupportedException cantHappen) {
            throw new InternalError();
        }
    }
}