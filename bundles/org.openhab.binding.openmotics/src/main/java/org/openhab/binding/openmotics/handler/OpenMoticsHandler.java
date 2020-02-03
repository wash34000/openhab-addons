/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.openmotics.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;

import com.google.gson.JsonObject;

/**
 * The {@link OpenMoticsHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author JULLIEN Cédric - Initial contribution
 */

@NonNullByDefault
public abstract class OpenMoticsHandler extends BaseThingHandler {
    // private final Logger logger = LoggerFactory.getLogger(getClass());

    public OpenMoticsHandler(Thing bridge) {
        super(bridge);
        // System.out.println("OPENMOTICS_OBJECT => Gestionnaire pour l'objet créé : " +
        // getThing().getUID().toString());
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        OpenMoticsBridgeHandler bridgeHandler = (OpenMoticsBridgeHandler) getBridge().getHandler();

        if (bridgeHandler == null) {
            // System.out.println("bridge is null");

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            return;
        }

        Map<String, String> properties = editProperties();
        Configuration config = editConfiguration();

        properties.put("vendor", "OpenMotics BVBA");

        if (properties.get("integrationId") == "" || properties.get("integrationId") == null) {
            Number integrationId = (Number) getThing().getConfiguration().get("integrationId");
            if (integrationId == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No integrationId");
                return;
            }
            properties.put("integrationId", String.valueOf(integrationId.intValue()));
            getThing().getConfiguration().remove("integrationId");
        }

        // System.out.println("OPENMOTICS_OBJECT -> IntegrationId : " + properties.get("integrationId"));

        if (properties.get("integrationId").contains(".")) {
            String noDotId = properties.get("integrationId").split("\\.")[0];

            properties.remove("integrationId");
            properties.put("integrationId", noDotId);
        }

        properties.put("uniqueId", getThing().getThingTypeUID() + ":" + properties.get("integrationId"));

        JsonObject obj = bridgeHandler.getConfiguration(properties.get("integrationId"));

        if (obj != null) {
            // System.out.println("DEBUG : Json getConfiguration : " + obj);

            if (obj.has("config")) {
                JsonObject configArray = obj.get("config").getAsJsonObject();

                try {
                    String moduleType;

                    if (configArray.has("moduleType")) {
                        moduleType = configArray.get("moduleType").getAsString();
                    } else {
                        moduleType = "T";
                    }

                    properties.put("moduleType", moduleType);

                    translateCommonConfiguration(configArray, config);
                    translateConfiguration(configArray, config);
                } catch (Exception e) {
                }
            }
            updateProperties(properties);
            updateConfiguration(config);
            updateStatus(ThingStatus.ONLINE);
        } else {
            // System.out.println("DEBUG : Cannot get configuration from Bridge.");
            updateStatus(ThingStatus.UNKNOWN);
        }

    }

    @SuppressWarnings({ "unused", "null" })
    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        // System.out.print("changing config...\n");

        Configuration config = editConfiguration();

        for (String key : configurationParameters.keySet()) {
            Object o = configurationParameters.get(key);
            if (o != null) {
                String val = o.toString();
                if (val.contains(".")) {
                    String[] parts = val.split("\\.");
                    String part1 = parts[0];
                    config.put(key, Integer.valueOf(part1));
                    continue;
                }
                config.put(key, configurationParameters.get(key));
                continue;
            } else {
                // config.put(key, configurationParameters.get(key));
            }
        }

        updateConfiguration(config);

        JsonObject command = new JsonObject();

        buildCommonConfigurationJson(config, command);
        buildConfigurationJson(config, command);

        OpenMoticsBridgeHandler bridgeHandler = (OpenMoticsBridgeHandler) getBridge().getHandler();

        bridgeHandler.setConfiguration(command.toString());
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    private void translateCommonConfiguration(JsonObject obj, Configuration config) {
        if (obj.has("name")) {
            String name = obj.get("name").getAsString();
            config.put("name", name);
            obj.remove("name");
        }
        if (obj.has("room")) {
            Integer room = Integer.valueOf(obj.get("room").getAsInt());
            if (room.intValue() == 255) {
                config.put("room", null);
            } else {
                config.put("room", room);
            }
            obj.remove("room");
        }
    }

    private void buildCommonConfigurationJson(Configuration config, JsonObject command) {
        command.addProperty("id", Integer.valueOf(editProperties().get("integrationId")));

        config.remove("integrationId");

        if (config.containsKey("name")) {
            Object o = config.get("name");
            if (o == null) {
                command.addProperty("name", "");
            } else {
                String name = (String) o;
                command.addProperty("name", name);
            }
            config.remove("name");
        }

        if (config.containsKey("room")) {
            Object o = config.get("room");
            if (o == null) {
                command.addProperty("room", Integer.valueOf(255));
            } else {
                Integer room = Integer.valueOf(o.toString());
                command.addProperty("room", room);
            }
            config.remove("room");
        }
    }

    protected abstract void translateConfiguration(JsonObject paramJsonObject, Configuration paramConfiguration);

    protected abstract void buildConfigurationJson(Configuration paramConfiguration, JsonObject paramJsonObject);
}
