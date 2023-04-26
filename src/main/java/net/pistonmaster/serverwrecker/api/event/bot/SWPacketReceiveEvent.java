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
package net.pistonmaster.serverwrecker.api.event.bot;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.event.Cancellable;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.kyori.event.AbstractCancellable;

/**
 * This event is called when a packet is received from the connected server.
 * This event is called before the packet is processed by the default bot listener.
 * Setter is used to change the packet by a plugin to change the behaviour of the bot.
 */
@Getter
@AllArgsConstructor
public class SWPacketReceiveEvent extends AbstractCancellable implements ServerWreckerEvent, Cancellable {
    private final BotConnection connection;
    @Setter
    private MinecraftPacket packet;
}