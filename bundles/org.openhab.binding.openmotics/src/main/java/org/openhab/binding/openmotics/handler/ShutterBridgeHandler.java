package org.openhab.binding.openmotics.handler;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ShutterBridgeHandler extends OpenMoticsBridgeHandler {
    // private static final Logger logger = LoggerFactory.getLogger(ShutterBridgeHandler.class);

    private final BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();

    public ShutterBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("SHUTTER_BRIDGE => Gestionnaire Shutter Module créé : " + getThing().getUID().toString());
    }

    public void updateData(JsonObject obj, String fieldName) {
        try {
            this.queue.put(obj);
        } catch (InterruptedException e) {
            // System.out.println("Problem inserting data into queue");
        }
    }

    @SuppressWarnings("null")
    @Override
    public JsonObject getFullConfiguration() {
        // System.out.println("SHUTTER_BRIDGE -> getFullConfiguration");
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            return masterBridgeHandler.sendCommand("/get_shutter_configurations");
        } else {
            // System.out.println("No MasterBridgeHandler for command : /get_shutter_configurations");
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
                    updateChildren(this.queue.take(), "shutter");
                }
                // break;
            } catch (InterruptedException e) {
                // System.out.println("Read data Shutter : Problem getting data from queue");
            }
        }
    }

    @SuppressWarnings("null")
    private void updateChildren(JsonObject obj, String fieldName) {
        // System.out.println("SHUTTER_BRIDGE -> updateChildren : " + fieldName);
        // System.out.println("SHUTTER_BRIDGE -> " + obj);
        if (obj.has(fieldName) && fieldName.contentEquals("detail")) {
            // System.out.println("SHUTTER_BRIDGE -> updateChildren INIT");
            JsonObject shuttersList = (obj.get(fieldName) != null) ? obj.get(fieldName).getAsJsonObject() : new JsonObject();
            for (Map.Entry<String, JsonElement> shutter : shuttersList.entrySet()) {
                try {
                    JsonObject informations = (JsonObject) shutter.getValue();
                    int shutterId = Integer.parseInt(shutter.getKey());
                    String state = informations.get("state").getAsString();

                    for (Thing thing : getThing().getThings()) {
                        Number thingId = (Number) thing.getConfiguration().get("integrationId");
                        if (thingId.intValue() == shutterId && thing.getHandler() != null) {
                            // System.out.println("SHUTTER_BRIDGE -> Update shutter children ID : " + shutterId + " /
                            // State : " + state);
                            ((ShutterHandler) thing.getHandler()).updateData(state);
                            break;
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("Error in deserializing received constraints");
                }

            }
        } else if (obj.has(fieldName) && fieldName.contentEquals("shutter")) {
            // System.out.println("SHUTTER_BRIDGE -> updateChildren SOCKET");
            JsonArray configArray = (obj.get(fieldName) != null) ? obj.get(fieldName).getAsJsonArray() : new JsonArray();
            for (JsonElement je : configArray) {
                try {
                    JsonObject q = je.getAsJsonObject();
                    int shutterId = q.get("id").getAsInt();
                    String state = q.get("state").getAsString();
                    for (Thing thing : getThing().getThings()) {
                        Number thingId = (Number) thing.getConfiguration().get("integrationId");
                        if (thingId.intValue() == shutterId && thing.getHandler() != null) {
                            // System.out.println("SHUTTER_BRIDGE -> Update shutter children ID : " + shutterId + " /
                            // State : " + state);
                            ((ShutterHandler) thing.getHandler()).updateData(state);
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
        return "/get_shutter_configuration";
    }

    @Override
    protected String setConfigurationCommand() {
        return "/set_shutter_configuration";
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof org.eclipse.smarthome.core.types.RefreshType) {
            // System.out.println("Refreshing shutters...");
            getShutterStatus();
        }
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        getShutterStatus();
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        getShutterStatus();
    }

    @SuppressWarnings("null")
    public void doShutterDown(int id) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        final String command = "/do_shutter_down?id=" + id;

        if (masterBridgeHandler != null) {
            // System.out.println(command);
            masterBridgeHandler.sendCommand(command);
        } else {
            // System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }

    @SuppressWarnings("null")
    public void doShutterUp(int id) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        final String command = "/do_shutter_up?id=" + id;

        if (masterBridgeHandler != null) {
            // System.out.println(command);
            masterBridgeHandler.sendCommand(command);
        } else {
            // System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }

    @SuppressWarnings("null")
    public void doShutterStop(int id) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        final String command = "/do_shutter_stop?id=" + id;

        if (masterBridgeHandler != null) {
            // System.out.println(command);
            masterBridgeHandler.sendCommand(command);
        } else {
            // System.out.println("No MasterBridgeHandler for command : " + command);
        }
    }

    @SuppressWarnings("null")
    private void getShutterStatus() {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            JsonObject obj = masterBridgeHandler.sendCommand("/get_shutter_status");
            updateChildren(obj, "detail");
        } else {
            // System.out.println("No MasterBridgeHandler for command : /get_shutter_status");
        }
    }
}
