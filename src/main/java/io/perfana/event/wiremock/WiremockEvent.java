/*
 * Copyright (C) 2024 Peter Paul Bakker, Perfana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.perfana.event.wiremock;

import io.perfana.eventscheduler.api.CustomEvent;
import io.perfana.eventscheduler.api.EventAdapter;
import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.config.TestContext;
import io.perfana.eventscheduler.api.message.EventMessageBus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;

public class WiremockEvent extends EventAdapter<WiremockEventContext> {

    public static final String EVENT_WIREMOCK_CHANGE_MAPPINGS = "wiremock-change-mappings";
    public static final String EVENT_WIREMOCK_CHANGE_SETTINGS = "wiremock-change-settings";
    public static final String EVENT_WIREMOCK_CHANGE_IMPORT = "wiremock-change-import";

    private static final Set<String> ALLOWED_CUSTOM_EVENTS =
            setOf(EVENT_WIREMOCK_CHANGE_MAPPINGS, EVENT_WIREMOCK_CHANGE_SETTINGS, EVENT_WIREMOCK_CHANGE_IMPORT);
    public static final String MAPPINGS_URI = "/__admin/mappings";
    public static final String MAPPINGS_IMPORT_URI = "/__admin/mappings/import";
    public static final String ADMIN_SETTINGS_URI = "/__admin/settings";

    private List<WiremockClient> clients;
    private File rootDir;
    
    public WiremockEvent(WiremockEventContext eventConfig, TestContext testContext, EventMessageBus messageBus, EventLogger logger) {
        super(eventConfig, testContext, messageBus, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("before test [" + testContext.getTestRunId() + "]");

        String filesDir = eventContext.getWiremockFilesDir();
        if (filesDir == null) {
            throw new WiremockEventException("wiremock files dir is not set");
        }
        rootDir = new File(filesDir);
        if (!rootDir.exists()) {
            throw new WiremockEventException(String.format("directory not found: %s", rootDir));
        }

        String wiremockUrl = eventContext.getWiremockUrl();
        boolean useProxy = eventContext.isUseProxy();

        if (wiremockUrl == null) {
            throw new WiremockEventException("wiremock url is not set");
        }
        clients = Arrays.stream(wiremockUrl.split(","))
                .map(url -> new WiremockClient(url, logger, useProxy))
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private void importAllWiremockFiles(WiremockClient client, File[] files, Map<String, String> replacements, String uriPath) {
        Arrays.stream(files)
                .peek(file -> logger.info("check " + file))
                .filter(file -> !file.isDirectory())
                .filter(File::canRead)
                .filter(file -> file.getName().endsWith(".json"))
                .peek(file -> logger.info("import " + file))
                .map(this::readContents)
                .filter(Objects::nonNull)
                .forEach(fileContents -> uploadWithTryCatch(client, replacements, uriPath, fileContents));
    }

    private void uploadWithTryCatch(WiremockClient client, Map<String, String> replacements, String uriPath, String fileContents) {
        try {
            client.uploadFileWithReplacements(fileContents, replacements, uriPath);
        } catch (Exception e) {
            if (eventContext.isContinueOnUploadError()) {
                logger.error("Error uploading file: " + e.getMessage());
            } else {
                logger.error("Error uploading file: " + e.getMessage());
                throw e;
            }
        }
    }

    private String readContents(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            logger.error("reading file: " + file);
            return null;
        }
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {

        String eventName = scheduleEvent.getName();
        
        if (EVENT_WIREMOCK_CHANGE_MAPPINGS.equalsIgnoreCase(eventName)) {
            injectDelayFromSettingsIntoFiles(scheduleEvent, MAPPINGS_URI);
        }
        else if (EVENT_WIREMOCK_CHANGE_IMPORT.equalsIgnoreCase(eventName)) {
            injectDelayFromSettingsIntoFiles(scheduleEvent, MAPPINGS_IMPORT_URI);
        }
        else if (EVENT_WIREMOCK_CHANGE_SETTINGS.equalsIgnoreCase(eventName)) {
            injectDelayFromSettingsIntoFiles(scheduleEvent, ADMIN_SETTINGS_URI);
        }
        else {
            logger.debug("ignoring unknown event [" + eventName + "]");
        }
    }

    private void injectDelayFromSettingsIntoFiles(CustomEvent scheduleEvent, String uriPath) {
        Map<String, String> settings = parseSettings(scheduleEvent.getSettings());

        if (settings.containsKey("file") && settings.containsKey("directory")) {
            throw new WiremockEventException("Both file and directory settings are present. Please use only one.");
        }

        if (settings.containsKey("file")) {
            String file = settings.get("file");

            Map<String, String> replacements = settings.entrySet().stream()
                    .filter(e -> !e.getKey().equals("file"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            File jsonFile = null;
            if (file != null) {
                jsonFile = new File(rootDir, file);
                if (!jsonFile.exists()) {
                    logger.error("Wiremock json file does not exist: " + jsonFile);
                    return;
                }
            }

            File[] files = (file != null) ? new File[]{jsonFile} : rootDir.listFiles();

            if (rootDir != null && clients != null) {
                clients.forEach(client -> importAllWiremockFiles(client, files, replacements, uriPath));
            }
        }

        // directory will load all files in the directory after deleting the old ones
        if (settings.containsKey("directory")) {
            String directory = settings.get("directory");
            File dir = new File(rootDir, directory);
            if (!dir.exists()) {
                throw new WiremockEventException("Directory does not exist: " + dir);
            }
            File[] files = dir.listFiles();
            if (files != null && clients != null) {
                clients.forEach((WiremockClient client) -> {
                    // delete all mappings... imports need to be deleted per mapping uuid
                    if (uriPath.equals(MAPPINGS_URI)) { client.deleteAllAtPath(uriPath); }
                    importAllWiremockFiles(client, files, null, uriPath);
                });
            }
        }
    }

    static Map<String, String> parseSettings(String eventSettings) {
        if (eventSettings == null || eventSettings.trim().length() == 0) {
            return Collections.emptyMap();
        }
        return Arrays.stream(eventSettings.split(";"))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(k -> k[0], v -> v.length == 2 ? v[1] : ""));
    }

    @Override
    public Collection<String> allowedCustomEvents() {
        return ALLOWED_CUSTOM_EVENTS;
    }
}