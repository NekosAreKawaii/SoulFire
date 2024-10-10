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
package com.soulfiremc.launcher;

import com.soulfiremc.api.MixinExtension;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.server.util.structs.CustomClassProvider;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.structs.SFLogAppender;
import com.soulfiremc.server.util.SFPathConstants;
import io.netty.util.ResourceLeakDetector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.reflect.Agents;
import net.lenni0451.reflect.Fields;
import org.fusesource.jansi.AnsiConsole;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * This class prepares the earliest work possible, such as loading mixins and setting up logging.
 */
@Slf4j
public abstract class SoulFireAbstractBootstrap {
  public static final Instant START_TIME = Instant.now();

  static {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

    // If Velocity's natives are being extracted to a different temporary directory, make sure the
    // Netty natives are extracted there as well
    if (System.getProperty("velocity.natives-tmpdir") != null) {
      System.setProperty("io.netty.native.workdir", System.getProperty("velocity.natives-tmpdir"));
    }

    // Disable the resource leak detector by default as it reduces performance. Allow the user to
    // override this if desired.
    if (System.getProperty("io.netty.leakDetection.level") == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  protected final Path pluginsDirectory = SFPathConstants.getPluginsDirectory(getBaseDirectory());
  protected final PluginManager pluginManager = new JarPluginManager(pluginsDirectory);

  protected SoulFireAbstractBootstrap() {}

  public static int getRPCPort(int defaultPort) {
    return Integer.getInteger("sf.grpc.port", defaultPort);
  }

  public static int getRandomRPCPort() {
    var port = getRPCPort(0);

    return port == 0 ? PortHelper.getRandomAvailablePort() : port;
  }

  public static String getRPCHost(String defaultHost) {
    return System.getProperty("sf.grpc.host", defaultHost);
  }

  public static void injectExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(
      (thread, throwable) -> {
        log.error("Exception in thread {}", thread.getName());
        //noinspection CallToPrintStackTrace
        throwable.printStackTrace();
      });
  }

  private void initPlugins(List<ClassLoader> classLoaders) {
    try {
      Files.createDirectories(pluginsDirectory);
    } catch (IOException e) {
      log.error("Failed to create plugins directory", e);
    }

    // Prepare the plugin manager
    pluginManager.setSystemVersion(BuildData.VERSION);

    // Load all plugins available
    pluginManager.loadPlugins();
    pluginManager.startPlugins();

    for (var plugin : pluginManager.getPlugins()) {
      classLoaders.add(plugin.getPluginClassLoader());
    }
  }

  @SneakyThrows
  protected void internalBootstrap(String[] args, List<ClassLoader> classLoaders) {
    var forkJoinPoolFactory = new CustomThreadFactory();
    // Ensure the ForkJoinPool uses our custom thread factory
    Fields.set(ForkJoinPool.commonPool(), ForkJoinPool.class.getDeclaredField("factory"), forkJoinPoolFactory);
    Fields.set(null, ForkJoinPool.class.getDeclaredField("defaultForkJoinWorkerThreadFactory"), forkJoinPoolFactory);

    SFLogAppender.INSTANCE.start();

    AnsiConsole.systemInstall();

    sendFlagsInfo();

    injectExceptionHandler();

    initPlugins(classLoaders);

    injectMixinsAndRun(args);
  }

  private void sendFlagsInfo() {
    if (Boolean.getBoolean("sf.flags.v1")) {
      return;
    }

    log.warn("We detected you are not using the recommended flags for SoulFire!");
    log.warn("Please add the following flags to your JVM arguments:");
    log.warn("-XX:+EnableDynamicAgentLoading -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysActAsServerClassMachine -XX:+UseNUMA -XX:+UseFastUnorderedTimeStamps -XX:+UseVectorCmov -XX:+UseCriticalJavaThreadPriority -Dsf.flags.v1=true");
    log.warn("The startup command should look like: 'java -Xmx<ram> <flags> -jar <jarfile>'");
    log.warn("If you already have those flags or want to disable this warning, only add the '-Dsf.flags.v1=true' to your JVM arguments");
  }

  private void injectMixinsAndRun(String[] args) {
    var mixinPaths = new HashSet<String>();
    pluginManager
      .getExtensions(MixinExtension.class)
      .forEach(
        mixinExtension -> {
          for (var mixinPath : mixinExtension.getMixinPaths()) {
            if (mixinPaths.add(mixinPath)) {
              log.info("Added mixin \"{}\"", mixinPath);
            } else {
              log.warn("Mixin path \"{}\" is already added!", mixinPath);
            }
          }
        });

    var classLoaders = new ArrayList<ClassLoader>();
    classLoaders.add(SoulFireAbstractBootstrap.class.getClassLoader());
    pluginManager
      .getPlugins()
      .forEach(pluginWrapper -> classLoaders.add(pluginWrapper.getPluginClassLoader()));

    var classProvider = new CustomClassProvider(classLoaders);
    var transformerManager = new TransformerManager(classProvider);
    transformerManager.addTransformerPreprocessor(new MixinsTranslator());
    mixinPaths.forEach(transformerManager::addTransformer);

    try {
      transformerManager.hookInstrumentation(Agents.getInstrumentation());
      log.info("Used Runtime Agent to inject mixins");

      this.postMixinMain(args);
    } catch (IOException t) {
      log.error("Failed to inject mixins", t);
      System.exit(1);
    }
  }

  protected abstract void postMixinMain(String[] args);

  protected abstract Path getBaseDirectory();

  private static class CustomThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
      var thread = new CustomForkJoinWorkerThread(pool);
      thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
      return thread;
    }
  }

  private static class CustomForkJoinWorkerThread extends ForkJoinWorkerThread {
    protected CustomForkJoinWorkerThread(ForkJoinPool pool) {
      super(pool);
    }
  }
}
