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
package com.soulfiremc.account;

import com.soulfiremc.account.service.AccountData;
import com.soulfiremc.account.service.BedrockData;
import com.soulfiremc.account.service.OfflineJavaData;
import com.soulfiremc.account.service.OnlineJavaData;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import java.util.UUID;
import lombok.NonNull;

/**
 * Represents an authenticated MC account.
 * This can be a premium, offline or bedrock account.
 * Beware that the profileId is not a valid online UUID for offline and bedrock accounts.
 *
 * @param authType The type of authentication
 * @param profileId Identifier that uniquely identifies the account
 * @param lastKnownName The last known name of the account
 * @param accountData The data of the account (values depend on the authType)
 */
public record MinecraftAccount(
    @NonNull AuthType authType,
    @NonNull UUID profileId,
    @NonNull String lastKnownName,
    @NonNull AccountData accountData) {
  public static MinecraftAccount fromProto(MinecraftAccountProto account) {
    return new MinecraftAccount(AuthType.valueOf(account.getType().name()), UUID.fromString(account.getProfileId()), account.getLastKnownName(), switch (account.getAccountDataCase()) {
      case ONLINEJAVADATA -> OnlineJavaData.fromProto(account.getOnlineJavaData());
      case OFFLINEJAVADATA -> OfflineJavaData.fromProto(account.getOfflineJavaData());
      case BEDROCKDATA -> BedrockData.fromProto(account.getBedrockData());
      case ACCOUNTDATA_NOT_SET -> throw new IllegalArgumentException("AccountData not set");
    });
  }

  @Override
  public String toString() {
    return String.format(
        "MinecraftAccount(authType=%s, profileId=%s, lastKnownName=%s)", authType, profileId, lastKnownName);
  }

  public boolean isPremiumJava() {
    return accountData instanceof OnlineJavaData;
  }

  public boolean isPremiumBedrock() {
    return accountData instanceof BedrockData;
  }

  public MinecraftAccountProto toProto() {
    var builder =
        MinecraftAccountProto.newBuilder()
            .setType(MinecraftAccountProto.AccountTypeProto.valueOf(authType.name()))
            .setProfileId(profileId.toString())
            .setLastKnownName(lastKnownName);

    switch (accountData) {
      case BedrockData bedrockData -> {
        builder.setBedrockData(bedrockData.toProto());
      }
      case OfflineJavaData offlineJavaData -> {
        builder.setOfflineJavaData(offlineJavaData.toProto());
      }
      case OnlineJavaData onlineJavaData -> {
        builder.setOnlineJavaData(onlineJavaData.toProto());
      }
    }

    return builder.build();
  }
}
