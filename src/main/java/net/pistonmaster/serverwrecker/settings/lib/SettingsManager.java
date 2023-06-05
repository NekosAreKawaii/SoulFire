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
package net.pistonmaster.serverwrecker.settings.lib;

import com.google.gson.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private final List<ListenerRegistration<?>> listeners = new ArrayList<>();
    private final List<ProviderRegistration<?>> providers = new ArrayList<>();
    private final Class<? extends SettingsObject>[] registeredSettings;
    private final Gson normalGson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ProtocolVersion.class, new ProtocolVersionAdapter())
            .create();
    private final Gson settingsGson = new GsonBuilder().registerTypeHierarchyAdapter(Object.class, new ObjectAdapter()).create();

    @SafeVarargs
    public SettingsManager(Class<? extends SettingsObject>... registeredSettings) {
        this.registeredSettings = registeredSettings;
    }

    public <T extends SettingsObject> void registerListener(Class<T> clazz, SettingsListener<T> listener) {
        listeners.add(new ListenerRegistration<>(clazz, listener));
    }

    public <T extends SettingsObject> void registerProvider(Class<T> clazz, SettingsProvider<T> provider) {
        providers.add(new ProviderRegistration<>(clazz, provider));
    }

    public <T extends SettingsObject> void registerDuplex(Class<T> clazz, SettingsDuplex<T> duplex) {
        registerListener(clazz, duplex);
        registerProvider(clazz, duplex);
    }

    public SettingsHolder collectSettings() {
        SettingsHolder settingsHolder = new SettingsHolder(providers.stream()
                .map(ProviderRegistration::provider)
                .map(SettingsProvider::collectSettings)
                .toList());

        for (Class<? extends SettingsObject> clazz : registeredSettings) {
            if (!settingsHolder.has(clazz)) {
                throw new IllegalArgumentException("No settings found for " + clazz.getSimpleName());
            }
        }

        return settingsHolder;
    }

    public void onSettingsLoad(SettingsHolder settings) {
        for (SettingsObject setting : settings.settings()) {
            for (ListenerRegistration<?> listener : listeners) {
                loadSetting(setting, listener);
            }
        }
    }

    private <T extends SettingsObject> void loadSetting(SettingsObject setting, ListenerRegistration<T> registration) {
        if (registration.clazz.isInstance(setting)) {
            T castedSetting = registration.clazz.cast(setting);
            registration.listener.onSettingsChange(castedSetting);
        }
    }

    public void loadProfile(Path path) throws IOException {
        try {
            JsonArray settingsHolder = normalGson.fromJson(Files.readString(path), JsonArray.class);
            List<SettingsObject> settingsObjects = new ArrayList<>();
            for (JsonElement jsonElement : settingsHolder) {
                settingsObjects.add(settingsGson.fromJson(jsonElement, SettingsObject.class));
            }

            onSettingsLoad(new SettingsHolder(settingsObjects));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void saveProfile(Path path) throws IOException {
        Files.createDirectories(path.getParent());

        try {
            List<JsonElement> settingsHolder = new ArrayList<>();
            for (SettingsObject settingsObject : collectSettings().settings()) {
                settingsHolder.add(settingsGson.toJsonTree(settingsObject));
            }

            Files.writeString(path, normalGson.toJson(settingsHolder));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private record ListenerRegistration<T extends SettingsObject>(Class<T> clazz, SettingsListener<T> listener) {
    }

    private record ProviderRegistration<T extends SettingsObject>(Class<T> clazz, SettingsProvider<T> provider) {
    }

    private static class ProtocolVersionAdapter implements JsonSerializer<ProtocolVersion>, JsonDeserializer<ProtocolVersion> {
        @Override
        public ProtocolVersion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ProtocolVersion.getClosest(json.getAsString());
        }

        @Override
        public JsonElement serialize(ProtocolVersion src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getName());
        }
    }

    private class ObjectAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {
        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement serialized = normalGson.toJsonTree(src);
            JsonObject jsonObject = serialized.getAsJsonObject();
            jsonObject.addProperty("class", src.getClass().getName());
            return jsonObject;
        }

        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String className = jsonObject.get("class").getAsString();
            try {
                Class<?> clazz = Class.forName(className);
                return normalGson.fromJson(jsonObject, clazz);
            } catch (ClassNotFoundException e) {
                return null; // Some extension might not be loaded, so we just ignore it
            }
        }
    }


}
