package org.openhab.binding.openmotics.handler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.smarthome.core.thing.Bridge;

import com.google.gson.JsonObject;

public class InputBridgeHandler extends OpenMoticsBridgeHandler {
    // private static final Logger logger = LoggerFactory.getLogger(InputBridgeHandler.class);

    private final BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();

    public InputBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("INPUT_BRIDGE => Gestionnaire Input Module créé : " + getThing().getUID().toString());
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
        // System.out.println("INPUT_BRIDGE -> getFullConfiguration");
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();
        if (masterBridgeHandler != null) {
            return masterBridgeHandler.sendCommand("/get_input_configurations");
        } else {
            // System.out.println("No MasterBridgeHandler for command : /get_input_configurations");
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
                    updateChildren(this.queue.take(), "input");
                }
                // break;
            } catch (InterruptedException e) {
                // System.out.println("Read data Input : Problem getting data from queue");
            }
        }
    }

    private void updateChildren(JsonObject obj, String fieldName) {
        // System.out.println("INPUT_BRIDGE -> updateChildren : " + fieldName);
        // System.out.println("INPUT_BRIDGE -> " + obj);
        // if (obj.has(fieldName)) {
        // JsonArray configArray = (obj.get(fieldName) != null) ? obj.get(fieldName).getAsJsonArray() : new JsonArray();
        // for (JsonElement je : configArray) {
        // try {
        // JsonObject q = je.getAsJsonObject();
        // int id = q.get("id").getAsInt();
        // String name = q.get("name").getAsString();
        // System.out.println("INPUT_BRIDGE -> Update input children ID : " + id + " / " + name);
        // } catch (Exception e) {
        // // System.out.println("Error in deserializing received constraints");
        // }
        // }
        // }
    }

    @Override
    protected String getConfigurationCommand() {
        return "/get_input_configuration";
    }

    @Override
    protected String setConfigurationCommand() {
        return "/set_input_configuration";
    }
}
