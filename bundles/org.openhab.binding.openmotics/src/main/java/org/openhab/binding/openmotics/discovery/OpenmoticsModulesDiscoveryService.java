package org.openhab.binding.openmotics.discovery;

import static org.openhab.binding.openmotics.OpenMoticsBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.openmotics.handler.MasterBridgeHandler;
import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

// Recherche de module BRIDGE OpenMotics à l'aide de requetes Web sur le masterBridge
//Si découverte alors ajout à la LISTE-DECOUVERTE
@Component(service = { DiscoveryService.class }, immediate = true, configurationPid = { "discovery.openmoticsModules" })
public class OpenmoticsModulesDiscoveryService extends AbstractDiscoveryService {

    // private final Logger logger = LoggerFactory.getLogger(getClass());
    @SuppressWarnings("unused")
    private static final int SEARCH_TIME = 2;

    private MasterBridgeHandler masterBridgeHandler;
    private boolean masterBridgeFinded = false;

    public OpenmoticsModulesDiscoveryService() {
        super(SUPPORTED_BRIDGE_THING_TYPES_UIDS, 2);
        // System.out.println("DISCOVERY_MODULE => Initialisation du module de découverte des MODULES OpenMotics");
    }

    public void register(MasterBridgeHandler masterBridgeHandler) {
        this.masterBridgeHandler = masterBridgeHandler;
        this.masterBridgeFinded = true;
        // System.out.println("DISCOVERY_MODULE -> Enregistrement gestionnaire : masterBridgeHandle");
    }

