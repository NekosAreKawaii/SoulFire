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
package com.soulfiremc.settings.proxy;

import com.soulfiremc.grpc.generated.ProxyProto;
import java.net.InetSocketAddress;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record SFProxy(
    @NonNull ProxyType type,
    @NonNull String host,
    int port,
    @Nullable String username,
    @Nullable String password) {
  public SFProxy {
    if (type == ProxyType.SOCKS4 && password != null) {
      throw new IllegalArgumentException("SOCKS4 does not support passwords!");
    } else if (username != null && username.isBlank()) {
      // Sanitize empty strings
      username = null;
    } else if (password != null && password.isBlank()) {
        // Sanitize empty strings
      password = null;
    }

    if (username == null && password != null) {
      throw new IllegalArgumentException("Username must be set if password is set!");
    }
  }

  public static SFProxy fromProto(ProxyProto proto) {
    return new SFProxy(
        ProxyType.valueOf(proto.getType().name()),
        proto.getHost(),
        proto.getPort(),
        proto.hasUsername() ? proto.getUsername() : null,
        proto.hasPassword() ? proto.getPassword() : null);
  }

  public InetSocketAddress getInetSocketAddress() {
    return new InetSocketAddress(host, port);
  }

  public ProxyProto toProto() {
    var builder =
        ProxyProto.newBuilder()
            .setType(ProxyProto.Type.valueOf(type.name()))
            .setHost(host)
            .setPort(port);

    if (username != null) {
      builder.setUsername(username);
    }

    if (password != null) {
      builder.setPassword(password);
    }

    return builder.build();
  }
}
