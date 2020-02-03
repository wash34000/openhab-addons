package org.openhab.binding.openmotics.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

import com.google.gson.JsonObject;

public class OutputHandler extends OpenMoticsHandler {
    // private static final Logger logger = LoggerFactory.getLogger(OutputHandler.class);

    public OutputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // System.out.println("OUTPUT -> handleCommand " + channelUID.getAsString() + " " + command.toString());
        OutputBridgeHandler bridgeHandler = (OutputBridgeHandler) getBridge().getHandler();

        if (bridgeHandler == null) {
            // System.out.println("OutputBridgeHandler is null");
            return;
        }
        if (command instanceof org.eclipse.smarthome.core.types.RefreshType) {
            bridgeHandler.handleCommand(channelUID, command);
            return;
        }

        Number thingId = (Number) getThing().getConfiguration().get("integrationId");
        int integrationId = thingId.intValue();
        String id = channelUID.getId();
        try {
            switch (id) {
                case "switchstatus":
                    if (command.equals(OnOffType.ON)) {
                        bridgeHandler.setOutput(integrationId, true);
                    } else if (command.equals(OnOffType.OFF)) {
                        bridgeHandler.setOutput(integrationId, false);
                    }
                    return;
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public void updateData(boolean isOn, int level) {
        updateState("switchstatus", isOn ? (State) OnOffType.ON : (State) OnOffType.OFF);
    }

    @Override
    protected void translateConfiguration(JsonObject q, Configuration config) {
        Integer floor = Integer.valueOf(q.get("floor").getAsInt());
        if (floor.intValue() == 255) {
            config.put("floor", null);
        } else {
            config.put("floor", floor);
        }
        Integer timer = Integer.valueOf(q.get("timer").getAsInt());
        config.put("timer", timer);
        boolean isLight = (q.get("type").getAsInt() == 255);
        config.put("isLight", Boolean.valueOf(isLight));
    }

    @Override
    protected void buildConfigurationJson(Configuration config, JsonObject command) {
        for (String key : config.keySet()) {
            Object o = config.get(key);
            if (key.equals("timer")) {
                command.addProperty(key, Integer.valueOf(o.toString()));
                continue;
            }
            if (key.equals("isLight")) {
                boolean isLight = Boolean.valueOf(o.toString()).booleanValue();
                Integer value = Integer.valueOf(isLight ? 255 : 0);
                command.addProperty("type", value);
                continue;
            }
            if (key.equals("floor")) {
                if (o == null) {
                    command.addProperty("floor", Integer.valueOf(255));
                    continue;
                }
                Integer floor = Integer.valueOf(o.toString());
                command.addProperty("floor", floor);
                continue;
            }
            if (o != null) {
                // System.out.println("Output error: " + o.toString());
            }
        }
    }
}
