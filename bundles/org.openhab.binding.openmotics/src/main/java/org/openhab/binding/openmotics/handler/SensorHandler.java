package org.openhab.binding.openmotics.handler;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public class SensorHandler extends OpenMoticsHandler {
    // private static final Logger logger = LoggerFactory.getLogger(ThermostatHandler.class);

    public SensorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        System.out.println("SENSOR -> handleCommand Channel: " + channelUID.getAsString() + "; channel id: " + channelUID.getId() + ", command: " + command.getClass());
        if (channelUID.getId().equals("temperature")) {
            int integrationId = Integer.parseInt(editProperties().get("integrationId"));
            float newTemperature = -20.0F;
            if (command instanceof DecimalType) {
                newTemperature = ((DecimalType) command).floatValue();
            } else if (command instanceof QuantityType) {
                newTemperature = ((QuantityType<?>) command).toUnit(SIUnits.CELSIUS).floatValue();
            }
            SensorBridgeHandler bridgeHandler = (SensorBridgeHandler) getBridge().getHandler();
            bridgeHandler.setSensor(integrationId, Float.valueOf(newTemperature), null, null);
        }
    }

    public void updateData(String channelId, float value) {
        if (channelIsAbsent(channelId)) {
            addChannel(channelId);
        } else {
            updateState(channelId, value);
        }
    }

    public void removeChannel(String channelId) {
        if (channelIsAbsent(channelId)) {
            return;
        }
        ThingBuilder builder = editThing();
        ChannelUID channelUID = new ChannelUID(getThing().getUID(), channelId);
        builder.withoutChannel(channelUID);
        updateThing(builder.build());
    }

    private void addChannel(String channelId) {
        switch (channelId) {
            case "luminosity":
                addChannel(channelId, "Brightness");
                break;
            case "temperature":
                addChannel(channelId, "Temperature");
                break;
            case "humidity":
                addChannel(channelId, "Humidity");
                break;
        }
    }

    private void updateState(String channelId, float value) {
        if (channelId == "temperature") {
            updateStateCelsius(channelId, value);
        } else {
            updateStateDimensionless(channelId, value);
        }
    }

    private void updateStateCelsius(String channelId, float value) {
        updateState(channelId, new QuantityType<Temperature>(new DecimalType(value), SIUnits.CELSIUS));
    }

    private void updateStateDimensionless(String channelId, float value) {
        updateState(channelId, new QuantityType<Dimensionless>(new DecimalType(value), SmartHomeUnits.PERCENT));
    }

    private boolean channelIsAbsent(String channelId) {
        return (getThing().getChannel(channelId) == null);
    }

    private void addChannel(String channelId, String label) {
        ThingBuilder builder = editThing();
        ChannelUID channelUID = new ChannelUID(getThing().getUID(), channelId);
        Channel channel = ChannelBuilder.create(channelUID, "Number").withLabel(label).build();
        builder.withChannel(channel);
        updateThing(builder.build());
    }

    @Override
    protected void translateConfiguration(JsonObject q, Configuration config) {
        System.out.println("JsonObject: " + q);
        boolean isVirtual = q.get("virtual").getAsBoolean();
        config.put("virtual", Boolean.valueOf(isVirtual));
        Float offset = Float.valueOf(q.get("offset").getAsFloat());
        config.put("offset", offset);
    }

    @Override
    protected void buildConfigurationJson(Configuration config, JsonObject command) {
        for (String key : config.keySet()) {
            Object o = config.get(key);
            if (key.equals("virtual")) {
                command.addProperty(key, new Boolean(o.toString()));
                continue;
            }
            if (key.equals("offset")) {
                command.addProperty(key, Integer.valueOf(o.toString()));
                continue;
            }
            if (o != null) {
                System.out.println("Sensor error: " + o.toString());
            }
        }
    }
}
