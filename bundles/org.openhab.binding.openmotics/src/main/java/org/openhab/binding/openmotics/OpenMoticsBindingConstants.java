/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.openmotics;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link OpenMoticsBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author JULLIEN Cédric - Initial contribution
 */
@NonNullByDefault
public class OpenMoticsBindingConstants {

    private static final String BINDING_ID = "openmotics";

    public static final int CHANNEL_PER_MODULE = 8;

    public static final ThingTypeUID THING_TYPE_MASTERBRIDGE = new ThingTypeUID(BINDING_ID, "masterBridge");

    public static final Set<ThingTypeUID> SUPPORTED_GATEWAY_THING_TYPES_UIDS = Collections.unmodifiableSet((Set<? extends ThingTypeUID>) Stream.<ThingTypeUID> of(THING_TYPE_MASTERBRIDGE).collect(Collectors.toSet()));

    public static final ThingTypeUID THING_TYPE_ENERGYBRIDGE = new ThingTypeUID(BINDING_ID, "energyBridge");
    public static final ThingTypeUID THING_TYPE_OUTPUTBRIDGE = new ThingTypeUID(BINDING_ID, "outputBridge");
    public static final ThingTypeUID THING_TYPE_INPUTBRIDGE = new ThingTypeUID(BINDING_ID, "inputBridge");
    public static final ThingTypeUID THING_TYPE_SENSORBRIDGE = new ThingTypeUID(BINDING_ID, "sensorBridge");
    public static final ThingTypeUID THING_TYPE_THERMOSTATBRIDGE = new ThingTypeUID(BINDING_ID, "thermostatBridge");
    public static final ThingTypeUID THING_TYPE_SHUTTERBRIDGE = new ThingTypeUID(BINDING_ID, "shutterBridge");

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = Collections.unmodifiableSet((Set<? extends ThingTypeUID>) Stream
            .<ThingTypeUID> of(new ThingTypeUID[] { THING_TYPE_ENERGYBRIDGE, THING_TYPE_OUTPUTBRIDGE, THING_TYPE_INPUTBRIDGE, THING_TYPE_SENSORBRIDGE, THING_TYPE_THERMOSTATBRIDGE, THING_TYPE_SHUTTERBRIDGE }).collect(Collectors.toSet()));

    public static final ThingTypeUID THING_TYPE_ENERGY = new ThingTypeUID(BINDING_ID, "energy");
    public static final ThingTypeUID THING_TYPE_OUTPUT = new ThingTypeUID(BINDING_ID, "output");
    public static final ThingTypeUID THING_TYPE_DIMMER = new ThingTypeUID(BINDING_ID, "dimmer");
    public static final ThingTypeUID THING_TYPE_INPUT = new ThingTypeUID(BINDING_ID, "input");
    public static final ThingTypeUID THING_TYPE_SENSOR = new ThingTypeUID(BINDING_ID, "sensor");
    public static final ThingTypeUID THING_TYPE_THERMOSTAT = new ThingTypeUID(BINDING_ID, "thermostat");
    public static final ThingTypeUID THING_TYPE_SHUTTER = new ThingTypeUID(BINDING_ID, "shutter");

    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = Collections.unmodifiableSet((Set<? extends ThingTypeUID>) Stream
            .<ThingTypeUID> of(new ThingTypeUID[] { THING_TYPE_ENERGY, THING_TYPE_OUTPUT, THING_TYPE_DIMMER, THING_TYPE_INPUT, THING_TYPE_SENSOR, THING_TYPE_THERMOSTAT, THING_TYPE_SHUTTER }).collect(Collectors.toSet()));

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet((Set<? extends ThingTypeUID>) Stream.concat(Stream.concat(SUPPORTED_GATEWAY_THING_TYPES_UIDS.stream(), SUPPORTED_DEVICE_THING_TYPES_UIDS.stream()), SUPPORTED_BRIDGE_THING_TYPES_UIDS.stream()).collect(Collectors.toSet()));

    public static final String PROPERTY_VENDOR_NAME = "OpenMotics BVBA";
    public static final String MASTER_ADDRESS = "masterAddress";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String REFRESH_INTERVAL = "refreshInterval";
    public static final String STATE = "state";
    public static final String INTEGRATION_ID = "integrationId";
    public static final String UNIQUE_ID = "uniqueId";
    public static final String CHANNEL_FREQUENCY = "frequency";
    public static final String CHANNEL_VOLTAGE = "voltage";
    public static final String CHANNEL_CURRENT = "current";
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_SWITCH = "switchstatus";
    public static final String CHANNEL_DIMMER = "dimmerlevel";
    public static final String CHANNEL_SHUTTER_STATE = "shutterState";
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_HUMIDITY = "humidity";
    public static final String CHANNEL_LUMINOSITY = "luminosity";
    public static final String CHANNEL_COOLING = "cooling";
    public static final String CHANNEL_MODE = "mode";
    public static final String CHANNEL_TARGET_TEMP = "targetTemp";
    public static final String CHANNEL_PRESET_AUTO_DAY1 = "presetTempAutoDay1";
    public static final String CHANNEL_PRESET_AUTO_NIGHT = "presetTempAutoNight";
    public static final String CHANNEL_PRESET_AUTO_DAY2 = "presetTempAutoDay2";
    public static final String CHANNEL_PRESET_TEMP_AWAY = "presetTempAway";
    public static final String CHANNEL_PRESET_TEMP_VACATION = "presetTempVacation";
    public static final String CHANNEL_PRESET_TEMP_PARTY = "presetTempParty";

}
