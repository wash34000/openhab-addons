package org.openhab.binding.openmotics.internal;

import static org.openhab.binding.openmotics.OpenMoticsBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.openmotics.discovery.OpenmoticsModulesDiscoveryService;
import org.openhab.binding.openmotics.discovery.OpenmoticsObjectsDiscoveryService;
import org.openhab.binding.openmotics.handler.DimmerHandler;
import org.openhab.binding.openmotics.handler.EnergyBridgeHandler;
import org.openhab.binding.openmotics.handler.EnergyHandler;
import org.openhab.binding.openmotics.handler.InputBridgeHandler;
import org.openhab.binding.openmotics.handler.InputHandler;
import org.openhab.binding.openmotics.handler.MasterBridgeHandler;
import org.openhab.binding.openmotics.handler.OpenMoticsBridgeHandler;
import org.openhab.binding.openmotics.handler.OutputBridgeHandler;
import org.openhab.binding.openmotics.handler.OutputHandler;
import org.openhab.binding.openmotics.handler.SensorBridgeHandler;
import org.openhab.binding.openmotics.handler.SensorHandler;
import org.openhab.binding.openmotics.handler.ShutterBridgeHandler;
import org.openhab.binding.openmotics.handler.ShutterHandler;
import org.openhab.binding.openmotics.handler.ThermostatBridgeHandler;
import org.openhab.binding.openmotics.handler.ThermostatHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

@Component(configurationPid = "binding.openmotics", service = ThingHandlerFactory.class)
public class OpenMoticsHandlerFactory extends BaseThingHandlerFactory {
    // private final Logger logger = LoggerFactory.getLogger(BaseThingHandlerFactory.class);

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    static int masterBridgeCount = 0;
    static int outputBridgeCount = 0;
    static int sensorBridgeCount = 0;
    static int thermostatBridgeCount = 0;
    static int inputBridgeCount = 0;
    static int shutterBridgeCount = 0;

    OpenmoticsObjectsDiscoveryService objectsDiscoveryService;
    OpenmoticsModulesDiscoveryService modulesDiscoveryService;

    public void OpenmoticsHandlerFactory() {
        // this.objectsDiscoveryService = new OpenmoticsObjectsDiscoveryService();
        // System.out.println("FAC => Initialisation du Gestionnaire d'objet");
    }

