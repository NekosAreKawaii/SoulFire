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
package com.soulfiremc.server.account;

import com.soulfiremc.server.util.LenniHttpHelper;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.OnlineJavaData;
import com.soulfiremc.settings.proxy.SFProxy;
import java.util.concurrent.CompletableFuture;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.apache.commons.validator.routines.EmailValidator;

public final class SFJavaMicrosoftAuthService
  implements MCAuthService<SFJavaMicrosoftAuthService.JavaMicrosoftAuthData> {
  @Override
  public CompletableFuture<MinecraftAccount> login(JavaMicrosoftAuthData data, SFProxy proxyData) {
    return CompletableFuture.supplyAsync(() -> {
      StepFullJavaSession.FullJavaSession fullJavaSession;
      try {
        fullJavaSession = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          new StepCredentialsMsaCode.MsaCredentials(data.email, data.password));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      var mcProfile = fullJavaSession.getMcProfile();
      var mcToken = mcProfile.getMcToken();
      return new MinecraftAccount(
        AuthType.MICROSOFT_JAVA,
        mcProfile.getId(),
        mcProfile.getName(),
        new OnlineJavaData(mcToken.getAccessToken(), mcToken.getExpireTimeMs()));
    });
  }

  @Override
  public JavaMicrosoftAuthData createData(String data) {
    var split = data.split(":");

    if (split.length != 2) {
      throw new IllegalArgumentException("Invalid data!");
    }

    var email = split[0].trim();
    var password = split[1].trim();
    if (!EmailValidator.getInstance().isValid(email)) {
      throw new IllegalArgumentException("Invalid email!");
    }

    return new JavaMicrosoftAuthData(email, password);
  }

  public record JavaMicrosoftAuthData(String email, String password) {}
}
