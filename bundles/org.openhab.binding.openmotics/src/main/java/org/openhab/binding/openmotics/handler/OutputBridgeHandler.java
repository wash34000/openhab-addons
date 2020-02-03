package org.openhab.binding.openmotics.handler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OutputBridgeHandler extends OpenMoticsBridgeHandler {
    // private static final Logger logger = LoggerFactory.getLogger(OutputBridgeHandler.class);

    private final BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();

    public OutputBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("OUPUT_BRIDGE => Gestionnaire Output Module créé : " + getThing().getUID().toString());
    }

    protected @Nullable MasterBridgeHandler getBridgeHandler() {
        return this.getBridge() != null ? (MasterBridgeHandler) this.getBridge().getHandler() : null;
    }

    public void updateData(JsonObject obj, String fieldName) {
        try {
            this.queue.put(obj);
        } catch (InterruptedException e) {
            // System.out.println("Problem inserting data into queue");
        }
    }

    @Override
    public JsonObject getFullConfiguration() {
        System.out.println("OUPUT_BRIDGE -> getFullConfiguration");
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            return masterBridgeHandler.sendCommand("/get_output_configurations");
        } else {
            System.out.println("No MasterBridgeHandler for command : /get_output_configurations");
            return null;
        }
    }

    @Override
    protected void getConfiguration() {
    }

    @Override
    protected void readData() {
        while (true) {
            try {
                while (true) {
                    updateChildren(this.queue.take(), "output");
                }
                // break;
            } catch (InterruptedException e) {
                // System.out.println("Read data Output : Problem getting data from queue");
            }
        }
    }

    @SuppressWarnings("null")
    private void updateChildren(JsonObject obj, String fieldName) {
        // System.out.println("OUPUT_BRIDGE -> updateChildren : " + fieldName);
        // System.out.println("OUPUT_BRIDGE -> " + obj);
        if (obj.has(fieldName)) {
            JsonArray configArray = (obj.get(fieldName) != null) ? obj.get(fieldName).getAsJsonArray() : new JsonArray();
            // System.out.println("Update children : configArraySize: " + configArray.size());
            for (JsonElement je : configArray) {
                try {
                    JsonObject q = je.getAsJsonObject();
                    int id = q.get("id").getAsInt();
                    boolean isOn = q.get("status").getAsString().equals("1");
                    int level = q.get("dimmer").getAsInt();
                    for (Thing thing : getThing().getThings()) {
                        Number thingId = (Number) thing.getConfiguration().get("integrationId");
                        if (thingId.intValue() == id && thing.getHandler() != null) {
                            if (thing.getHandler() instanceof OutputHandler) {
                                // System.out.println("OUPUT_BRIDGE -> Update output children ID : " + id + " / Status :
                                // " + isOn + " / Level : " + level);
                                ((OutputHandler) thing.getHandler()).updateData(isOn, level);
                                break;
                            }
                            if (thing.getHandler() instanceof DimmerHandler) {
                                // System.out.println("OUPUT_BRIDGE -> Update dimmer children ID : " + id + " / Status :
                                // " + isOn + " / Level : " + level);
                                ((DimmerHandler) thing.getHandler()).updateData(isOn, level);
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("Error in deserializing received constraints");
                }
            }
        }
    }

    @Override
    protected String getConfigurationCommand() {
        return "/get_output_configuration";
    }

    @Override
    protected String setConfigurationCommand() {
        return "/set_output_configuration";
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof org.eclipse.smarthome.core.types.RefreshType) {
            // System.out.println("Refreshing outputs...");
            getOutputStatus();
        }
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        getOutputStatus();
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        getOutputStatus();
    }

    @SuppressWarnings("null")
    public void setOutput(int id, boolean state) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        final String command = "/set_output?id=" + id + "&is_on=" + state;

        if (masterBridgeHandler != null) {
            // System.out.println(command);
            masterBridgeHandler.sendCommand(command);
        } else {
            // System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }

    @SuppressWarnings("null")
    public void setOutput(int id, boolean state, int level) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        final String command = "/set_output?id=" + id + "&is_on=" + state + "&dimmer=" + level;

        if (masterBridgeHandler != null) {
            // System.out.println(command);
            masterBridgeHandler.sendCommand(command);
        } else {
            // System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }

    @SuppressWarnings("null")
    private void getOutputStatus() {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            JsonObject obj = masterBridgeHandler.sendCommand("/get_output_status");
            updateChildren(obj, "status");
        } else {
            // System.out.println("No MasterBridgeHandler for command : /get_output_status");
        }
    }
}
