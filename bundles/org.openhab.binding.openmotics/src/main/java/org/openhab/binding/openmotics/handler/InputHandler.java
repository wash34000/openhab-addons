package org.openhab.binding.openmotics.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public class InputHandler extends OpenMoticsHandler {
    // private static final Logger logger = LoggerFactory.getLogger(InputHandler.class);

    public InputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    protected void translateConfiguration(JsonObject q, Configuration config) {
        String basic_actions = q.get("basic_actions").getAsString();
        config.put("basic_actions", basic_actions);
        Integer invert = Integer.valueOf(q.get("invert").getAsInt());
        config.put("invert", invert);
        String can = q.get("can").getAsString();
        config.put("can", can);
        Integer action = Integer.valueOf(q.get("action").getAsInt());
        config.put("action", action);
    }

    @Override
    protected void buildConfigurationJson(Configuration config, JsonObject command) {
        for (String key : config.keySet()) {
            Object o = config.get(key);
            if (key.equals("action")) {
                command.addProperty(key, Integer.valueOf(o.toString()));
                continue;
            }
            if (o != null) {
                System.out.println("Input error: " + o.toString());
            }
        }
    }
}
