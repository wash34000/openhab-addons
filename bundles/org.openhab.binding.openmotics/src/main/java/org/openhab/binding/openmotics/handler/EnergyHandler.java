package org.openhab.binding.openmotics.handler;

import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Power;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public class EnergyHandler extends OpenMoticsHandler {
    // private static final Logger logger = LoggerFactory.getLogger(EnergyHandler.class);

    public EnergyHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // System.out.println("ENERGY -> handleCommand " + channelUID.getAsString() + " " + command.toString());
    }

    public void updateData(float voltage, float frequency, float current, float power) {
        updateState("voltage", new QuantityType<ElectricPotential>(new DecimalType(voltage), SmartHomeUnits.VOLT));
        updateState("frequency", new QuantityType<Frequency>(new DecimalType(frequency), SmartHomeUnits.HERTZ));
        updateState("current", new QuantityType<ElectricCurrent>(new DecimalType(current), SmartHomeUnits.AMPERE));
        updateState("power", new QuantityType<Power>(new DecimalType(power), SmartHomeUnits.WATT));
    }

    @Override
    protected void translateConfiguration(JsonObject q, Configuration config) {
        String input = q.get("input").getAsString();
        config.put("input", input);
        Integer sensor = Integer.valueOf(q.get("sensor").getAsInt());
        config.put("sensor", sensor);
        boolean isInverted = (q.get("inverted").getAsInt() == 1);
        config.put("isInverted", Boolean.valueOf(isInverted));
    }

    @Override
    protected void buildConfigurationJson(Configuration config, JsonObject command) {
        for (String key : config.keySet()) {
            Object o = config.get(key);
            if (key.equals("input")) {
                command.addProperty(key, o.toString());
                continue;
            }
            if (key.equals("sensor")) {
                command.addProperty(key, Integer.valueOf(o.toString()));
                continue;
            }
            if (key.equals("isInverted")) {
                boolean isInverted = Boolean.valueOf(o.toString()).booleanValue();
                Integer value = Integer.valueOf(isInverted ? 1 : 0);
                command.addProperty("inverted", value);
                continue;
            }
            if (o != null) {
                // System.out.println("Energy error: " + o.toString());
            }
        }
    }
}
