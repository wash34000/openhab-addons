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
import org.openhab.binding.openmotics.handler.OpenMoticsBridgeHandler;
import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// Recheche d'OBJET OpenMotics
// Si découverte alors ajout à la LISTE-DECOUVERTE
@Component(service = { DiscoveryService.class }, immediate = true, configurationPid = { "discovery.openmoticsObjects" })
public class OpenmoticsObjectsDiscoveryService extends AbstractDiscoveryService {

    // private final Logger logger = LoggerFactory.getLogger(getClass());
    @SuppressWarnings("unused")
    private static final int SEARCH_TIME = 2;

    private boolean openmoticsBridgeFinded = false;

    private final Map<ThingTypeUID, OpenMoticsBridgeHandler> thingBridgeHandlerMap = new HashMap<>();

    public OpenmoticsObjectsDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 2);
        // System.out.println("DISCOVERY_OBJECT => Initialisation du module de découverte des OBJETS OpenMotics");
    }

    public void register(OpenMoticsBridgeHandler bridgeHandler, ThingTypeUID typeUID) {
        // System.out.println("DISCOVERY_OBJECT -> register : " + bridgeHandler.getThing().getUID() + " / " + typeUID);
        this.thingBridgeHandlerMap.put(typeUID, bridgeHandler);
        openmoticsBridgeFinded = true;
    }

    // Démarrage de la recherche des OBJETS
    @Override
    protected void startScan() {
        // System.out.println("DISCOVERY_OBJECT -> startScan : Découverte des OBJETS Openmotics");
        if (openmoticsBridgeFinded) {
            // Pour chaque type/gestionnaire lancement de la recherche en fonction du type d'OBJET
            for (Map.Entry<ThingTypeUID, OpenMoticsBridgeHandler> entry : this.thingBridgeHandlerMap.entrySet()) {
                OpenMoticsBridgeHandler bridgeHandler = entry.getValue();
                ThingTypeUID typeUID = entry.getKey();

                // System.out.println("DISCOVERY_OBJECT -> startScan : " + bridgeHandler.getThing().getUID() + " / " +
                // typeUID);

                if (typeUID.equals(THING_TYPE_ENERGY)) {
                    discoverEnergyObjects(typeUID, bridgeHandler);
                    continue;
                } else {
                    discoverStandarObjects(typeUID, bridgeHandler);
                }
            }
        } else {
            // System.out.println("DISCOVERY_OBJECT -> Pas de gestionnaire openmoticsBridge installé");
        }
    }

    @Override
    protected synchronized void stopScan() {
        // System.out.println("DISCOVERY_OBJECT -> stopScan");
        super.stopScan();
    }

    @Override
    public void deactivate() {
        // System.out.println("DISCOVERY_OBJECT -> desactivate");
        super.deactivate();
    }

    private void discoverStandarObjects(ThingTypeUID typeUID, OpenMoticsBridgeHandler handler) {
        // System.out.println("DISCOVERY_MODULE -> discover STANDARD OBJECT");
        JsonObject obj = handler.getFullConfiguration();
        // System.out.println("DISCOVERY_MODULE -> discover STANDARD OBJECT" + obj);
        ThingUID bridgeUID = handler.getThing().getUID();

        if (obj != null && obj.has("config")) {
            JsonArray configArray = (obj.get("config") != null) ? obj.get("config").getAsJsonArray() : new JsonArray();

            for (JsonElement je : configArray) {
                try {
                    JsonObject q = je.getAsJsonObject();
                    Integer integrationId = Integer.valueOf(q.get("id").getAsInt());
                    // System.out.println("integrationId in the discovery : " + integrationId + " ");
                    Integer room = Integer.valueOf(q.get("room").getAsInt());
                    // String RoomName = q.get("name").getAsString();
                    String name = q.get("name").getAsString();

                    Map<String, Object> properties = new HashMap<>();

                    // Génération des données
                    properties.put("room", room);
                    properties.put("name", name);

                    if (room != 255) {
                        // Si c'est un DIMMER
                        if (typeUID.getId().equals("dimmer")) {
                            String moduleType = q.get("module_type").getAsString();
                            if (moduleType.contains("D")) {
                                // Ajout à la LISTE-DECOUVERTE
                                String label = "Dimmer : " + name + " (Room " + room + ")";
                                notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);
                            }

                            // Si c'est un OUTPUT
                        } else if (typeUID.getId().equals("output")) {
                            String moduleType = q.get("module_type").getAsString();
                            if (moduleType.contains("O")) {
                                String label = "Output : " + name + " (Room " + room + ")";
                                // Ajout à la LISTE-DECOUVERTE
                                notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);
                            }

                            // Si c'est un INPUT
                        } else if (typeUID.getId().equals("input")) {
                            String label = "Input : " + name + " (Room " + room + ")";
                            // Ajout à la LISTE-DECOUVERTE
                            notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);

                            // Si c'est un THERMOSTAT
                        } else if (typeUID.getId().equals("thermostat")) {
                            String label = "Thermostat : " + name + " (Room " + room + ")";
                            // Ajout à la LISTE-DECOUVERTE
                            notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);

                            // Si c'est un SHUTTER
                        } else if (typeUID.getId().equals("shutter")) {
                            Integer group1 = Integer.valueOf(q.get("group_1").getAsInt());
                            Integer group2 = Integer.valueOf(q.get("group_2").getAsInt());
                            Integer timerUp = Integer.valueOf(q.get("timer_up").getAsInt());
                            Integer timerDown = Integer.valueOf(q.get("timer_down").getAsInt());

                            properties.put("group1", group1);
                            properties.put("group2", group2);

                            properties.put("timerUp", timerUp);
                            properties.put("timerDown", timerDown);

                            String label = "Shutter : " + name + " (Room " + room + ")" + " Groups : " + group1 + "/" + group2 + " Timers : " + timerUp + "/" + timerDown;
                            // Ajout à la LISTE-DECOUVERTE
                            notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);

                            // Si AUTRE
                        } else {
                            String label = typeUID.getId() + " : " + name + " (Room " + room + ")";
                            // Ajout à la LISTE-DECOUVERTE
                            notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);
                        }

                        // System.out.println(name);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    private void discoverEnergyObjects(ThingTypeUID typeUID, OpenMoticsBridgeHandler handler) {
        // System.out.println("DISCOVERY_MODULE -> discover ENERGY OBJECT");
        JsonObject obj = handler.getFullConfiguration();
        ThingUID bridgeUID = handler.getThing().getUID();

        int version = obj.get("version").getAsInt();
        int id = obj.get("id").getAsInt();

        for (int j = 0; j < version; j++) {
            try {
                String jString = String.valueOf(j);
                String inputKey = "input" + jString;
                String sensorKey = "sensor" + jString;
                String invertedKey = "inverted" + jString;
                String input = obj.get(inputKey).getAsString();
                Integer sensor = Integer.valueOf(obj.get(sensorKey).getAsInt());
                Integer inverted = Integer.valueOf(obj.get(invertedKey).getAsInt());

                // System.out.println("DISCOVERY_OBJECT -> Energie objet découvert : " + input + " sensor: " + sensor +
                // " invert: " + inverted);

                Map<String, Object> properties = new HashMap<>();

                // Génération des données
                properties.put("input", input);
                properties.put("sensor", sensor);
                properties.put("inverted", inverted);

                String labelBase = "Energy : ";
                String label = input.isEmpty() ? (labelBase + "Channel " + j + " (Module " + String.valueOf(id) + ")") : labelBase + input;
                Integer integrationId = Integer.valueOf((id - 1) * version + j);

                // Ajout à la LISTE-DECOUVERTE
                notifyDiscovery(typeUID, integrationId, properties, label, bridgeUID);
            } catch (Exception e) {
            }
        }
    }

    // Ajout à la LISTE-DECOUVERTE
    private void notifyDiscovery(ThingTypeUID thingTypeUID, Integer integrationId, Map<String, Object> properties, String label, ThingUID bridgeUID) {

        if (integrationId == null) {
            return;
        }

        // System.out.println("DISCOVERY_OBJECT -> Objet découvert : " + thingTypeUID.toString() + " " +
        // bridgeUID.toString() + " " + integrationId.toString());
        // System.out.println("DISCOVERY_OBJECT -> Objet découvert : " + label);

        // Génération des données
        ThingUID thingUID = new ThingUID(thingTypeUID, integrationId.toString());
        String uniqueID = thingUID.toString();

        properties.put("integrationId", integrationId);
        properties.put("uniqueId", uniqueID);

        // Ajout à la LISTE-DECOUVERTE
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridgeUID).withLabel(label).withProperties(properties).withRepresentationProperty("uniqueId").build();

        thingDiscovered(result);
    }
}
