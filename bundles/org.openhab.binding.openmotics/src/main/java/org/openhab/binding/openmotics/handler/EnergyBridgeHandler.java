package org.openhab.binding.openmotics.handler;

import java.util.Map;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EnergyBridgeHandler extends OpenMoticsBridgeHandler {
    // private static final Logger logger = LoggerFactory.getLogger(EnergyBridgeHandler.class);

    public EnergyBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("ENERGY_BRIDGE => Gestionnaire Energie Module crée : " + getThing().getUID().toString());
    }

    @Override
    public void initialize() {
        super.initialize();
        Map<String, String> properties = editProperties();
        if (properties.get("integrationId") == null) {
            Number id = (Number) getThing().getConfiguration().get("integrationId");
            if (id == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No integrationId");
                return;
            }
            properties.put("integrationId", String.valueOf(id.intValue()));
            getThing().getConfiguration().remove("integrationId");
        }
        properties.put("uniqueId", getThing().getThingTypeUID() + ":" + properties.get("integrationId"));
        updateProperties(properties);
        updateStatus(ThingStatus.ONLINE);
    }

    @SuppressWarnings("null")
    @Override
    public JsonObject getFullConfiguration() {
        MasterBridgeHandler bridgeHandler = (MasterBridgeHandler) getBridge().getHandler();
        JsonObject obj = bridgeHandler.sendCommand("/get_power_modules");
        JsonObject data = null;
        if (obj != null && obj.has("modules")) {
            JsonArray configArray = (obj.get("modules") != null) ? obj.get("modules").getAsJsonArray() : new JsonArray();
            String identity = getThing().getProperties().get("integrationId");
            for (JsonElement je : configArray) {
                JsonObject q = je.getAsJsonObject();
                if (q.get("id").getAsInt() == Integer.parseInt(identity)) {
                    data = q;
                }
            }
        }
        return data;
    }

    @Override
    protected void getConfiguration() {
    }

    @Override
    public JsonObject getConfiguration(String id) {
        @SuppressWarnings("null")
        JsonObject obj = ((MasterBridgeHandler) getBridge().getHandler()).sendCommand("/get_power_modules");
        JsonArray modules = obj.getAsJsonArray("modules");
        JsonObject module = modules.get(0).getAsJsonObject();
        JsonObject currentChannel = new JsonObject();
        currentChannel.addProperty("input", module.get("input" + id).getAsString());
        currentChannel.addProperty("sensor", module.get("sensor" + id).getAsString());
        currentChannel.addProperty("inverted", module.get("inverted" + id).getAsString());
        currentChannel.addProperty("id", id);
        JsonObject config = new JsonObject();
        config.add("config", currentChannel);
        return config;
    }

    @SuppressWarnings("null")
    @Override
    public JsonObject setConfiguration(String data) {
        JsonObject obj = ((MasterBridgeHandler) getBridge().getHandler()).sendCommand("/get_power_modules");
        JsonArray modules = obj.getAsJsonArray("modules");
        JsonObject module = modules.get(0).getAsJsonObject();
        JsonParser parser = new JsonParser();
        JsonElement je = parser.parse(data);
        JsonObject obj2 = je.getAsJsonObject();
        String input = obj2.get("input").getAsString();
        Integer sensor = Integer.valueOf(obj2.get("sensor").getAsInt());
        Integer inverted = Integer.valueOf(obj2.get("inverted").getAsInt());
        String jString = String.valueOf(obj2.get("id").getAsInt());
        module.addProperty("input" + jString, input);
        module.addProperty("sensor" + jString, sensor);
        module.addProperty("inverted" + jString, inverted);
        JsonArray array = new JsonArray();
        array.add(module);
        String message = "/set_power_modules?modules=" + array.toString();
        // System.out.print(message);
        return ((MasterBridgeHandler) getBridge().getHandler()).sendCommand(message);
    }

    @SuppressWarnings("null")
    @Override
    protected void readData() {
        MasterBridgeHandler bridgeHandler = (MasterBridgeHandler) getBridge().getHandler();
        JsonObject obj = bridgeHandler.sendCommand("/get_realtime_power");
        String identity = getThing().getProperties().get("integrationId");
        if (obj.has(identity)) {
            JsonArray configArray = (obj.get(identity) != null) ? obj.get(identity).getAsJsonArray() : new JsonArray();
            for (Thing thing : getThing().getThings()) {
                Number id = (Number) thing.getConfiguration().get("integrationId");
                for (int j = 0; j < configArray.size(); j++) {
                    if (j == id.intValue()) {
                        try {
                            JsonArray q = configArray.get(j).getAsJsonArray();
                            ((EnergyHandler) thing.getHandler()).updateData(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat());
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String getConfigurationCommand() {
        return "";
    }

    @Override
    protected String setConfigurationCommand() {
        return "";
    }
}
