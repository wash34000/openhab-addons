package org.openhab.binding.openmotics.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public class ShutterHandler extends OpenMoticsHandler {
    // private static final Logger logger = LoggerFactory.getLogger(ShutterHandler.class);

    public ShutterHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // System.out.println("SHUTTER -> handleCommand " + channelUID.getAsString() + " " + command.toString());
        ShutterBridgeHandler bridgeHandler = (ShutterBridgeHandler) getBridge().getHandler();

        if (bridgeHandler == null) {
            // System.out.println("ShutterBridgeHandler is null");
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
                case "shutterState":
                    if (command.equals(UpDownType.UP)) {
                        bridgeHandler.doShutterUp(integrationId);
                    } else if (command.equals(UpDownType.DOWN)) {
                        bridgeHandler.doShutterDown(integrationId);
                    } else if (command.equals(StopMoveType.STOP)) {
                        bridgeHandler.doShutterStop(integrationId);
                    }
                    return;
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public void updateData(String state) {
        switch (state) {
            case "up": {
                // updateState("shutterState", PercentType.ZERO);
                updateState("shutterState", UpDownType.UP);
                break;
            }
            case "down": {
                // updateState("shutterState", PercentType.HUNDRED);
                updateState("shutterState", UpDownType.DOWN);
                break;
            }
            case "going_up": {
                updateState("shutterState", new PercentType(50));
                // updateState("shutterState", StopMoveType.MOVE);
                break;
            }
            case "going_down": {
                updateState("shutterState", new PercentType(50));
                // updateState("shutterState", StopMoveType.MOVE);
                break;
            }
            case "stopped": {
                updateState("shutterState", new PercentType(50));
                // updateState("shutterState", StopMoveType.STOP);
                break;
            }
            default:
                break;
        }
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
                // System.out.println("Shutter error: " + o.toString());
            }
        }
    }
}
