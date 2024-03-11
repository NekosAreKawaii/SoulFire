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
package com.soulfiremc.dedicated;

import com.soulfiremc.launcher.SoulFireAbstractBootstrap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoulFireDedicatedBootstrap extends SoulFireAbstractBootstrap {
  private SoulFireDedicatedBootstrap() {
    super();
  }

  @SuppressWarnings("unused")
  public static void bootstrap(String[] args, List<ClassLoader> classLoaders) {
    new SoulFireDedicatedBootstrap().internalBootstrap(args, classLoaders);
  }

  @Override
  protected void postMixinMain(String[] args) {
    var host = getRPCHost();
    var port = getRPCPort();

    log.info("Starting server on {}:{}", host, port);
    SoulFireDedicatedLoader.runDedicated(host, port);
  }
}