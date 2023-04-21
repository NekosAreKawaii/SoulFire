/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;

import java.util.UUID;

public class ProtocolWrapper extends MinecraftProtocol {
    public ProtocolWrapper(String username) {
        super(username);
        setUseDefaultListeners(false);
    }

    public ProtocolWrapper(GameProfile profile, String accessToken) {
        super(profile, accessToken);
        setUseDefaultListeners(false);
    }

    public String getProfileName() {
        return getProfile().getName();
    }

    public UUID getUUID() {
        return getProfile().getId();
    }
}
