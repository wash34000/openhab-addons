package org.openhab.binding.openmotics.handler;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public abstract class OpenMoticsBridgeHandler extends BaseBridgeHandler {
    // private final Logger logger = LoggerFactory.getLogger(getClass());
    private final long REFRESH_INTERVAL = 5;
    private ScheduledFuture<?> refreshDataJob;

    public OpenMoticsBridgeHandler(final Bridge bridge) {
        super(bridge);
        // System.out.println("OPENMOTICS_BRIDGE => Gestionnaire Openmotics créé : " +
        // this.getThing().getUID().toString());
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        // System.out.println("OPENMOTICS_BRIDGE -> Initialisation de : " + this.getThing().getUID().toString());

        if (this.getBridge() != null && this.getBridge().getStatus() == ThingStatus.ONLINE) {
            final Map<String, String> properties = this.editProperties();

            properties.put("vendor", "OpenMotics BVBA");
            properties.put("uniqueId", this.getThing().getThingTypeUID().toString());

            this.updateProperties(properties);

            this.getConfiguration();

            this.refreshDataJob = this.scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    OpenMoticsBridgeHandler.this.readData();
                }
            }, REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.SECONDS);

            this.updateStatus(ThingStatus.ONLINE);
        } else {
            this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    @Override
    public void dispose() {
        this.refreshDataJob.cancel(true);
        super.dispose();
    }

    public JsonObject getFullConfiguration() {
        return null;
    }

    @SuppressWarnings("null")
    public JsonObject getConfiguration(String id) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();

        if (masterBridgeHandler != null) {
            final String message = String.valueOf(this.getConfigurationCommand()) + "?id=" + id;
            return masterBridgeHandler.sendCommand(message);

        } else {
            return null;
        }
    }

    @SuppressWarnings("null")
    public JsonObject setConfiguration(String data) {
        MasterBridgeHandler masterBridgeHandler = (MasterBridgeHandler) this.getBridge().getHandler();

        if (masterBridgeHandler != null) {
            final String message = String.valueOf(this.setConfigurationCommand()) + "?config=" + data;
            return masterBridgeHandler.sendCommand(message);

        } else {
            return null;
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // System.out.println("Received channel: {}, command: {}\", channelUID, command");
    }

    @Override
    public void bridgeStatusChanged(final ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            System.out.println("OPENMOTICS_BRIDGE -> Bridge ONLINE");
            System.out.println("OPENMOTICS_BRIDGE -> Restart refresh data JOB " + this.refreshDataJob);

            if (this.refreshDataJob == null || this.refreshDataJob.isCancelled() || this.refreshDataJob.isDone()) {
                System.out.println("OPENMOTICS_BRIDGE -> Restart JOB");
                this.refreshDataJob = this.scheduler.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        OpenMoticsBridgeHandler.this.readData();
                    }
                }, REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.SECONDS);
            }

            this.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        } else {
            System.out.println("OPENMOTICS_BRIDGE -> Bridge OFFLINE");
            this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            this.refreshDataJob.cancel(false);
        }
    }

    @Override
    public void childHandlerInitialized(final ThingHandler childHandler, final Thing childThing) {
        System.out.println("OPENMOTICS_BRIDGE -> New " + childThing.getLabel() + " added with Id: " + childThing.getProperties().get("integrationId"));
        // getConfiguration().get("integrationId"));
    }

    @Override
    public void childHandlerDisposed(final ThingHandler childHandler, final Thing childThing) {
        System.out.println("OPENMOTICS_BRIDGE -> Deleted " + childThing.getLabel() + " with Id: " + childThing.getProperties().get("integrationId"));
        super.childHandlerDisposed(childHandler, childThing);
    }

    protected abstract void getConfiguration();

    protected abstract void readData();

    protected abstract String getConfigurationCommand();

    protected abstract String setConfigurationCommand();
}
