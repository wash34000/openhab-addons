package org.openhab.binding.openmotics.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

public class DimmerHandler extends OutputHandler {
    // private static final Logger logger = LoggerFactory.getLogger(DimmerHandler.class);

    public DimmerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {

    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // System.out.println("DIMMER -> handleCommand : " + channelUID.getAsString() + " : " + command.toString());
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
            // System.out.println("Dimmer : " + id + " / Command : " + command);
            switch (id) {
                case "switchstatus":
                    if (command.equals(OnOffType.ON)) {
                        bridgeHandler.setOutput(integrationId, true);
                    } else if (command.equals(OnOffType.OFF)) {
                        bridgeHandler.setOutput(integrationId, false);
                    }
                    return;
                case "dimmerlevel":
                    if (command.equals(OnOffType.ON)) {
                        bridgeHandler.setOutput(integrationId, true);
                        // bridgeHandler.setOutput(integrationId, true, 100);

                    } else if (command.equals(OnOffType.OFF)) {
                        // bridgeHandler.setOutput(integrationId, false, 0);
                        bridgeHandler.setOutput(integrationId, false);

                    } else if (command instanceof PercentType) {
                        bridgeHandler.setOutput(integrationId, true, ((PercentType) command).intValue());
                        if (((PercentType) command).intValue() == 0) {
                            bridgeHandler.setOutput(integrationId, false);
                        }
                    }
                    return;
                default:
                    break;
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void updateData(boolean isOn, int level) {
        updateState("switchstatus", isOn ? (State) OnOffType.ON : (State) OnOffType.OFF);
        try {
            if (isOn) {
                updateState("dimmerlevel", new PercentType(level));
            } else {
                updateState("dimmerlevel", new PercentType(0));
            }
        } catch (Exception ex) {
            // System.out.println("Level: " + level);
            // System.out.println("Level big: " + new PercentType(level));
            // System.out.println("uffa: " + ex.getMessage());
        }
    }
}
