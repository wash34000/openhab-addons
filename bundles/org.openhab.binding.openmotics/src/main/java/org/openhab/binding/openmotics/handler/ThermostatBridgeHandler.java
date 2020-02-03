package org.openhab.binding.openmotics.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ThermostatBridgeHandler extends OpenMoticsBridgeHandler {
    // private static final Logger logger = LoggerFactory.getLogger(SensorBridgeHandler.class);

    public ThermostatBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("THERMOSTAT_BRIDGE => Gestionnaire Thermostat Module créé : " +
        // getThing().getUID().toString());
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String id = channelUID.getId();
        try {
            switch (id) {
                case "cooling":
                    MasterBridgeHandler bridgeHandler = (MasterBridgeHandler) getBridge().getHandler();
                    String isCooling = command.equals(OnOffType.ON) ? "true" : "false";
                    // set_thermostat_mode?thermostat_on=true&automatic=true&setpoint=0&cooling_mode=false&cooling_on=true
                    String command1 = "/set_thermostat_mode?thermostat_on=true&automatic=true&cooling_on=true&cooling_mode=" + isCooling;
                    bridgeHandler.sendCommand(command1);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void setMode(int id, boolean auto, int mode) {
        MasterBridgeHandler bridgeHandler = (MasterBridgeHandler) getBridge().getHandler();
        String command = "/set_per_thermostat_mode?thermostat_id=" + id + "&automatic=" + auto + "&setpoint=" + mode;
        bridgeHandler.sendCommand(command);
    }

    @SuppressWarnings("null")
    public void setTargetTemp(int id, float t) {
        MasterBridgeHandler bridgeHandler = (MasterBridgeHandler) getBridge().getHandler();
        String command = "/set_current_setpoint?thermostat=" + id + "&temperature=" + t;
        bridgeHandler.sendCommand(command);
    }

    @SuppressWarnings("null")
    @Override
    public JsonObject getFullConfiguration() {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            return masterBridgeHandler.sendCommand("/get_sensor_configurations");
        } else {
            System.out.println("No MasterBridgeHandler for command : /get_thermostat_configurations");
            return null;
        }
    }

    @Override
    protected void getConfiguration() {
    }

    @SuppressWarnings("null")
    @Override
    protected void readData() {
        MasterBridgeHandler bridgeHandler = (MasterBridgeHandler) getBridge().getHandler();
        JsonObject obj1 = bridgeHandler.sendCommand("/get_thermostat_status");
        if (obj1.has("cooling")) {
            boolean isCooling = obj1.get("cooling").getAsBoolean();
            updateState("cooling", isCooling ? (State) OnOffType.ON : (State) OnOffType.OFF);
        }
        if (obj1.has("status")) {
            JsonArray statusArray1 = (obj1.get("status") != null) ? obj1.get("status").getAsJsonArray() : new JsonArray();
            if (statusArray1.size() > 0) {
                for (Thing thing : getThing().getThings()) {
                    Number id = (Number) thing.getConfiguration().get("integrationId");
                    try {
                        JsonObject e = (JsonObject) statusArray1.get(id.intValue());
                        ((ThermostatHandler) thing.getHandler()).updateData(e.get("mode").getAsInt(), e.get("csetp").getAsFloat());
                    } catch (Exception e) {
                        System.out.println("Error in deserializing received constraints");
                    }
                }
            }
        }
        JsonObject obj2 = bridgeHandler.sendCommand("/get_thermostat_configurations");
        if (obj2.has("config")) {
            JsonArray statusArray2 = (obj2.get("config") != null) ? obj2.get("config").getAsJsonArray() : new JsonArray();
            if (statusArray2.size() > 0) {
                for (Thing thing : getThing().getThings()) {
                    Number id = (Number) thing.getConfiguration().get("integrationId");
                    try {
                        JsonObject e = (JsonObject) statusArray2.get(id.intValue());
                        ((ThermostatHandler) thing.getHandler()).updatePresetData(e);
                    } catch (Exception e) {
                        System.out.println("Error in deserializing received constraints");
                    }
                }
            }
        }
    }

    @Override
    protected String getConfigurationCommand() {
        return "/get_thermostat_configuration";
    }

    @Override
    protected String setConfigurationCommand() {
        return "/set_thermostat_configuration";
    }
}
