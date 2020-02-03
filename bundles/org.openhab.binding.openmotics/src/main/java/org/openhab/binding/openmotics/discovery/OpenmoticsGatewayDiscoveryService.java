package org.openhab.binding.openmotics.discovery;

import static org.openhab.binding.openmotics.OpenMoticsBindingConstants.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.openmotics.net.Connector;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Recherche d'une passerelle sur le réseau local
// Si passerelle découverte, alors ajout à la LISTE-DECOUVERTE
@Component(service = { DiscoveryService.class }, immediate = true, configurationPid = { "discovery.openmoticsGateway" })
public class OpenmoticsGatewayDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @SuppressWarnings("unused")
    private static final int SEARCH_TIME = 2;

    public OpenmoticsGatewayDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 2);
        // System.out.println("DISCOVERY_GATEWAY => Initialisation du module de découverte de la PASSERELLE
        // OpenMotics");
    }

    // Lancement de la recherche d'un hôte répondant au critère d'une passerelle sur le réseau local
    @Override
    protected void startScan() {
        // System.out.println("DISCOVERY_GATEWAY -> startScan : Démarrage de la recherche d'une passerelle OpenMotics
        // sur IP");
        ValidateIPV4 validator = new ValidateIPV4();
        try {
            Enumeration<NetworkInterface> enumNetworkInterface = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterface.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterface.nextElement();

                if (!networkInterface.isUp() || networkInterface.isVirtual() || networkInterface.isLoopback()) {
                    continue;
                }

                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if (!validator.isValidIPV4(address.getAddress().getHostAddress())) {
                        continue;
                    }

                    String ipAddress = address.getAddress().getHostAddress();
                    Short prefix = address.getNetworkPrefixLength();
                    // System.out.println("DISCOVERY_GATEWAY -> Scan des IPs du reseau. IP d'OpenHab : " + ipAddress);

                    // Exécution du test de vérification (Login Url) sur l'IP d'OpenHab
                    this.scheduler.execute(new GatewayScan(ipAddress));
                    String subnet = String.valueOf(ipAddress) + "/" + prefix;

                    SubnetUtils utils = new SubnetUtils(subnet);

                    // Exécution du test de vérification (Login Url) sur les IPs du sous-réseau d'OpenHab
                    for (String addressInSubnet : utils.getInfo().getAllAddresses()) {
                        this.scheduler.execute(new GatewayScan(addressInSubnet));
                    }
                }
            }
        } catch (SocketException e) {
            this.logger.warn("Error occurred while searching Openmotics Gateway ({})", e.getMessage());
        }
    }

    @Override
    protected synchronized void stopScan() {
        // System.out.println("DISCOVERY_GATEWAY -> stopScan");
        super.stopScan();
    }

    @Override
    public void deactivate() {
        // System.out.println("DISCOVERY_GATEWAY -> desactivate");
        super.deactivate();
    }

    // Vérification si l'IP est bien celle d'une passerelle OpenMotics (Test de login)
    // Si IP est validé, ajout dans la liste des OBJECT-DECOUVERT
    private class GatewayScan implements Runnable {
        private final String ipAddress;
        static private final int PORT = 443;
        static private final int TIMEOUT = 50;

        public GatewayScan(String ip) {
            this.ipAddress = ip;
        }

        @Override
        public void run() {
            if (!this.pingHost(this.ipAddress, PORT, TIMEOUT)) {
                return;
            }
            try {
                Connector connector = new Connector(this.ipAddress);
                int code = connector.getResponseCode("/login?username=prova&password=prova");
                if (code == 401) {
                    System.out.println("DISCOVERY_GATEWAY -> Passerelle OpenMotics trouvé à l'adresse : " + this.ipAddress);

                    Map<String, Object> properties = new HashMap<>();

                    // Génération des données
                    ThingUID thingUID = new ThingUID(THING_TYPE_MASTERBRIDGE, this.ipAddress.replaceAll("\\.", "_"));
                    String uniqueID = thingUID.toString();
                    String label = "Openmotics Gateway (" + this.ipAddress + ")";

                    properties.put("masterAddress", this.ipAddress);
                    properties.put("uniqueId", uniqueID);

                    // Ajout à la LISTE-DECOUVERTE
                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel(label).withProperties(properties).withRepresentationProperty("uniqueId").build();

                    OpenmoticsGatewayDiscoveryService.this.thingDiscovered(result);
                }
            } catch (Exception e) {
                OpenmoticsGatewayDiscoveryService.this.logger.warn("Discovery resulted in an unexpected exception", e);
            }
        }

        /**
         * Fast pinging of a subnet
         *
         * @see https://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-an-http-url-for-availability
         * @param host Host to ping
         * @param port Port to ping
         * @param timeout Timeout in milliseconds
         * @return Ping result
         */
        private boolean pingHost(String host, int port, int timeout) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeout);
                return true;
            } catch (IOException e) {
                return false; // Either timeout or unreachable or failed DNS lookup.
            }
        }
    }

    class ValidateIPV4 {
        static private final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
        private Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

        public boolean isValidIPV4(final String s) {
            return IPV4_PATTERN.matcher(s).matches();
        }
    }
}
