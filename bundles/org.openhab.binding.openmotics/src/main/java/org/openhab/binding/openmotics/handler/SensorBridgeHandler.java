package org.openhab.binding.openmotics.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SensorBridgeHandler extends OpenMoticsBridgeHandler {
    // private static final Logger logger = LoggerFactory.getLogger(SensorBridgeHandler.class);

    private int nSensor = 0;

    public SensorBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("SENSOR_BRIDGE => Gestionnaire Sensor Module créé : " + getThing().getUID().toString());
    }

    @SuppressWarnings("null")
    @Override
    public JsonObject getFullConfiguration() {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            return masterBridgeHandler.sendCommand("/get_sensor_configurations");
        } else {
            System.out.println("No MasterBridgeHandler for command : /get_sensor_configurations");
            return null;
        }
    }

    @SuppressWarnings("null")
    public void setSensor(int id, Float temp, Float hum, Integer brig) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        final String command = "/set_virtual_sensor?sensor_id=" + id + "&temperature=" + temp + "&humidity=" + hum + "&brightness=" + brig;

        if (masterBridgeHandler != null) {
            System.out.println(command);
            masterBridgeHandler.sendCommand(command);
        } else {
            System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }

    @SuppressWarnings("null")
    @Override
    protected void getConfiguration() {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            JsonObject obj = masterBridgeHandler.sendCommand("/get_modules");
            int count = 0;
            if (obj.has("inputs")) {
                JsonArray configArray = (obj.get("inputs") != null) ? obj.get("inputs").getAsJsonArray() : new JsonArray();
                if (configArray.size() > 0) {
                    for (JsonElement je : configArray) {
                        try {
                            String s = je.getAsString().toLowerCase();
                            if (s.contains("T".toLowerCase())) {
                                count++;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
            this.nSensor = count * 8;
            System.out.println("nSensor: " + this.nSensor);
            obj = masterBridgeHandler.sendCommand("/get_sensor_configurations");
            if (obj.has("config")) {
                JsonArray configArray = (obj.get("config") != null) ? obj.get("config").getAsJsonArray() : new JsonArray();
                for (JsonElement je : configArray) {
                    try {
                        JsonObject q = je.getAsJsonObject();
                        System.out.println("id: " + q.get("id").getAsString() + " name: " + q.get("name").getAsString());
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            System.out.println("No MasterBridgeHandler for command : get Sensor Configuration");
        }
    }

    @Override
    protected void readData() {
        updateSensors("/get_sensor_temperature_status", "temperature");
        updateSensors("/get_sensor_humidity_status", "humidity");
        updateSensors("/get_sensor_brightness_status", "luminosity");
    }

    @Override
    protected String getConfigurationCommand() {
        return "/get_sensor_configuration";
    }

    @Override
    protected String setConfigurationCommand() {
        return "/set_sensor_configuration";
    }

    @SuppressWarnings("null")
    private void updateSensors(String command, String channelId) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            JsonObject obj = masterBridgeHandler.sendCommand(command);
            if (obj.has("status")) {
                JsonArray statusArray = (obj.get("status") != null) ? obj.get("status").getAsJsonArray() : new JsonArray();
                if (statusArray.size() > 0) {
                    for (Thing thing : getThing().getThings()) {
                        Number id = (Number) thing.getConfiguration().get("integrationId");
                        try {
                            JsonElement e = statusArray.get(id.intValue());
                            if (e != null && !e.isJsonNull()) {
                                ((SensorHandler) thing.getHandler()).updateData(channelId, e.getAsFloat());
                                continue;
                            }
                            ((SensorHandler) thing.getHandler()).removeChannel(channelId);
                        } catch (Exception e) {
                            System.out.println("Error in deserializing command: " + command + " " + channelId);
                        }
                    }
                }
            }
        } else {
            System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }
}