    // Renvoie VRAI si ThingHandlerFactory supporte ce TYPE-OBJET_THING-TYPE et appelle la fonction createHandler
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    // Lance la création des THING en fonction de leur type : MasterBridge ou OutputBridge ...
    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID, ThingUID bridgeUID) {
        // Si le type est MasterBridge
        if (thingTypeUID.equals(THING_TYPE_MASTERBRIDGE)) {
            masterBridgeCount++;
            // System.out.println("FAC -> createThing MasterBridge / Compteur : " + masterBridgeCount);
            if (masterBridgeCount == 1) {
                return super.createThing(thingTypeUID, configuration, thingUID, null);
            }
            // System.out.println("FAC -> createThing : Object already created");
            return null;
        }

        // Si le type est IutputBridge
        if (thingTypeUID.equals(THING_TYPE_INPUTBRIDGE)) {
            inputBridgeCount++;
            // System.out.println("FAC -> createThing InputBridge / Compteur : " + inputBridgeCount);
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }

        // Si le type est OutputBridge
        if (thingTypeUID.equals(THING_TYPE_OUTPUTBRIDGE)) {
            outputBridgeCount++;
            // System.out.print("FAC -> createThing OutputBridge / Count: " + outputBridgeCount);
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }

        // Si le type est OutputBridge
        if (thingTypeUID.equals(THING_TYPE_SHUTTERBRIDGE)) {
            shutterBridgeCount++;
            // System.out.print("FAC -> createThing ShutterBridge / Count: " + shutterBridgeCount);
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }

        return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        ThingTypeUID thingTypeUID = thingHandler.getThing().getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_MASTERBRIDGE)) {
            // System.out.println("FAC -> removeHandler : Reset du compteur");
            masterBridgeCount = 0;
        }
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        // System.out.println("FAC -> Creation des gestionnaires Openmotics ...");

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        // System.out.println("FAC -> Objet en cours (thingUID) : " + thing.getUID().toString());

        // Lancement de création du GESTIONNAIRE du MASTER-BRIDGE
        if (thingTypeUID.equals(THING_TYPE_MASTERBRIDGE)) {
            // System.out.println("FAC -> createHandler : MASTER_BRIDGE : " + thing.getUID().toString());
            MasterBridgeHandler handler = new MasterBridgeHandler((Bridge) thing);

            this.modulesDiscoveryService = new OpenmoticsModulesDiscoveryService();

            this.modulesDiscoveryService.register(handler);
            this.discoveryServiceRegs.put(handler.getThing().getUID(), this.bundleContext.registerService(DiscoveryService.class.getName(), this.modulesDiscoveryService, new Hashtable<>()));
            return handler;
        }

        // // Don't know why constructor is not called
        this.objectsDiscoveryService = new OpenmoticsObjectsDiscoveryService();

        if (thingTypeUID.equals(THING_TYPE_OUTPUTBRIDGE)) {
            // System.out.println("FAC -> Creation du gestionnaire OUTPUT/DIMMER : " + thing.getUID().toString());
            OutputBridgeHandler handler = new OutputBridgeHandler((Bridge) thing);

            // Ajout du type THING_TYPE_OUTPUT et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_OUTPUT, this.objectsDiscoveryService);
            // Ajout du type THING_TYPE_DIMMER et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_DIMMER, this.objectsDiscoveryService);

            return handler;
        }

        if (thingTypeUID.equals(THING_TYPE_ENERGYBRIDGE)) {
            // System.out.println("FAC -> Creation du gestionnaire ENERGY : " + thing.getUID().toString());
            EnergyBridgeHandler handler = new EnergyBridgeHandler((Bridge) thing);

            // Ajout du type THING_TYPE_ENERGY et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_ENERGY, this.objectsDiscoveryService);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_SENSORBRIDGE)) {
            // System.out.println("FAC -> Creation du gestionnaire SENSOR : " + thing.getUID().toString());
            SensorBridgeHandler handler = new SensorBridgeHandler((Bridge) thing);

            // Ajout du type THING_TYPE_SENSOR et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_SENSOR, this.objectsDiscoveryService);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_THERMOSTATBRIDGE)) {
            // System.out.println("FAC -> Creation du gestionnaire THERMOSTAT : " + thing.getUID().toString());
            ThermostatBridgeHandler handler = new ThermostatBridgeHandler((Bridge) thing);

            // Ajout du type THING_TYPE_THERMOSTAT et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_THERMOSTAT, this.objectsDiscoveryService);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_INPUTBRIDGE)) {
            // System.out.println("FAC -> createHandler : INPUT_BRIDGE : " + thing.getUID().toString());
            InputBridgeHandler handler = new InputBridgeHandler((Bridge) thing);

            // Ajout du type THING_TYPE_INPUT et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_INPUT, this.objectsDiscoveryService);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_SHUTTERBRIDGE)) {
            // System.out.println("FAC -> createHandler : SHUTTER_BRIDGE : " + thing.getUID().toString());
            ShutterBridgeHandler handler = new ShutterBridgeHandler((Bridge) thing);

            // Ajout du type THING_TYPE_SHUTTERBRIDGE et du GESTIONNAIRE en vue de démarrer la recherche.
            registerObjectsDiscoveryService(handler, THING_TYPE_SHUTTER, this.objectsDiscoveryService);
            return handler;
        }

        if (thingTypeUID.equals(THING_TYPE_OUTPUT)) {
            // System.out.println("FAC -> Creation de l'objet OUTPUT : " + thing.getUID().toString());
            return new OutputHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_DIMMER)) {
            // System.out.println("FAC -> Creation de l'objet DIMMER : " + thing.getUID().toString());
            return new DimmerHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_ENERGY)) {
            // System.out.println("FAC -> Creation de l'objet ENERGY : " + thing.getUID().toString());
            return new EnergyHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_SENSOR)) {
            // System.out.println("FAC -> Creation de l'objet SENSOR : " + thing.getUID().toString());
            return new SensorHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_THERMOSTAT)) {
            // System.out.println("FAC -> Creation de l'objet THERMOSTAT : " + thing.getUID().toString());
            return new ThermostatHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_INPUT)) {
            // System.out.println("FAC -> Creation de l'objet INPUT : " + thing.getUID().toString());
            return new InputHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_SHUTTER)) {
            // System.out.println("FAC -> Creation de l'objet SHUTTER : " + thing.getUID().toString());
            return new ShutterHandler(thing);
        }
        return null;

    }

    private void registerObjectsDiscoveryService(OpenMoticsBridgeHandler bridgeHandler, ThingTypeUID uid, OpenmoticsObjectsDiscoveryService objectsDiscoveryService) {
        objectsDiscoveryService.register(bridgeHandler, uid);
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), this.bundleContext.registerService(DiscoveryService.class.getName(), objectsDiscoveryService, new Hashtable<>()));
    }
}