    // Démarrage de la recherche des MODULES
    @Override
    protected void startScan() {
        // System.out.println("DISCOVERY_MODULE -> startScan : Découverte des MODULES Openmotics");
        if (masterBridgeFinded && this.masterBridgeHandler.isInstalled()) {
            // System.out.println("DISCOVERY_MODULE -> startScan : masterBridge installé, lancement de la recherche de
            // module");

            // Recherche d'un module POWER sur le MasterBridge
            JsonObject obj = this.masterBridgeHandler.sendCommand("/get_power_modules");

            if (obj != null && obj.has("modules")) {
                JsonArray configArray = (obj.get("modules") != null) ? obj.get("modules").getAsJsonArray() : new JsonArray();

                for (JsonElement je : configArray) {
                    JsonObject q = je.getAsJsonObject();
                    String address = q.get("address").getAsString();
                    String name = q.get("name").getAsString();
                    Integer id = Integer.valueOf(q.get("id").getAsInt());
                    Integer version = Integer.valueOf(q.get("version").getAsInt());

                    // System.out.println("DISCOVERY_MODULE -> startScan : Module " + name + " avec pour id #" + id + "
                    // (" + address + ") version: " + version);

                    Map<String, Object> properties = new HashMap<>();

                    // Génération des données
                    ThingUID thingUID = new ThingUID(THING_TYPE_ENERGYBRIDGE, id.toString());
                    ThingUID bridgeUID = this.masterBridgeHandler.getThing().getUID();
                    String uniqueID = thingUID.toString();
                    String label = "OpenMotics Power module : " + name + " (" + address + ")";

                    properties.put("address", address);
                    properties.put("name", name);
                    properties.put("version", version);
                    properties.put("integrationId", id.toString());
                    properties.put("uniqueId", uniqueID);
                    properties.put("vendor", "OpenMotics BVBA");

                    // Ajout à la LISTE-DECOUVERTE
                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridgeUID).withLabel(label).withProperties(properties).withRepresentationProperty("uniqueId").build();

                    thingDiscovered(result);
                }
            }

            // Recherche de module : OUTPUT / INPUT / THERMOSTAT / SENSOR / SHUTTER sur le MasterBridge
            JsonObject modules = this.masterBridgeHandler.sendCommand("/get_modules");

            // System.out.println("DISCOVERY_MODULE -> startScan : Module Input/Output/Sensor :");

            JsonArray outputs = modules.getAsJsonArray("outputs");
            JsonArray intputs = modules.getAsJsonArray("inputs");
            JsonArray shutters = modules.getAsJsonArray("shutters");

            // Génération des module découvert sur le MasterBridge
            boolean hasOutput = !(!outputs.contains(new JsonPrimitive("O")) && !outputs.contains(new JsonPrimitive("D")));
            boolean hasInput = intputs.contains(new JsonPrimitive("I"));
            boolean hasSensor = intputs.contains(new JsonPrimitive("T"));
            boolean hasShutter = shutters.contains(new JsonPrimitive("S"));

            // System.out.println("DISCOVERY_MODULE -> startScan : La passerelle OpenMotics contient les modules
            // suivants : ");
            if (hasOutput) {
                // System.out.println("Module Output");
            }
            if (hasInput) {
                // System.out.println("Module Input");
            }
            if (hasSensor) {
                // System.out.println("Module Sensor");
            }
            if (hasShutter) {
                // System.out.println("Module Shutter");
            }

            // Pour chaque type de module : ajout à LISTE-DECOUVERTE
            for (ThingTypeUID type : SUPPORTED_BRIDGE_THING_TYPES_UIDS) {
                // Si module ENERGYBRIDGE -> Ne rien faire
                if (type.equals(THING_TYPE_ENERGYBRIDGE)) {
                    continue;
                }
                // Si module aucun module OUTPUTBRIDGE -> Ne rien faire
                if (type.equals(THING_TYPE_OUTPUTBRIDGE) && !hasOutput) {
                    continue;
                }
                // Si module aucun module INPUTBRIDGE -> Ne rien faire
                if (type.equals(THING_TYPE_INPUTBRIDGE) && !hasInput) {
                    continue;
                }
                // Si module aucun module SENSORBRIDGE -> Ne rien faire
                if (type.equals(THING_TYPE_SENSORBRIDGE) && !hasSensor) {
                    continue;
                }
                // Si module aucun module SENSORBRIDGE -> Ne rien faire
                if (type.equals(THING_TYPE_SHUTTERBRIDGE) && !hasShutter) {
                    continue;
                }

                // System.out.println("Module OpenMotics ajouté la liste : " + type);

                Map<String, Object> properties = new HashMap<>();

                // Génération des données
                ThingUID thingUID = new ThingUID(type, "controller");
                ThingUID bridgeUID = this.masterBridgeHandler.getThing().getUID();
                String uniqueID = type.toString();
                String label = "";

                if (type.equals(THING_TYPE_OUTPUTBRIDGE)) {
                    label = "OpenMotics Output controller";
                }
                if (type.equals(THING_TYPE_INPUTBRIDGE)) {
                    label = "OpenMotics Input controller";
                }
                if (type.equals(THING_TYPE_SENSORBRIDGE)) {
                    label = "OpenMotics Sensor controller";
                }
                if (type.equals(THING_TYPE_THERMOSTATBRIDGE)) {
                    label = "OpenMotics Thermostat controller";
                }
                if (type.equals(THING_TYPE_SHUTTERBRIDGE)) {
                    label = "OpenMotics Shutter controller";
                }

                properties.put("uniqueId", uniqueID);
                properties.put("vendor", "OpenMotics BVBA");

                // Ajout à la LISTE-DECOUVERTE
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridgeUID).withLabel(label).withProperties(properties).withRepresentationProperty("uniqueId").build();

                thingDiscovered(discoveryResult);
            }
        } else {
            // System.out.println("DISCOVERY_MODULE -> Pas de gestionnaire masterBridge installé");
        }
    }

    @Override
    protected synchronized void stopScan() {
        // System.out.println("DISCOVERY_MODULE -> stopScan");
        super.stopScan();
    }

    @Override
    public void deactivate() {
        // System.out.println("DISCOVERY_MODULE -> desactivate");
        super.deactivate();
    }
}
