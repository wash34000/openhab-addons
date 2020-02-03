package org.openhab.binding.openmotics.handler;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public class ThermostatHandler extends OpenMoticsHandler {
    // private static final Logger logger = LoggerFactory.getLogger(SensorHandler.class);

    public ThermostatHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        System.out.println("THERMOSTAT -> handleCommand " + channelUID.getAsString() + " " + command.toString());
        ThermostatBridgeHandler bridgeHandler = (ThermostatBridgeHandler) getBridge().getHandler();

        if (bridgeHandler == null) {
            System.out.println("ThermostatBridgeHandler is null");
            return;
        }
        if (command instanceof org.eclipse.smarthome.core.types.RefreshType) {
            bridgeHandler.handleCommand(channelUID, command);
            return;
        }
        int integrationId = Integer.parseInt(editProperties().get("integrationId"));
        String id = channelUID.getId();
        try {
            int modeCommand;
            boolean isAuto;
            int mode;
            float newTemperature;

            switch (id) {
                case "mode":
                    modeCommand = ((DecimalType) command).intValue();
                    isAuto = (modeCommand == 8);
                    mode = isAuto ? 0 : modeCommand;
                    bridgeHandler.setMode(integrationId, isAuto, mode);
                    return;

                case "targetTemp":
                    newTemperature = 0.0F;
                    if (command instanceof DecimalType) {
                        newTemperature = ((DecimalType) command).floatValue();
                    } else if (command instanceof QuantityType) {
                        newTemperature = ((QuantityType<?>) command).toUnit(SIUnits.CELSIUS).floatValue();
                    }
                    bridgeHandler.setTargetTemp(integrationId, newTemperature);
                    return;
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public void updateData(int mode, float targetTemp) {
        updateState("mode", new DecimalType(mode));
        updateState("targetTemp", new QuantityType<Temperature>(new DecimalType(targetTemp), SIUnits.CELSIUS));
    }

    public void updatePresetData(JsonObject q) {
        updateState("presetTempAutoDay1", new QuantityType<Temperature>(new DecimalType(q.get("setp0").getAsFloat()), SIUnits.CELSIUS));
        updateState("presetTempAutoNight", new QuantityType<Temperature>(new DecimalType(q.get("setp1").getAsFloat()), SIUnits.CELSIUS));
        updateState("presetTempAutoDay2", new QuantityType<Temperature>(new DecimalType(q.get("setp2").getAsFloat()), SIUnits.CELSIUS));
        updateState("presetTempAway", new QuantityType<Temperature>(new DecimalType(q.get("setp3").getAsFloat()), SIUnits.CELSIUS));
        updateState("presetTempVacation", new QuantityType<Temperature>(new DecimalType(q.get("setp4").getAsFloat()), SIUnits.CELSIUS));
        updateState("presetTempParty", new QuantityType<Temperature>(new DecimalType(q.get("setp5").getAsFloat()), SIUnits.CELSIUS));
    }

    @Override
    protected void translateConfiguration(JsonObject q, Configuration config) {
        Integer sensor = Integer.valueOf(q.get("sensor").getAsInt());
        config.put("sensor", sensor);
        Integer output0 = Integer.valueOf(q.get("output0").getAsInt());
        config.put("output0", output0);
        Integer output1 = Integer.valueOf(q.get("output1").getAsInt());
        config.put("output1", output1);
        Integer pid_p = Integer.valueOf(q.get("pid_p").getAsInt());
        config.put("pid_p", pid_p);
        Integer pid_i = Integer.valueOf(q.get("pid_i").getAsInt());
        config.put("pid_i", pid_i);
        Integer pid_d = Integer.valueOf(q.get("pid_d").getAsInt());
        config.put("pid_d", pid_d);
        Integer pid_int = Integer.valueOf(q.get("pid_int").getAsInt());
        config.put("pid_int", pid_int);
    }

    @Override
    protected void buildConfigurationJson(Configuration config, JsonObject command) {
        for (String key : config.keySet()) {
            Object o = config.get(key);
            if (key.equals("sensor") || key.equals("output0") || key.equals("output1") || key.equals("pid_p") || key.equals("pid_i") || key.equals("pid_d") || key.equals("pid_int")) {
                command.addProperty(key, Integer.valueOf(o.toString()));
                command.addProperty(key, new Boolean(o.toString()));
                continue;
            }
            if (o != null) {
                System.out.println("Thermostat error: " + o.toString());
            }
        }
    }
}
