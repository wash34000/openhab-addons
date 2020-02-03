package org.openhab.binding.openmotics.handler;

import static org.openhab.binding.openmotics.OpenMoticsBindingConstants.THING_TYPE_MASTERBRIDGE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.openmotics.net.Connector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@NonNullByDefault
public class MasterBridgeHandler extends BaseBridgeHandler {

    // private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final ScheduledExecutorService SCHEDULER = ThreadPoolManager.getScheduledPool(MasterBridgeHandler.class.getName());

    private @Nullable Connector connector;

    private @Nullable String token;

    private int TOKEN_REFRESH_SECONDS = 10;
    private int TOKEN_REFRESH_SECONDS_BEFORE_EXPIRY = 10 * 60;
    private @Nullable ScheduledFuture<?> tokenRefreshTimer;
    private final Lock tokenRefreshLock = new ReentrantLock();

    private int DATA_REFRESH_SECONDS = 5;
    private @Nullable ScheduledFuture<?> dataRefreshTimer;
    private final Lock dataRefreshLock = new ReentrantLock();

    private boolean socketIsRunning = true;

    private static final String httpResponse = "HTTP/1.1 204 OK\r\n\r\n";
    private @Nullable ServerSocket socket;
    private final int RECEIVING_PORT = 8087;

    public MasterBridgeHandler(Bridge bridge) {
        super(bridge);
        // System.out.println("MASTER_BRIDGE => Gestionnaire Openmotics masterBridgeHandler créé : " +
        // this.getThing().getUID().toString());
    }

    @Override
    public void initialize() {
        // System.out.println("MASTER_BRIDGE -> Initialisation du MasterBridge : " + getThing().getUID().toString());

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Checking configuration...");

        Configuration config = getThing().getConfiguration();
        Map<String, String> properties = editProperties();

        properties.put("vendor", "OpenMotics BVBA");

        // for (String key : properties.keySet()) {
        // System.out.print("Property: " + key + " - " + (properties.get(key)) + "\n");
        // }
        // if (properties.get("uniqueId") == null) {
        if (!properties.containsKey("uniqueId") && properties.get("uniqueId").isEmpty()) {
            String ipAddress = (String) config.get("masterAddress");

            if (ipAddress == null) {
                // System.out.println("Openmotics Gateway: configuration not found.");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No IP Address");
                return;
            }

            ThingUID thingUID = new ThingUID(THING_TYPE_MASTERBRIDGE, ipAddress.replaceAll("\\.", "_"));

            String thingUID2 = thingUID.toString();

            properties.put("uniqueId", thingUID2);
        }

        // for (String key : properties.keySet()) {
        // System.out.print("Property: " + key + " - " + (properties.get(key)) + "\n");
        // }

        updateProperties(properties);

        String masterAddress = (String) config.get("masterAddress");
        String userName = (String) config.get("user");
        String password = (String) config.get("password");

        if (masterAddress.isEmpty() || userName.isEmpty() || password.isEmpty()) {
            // System.out.println("Address, username or passowrd not found: " + getThing().getUID().toString());

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);

            return;
        }

        this.connector = new Connector(masterAddress);

        // Get gateway token by login request
        String token = getGatewayToken();
        if (token == null) {
            // System.out.println("Cannot connect to Gateway: " + getThing().getUID().toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Gateway");
            return;
        }
        MasterBridgeHandler.setToken(MasterBridgeHandler.this, token);

        JsonObject status = sendCommand("/get_status");
        JsonObject version = sendCommand("/get_version");

        if (status != null) {
            properties.put("firmwareVersion", status.get("version").getAsString());
            properties.put("hardwareVersion", status.get("hw_version").getAsString());
            properties.put("gateway software", version.get("gateway").getAsString());
            properties.put("package software version", version.get("version").getAsString());
        }

        this.updateProperties(properties);

        // System.out.println("MASTER_BRIDGE -> initialise : Start schedule Socket and Data");
        try {
            this.socket = new ServerSocket(RECEIVING_PORT);
            refreshSocket();
            refreshGatewayData();

            updateStatus(ThingStatus.ONLINE);

        } catch (IOException ex) {
            // System.out.println("MASTER_BRIDGE -> initialise : Problem opening server socket : " + RECEIVING_PORT);
            // System.out.println("MASTER_BRIDGE -> initialise :" + ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Socket problem");
            return;
        }

    }

    private void refreshSocket() {
        // System.out.println("MASTER_BRIDGE -> refreshSocket : START listening socket");
        Thread waitingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (MasterBridgeHandler.this.socketIsRunning == true) {

                    try {
                        // Wait for client connexion
                        Socket client = MasterBridgeHandler.this.socket.accept();

                        try {
                            // When connexion received start read process
                            // System.out.println("OpenMotics send data ...");
                            final StringBuffer body = new StringBuffer("");
                            MasterBridgeHandler.this.readStream(client, body);

                            if (body.length() == 0) {
                                // System.out.println("body is empty!\n");
                            } else {
                                // System.out.println("body? " + body.toString() + "\n");

                                final JsonParser parser = new JsonParser();
                                final JsonElement je = parser.parse(body.toString());
                                final JsonObject obj = je.getAsJsonObject();

                                for (final Thing thing : MasterBridgeHandler.this.getThing().getThings()) {
                                    // System.out.println("Refresh socket : Find thing in MasterBridge");
                                    final ThingHandler handler = thing.getHandler();

                                    if (handler != null && handler instanceof OutputBridgeHandler) {
                                        // final OutputBridgeHandler hand = (OutputBridgeHandler) handler;
                                        // System.out.println("MASTER_BRIDGE -> Refresh socket : Update Output data");
                                        // System.out.println("MASTER_BRIDGE -> Output Data : " + obj);
                                        ((OutputBridgeHandler) handler).updateData(obj, "output");
                                    }

                                    if (handler != null && handler instanceof ShutterBridgeHandler) {
                                        // final InputBridgeHandler hand2 = (InputBridgeHandler) handler;
                                        // System.out.println("MASTER_BRIDGE -> Refresh socket : Update Shutter data");
                                        // System.out.println("MASTER_BRIDGE -> Shutter Data : " + obj);
                                        ((ShutterBridgeHandler) handler).updateData(obj, "shutter");
                                    }

                                    if (handler != null && handler instanceof InputBridgeHandler) {
                                        // final InputBridgeHandler hand2 = (InputBridgeHandler) handler;
                                        // System.out.println("MASTER_BRIDGE -> Refresh socket : Update Input data");
                                        // System.out.println("MASTER_BRIDGE -> Input Data : " + obj);
                                        ((InputBridgeHandler) handler).updateData(obj, "input");
                                    }
                                }
                            }

                        } catch (IOException e) {
                            // System.out.println("MASTER_BRIDGE -> refreshSocket : EXCEPTION readStream " + e);
                        }

                    } catch (IOException e) {
                        // System.out.println("MASTER_BRIDGE -> refreshSocket : EXCEPTION socket.accept " + e);
                    }
                }

                try {
                    MasterBridgeHandler.this.socket.close();
                    // System.out.println("MASTER_BRIDGE -> refreshSocket : STOP listening socket");
                } catch (IOException e) {
                    MasterBridgeHandler.this.socket = null;
                    // System.out.println("MASTER_BRIDGE -> refreshSocket : STOP listening socket EXCEPTION " + e);
                }
            }
        });

        waitingThread.start();
    }

    private void refreshGatewayData() {
        dataRefreshLock.lock();
        try {
            dataRefreshTimer = null;

            final JsonObject resultStatus = sendCommand("/get_status");

            if (resultStatus != null) {
                final String channelId = "state";
                final int mode = resultStatus.get("mode").getAsInt();
                final String time = resultStatus.get("time").getAsString();

                StringType channelInformation = new StringType("Mode : " + Character.toString((char) mode) + " - Time : " + time);

                this.updateStatus(ThingStatus.ONLINE);
                this.updateState(channelId, channelInformation);

            } else {
                // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Cannot get gateway status");
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot get gateway status");
            }

            // if (this.getThing().getStatus() != ThingStatus.ONLINE) {
            // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Gateway OFFLINE ");
            // final JsonObject obj = sendCommand("/get_status");
            //
            // if (obj != null) {
            // String error = obj.get("error").getAsString();
            //
            // if (error == "token") {
            // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Token expired");
            // refreshToken();
            // } else if (error == "server") {
            // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Server not respond");
            // } else {
            // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Go back to ONLINE");
            // this.updateStatus(ThingStatus.ONLINE);
            // }
            // }
            //
            // } else {
            // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Gateway ONLINE ");
            // final JsonObject obj = sendCommand("/get_status");
            //
            // if (obj != null) {
            // final String channelId = "state";
            // final int mode = obj.get("mode").getAsInt();
            // final String time = obj.get("time").getAsString();
            //
            // StringType channelInformation = new StringType("Mode: " + Character.toString((char) mode) + " - time: " +
            // time);
            // this.updateState(channelId, channelInformation);
            //
            // } else {
            // System.out.println("MASTER_BRIDGE -> refreshGatewayData : Cannot connect to Gateway to get status");
            // this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            // }
            // }

            scheduleGatewayData(DATA_REFRESH_SECONDS);
        } finally {
            dataRefreshLock.unlock();
        }
    }

    private void scheduleGatewayData(long delay) {
        dataRefreshLock.lock();
        try {
            dataRefreshTimer = SCHEDULER.schedule(this::refreshGatewayData, delay, TimeUnit.SECONDS);
        } finally {
            dataRefreshLock.unlock();
        }
    }

    private void refreshToken() {
        tokenRefreshLock.lock();
        try {
            tokenRefreshTimer = null;
            String token = getGatewayToken();

            if (token != null) {
                MasterBridgeHandler.setToken(MasterBridgeHandler.this, token);

                // System.out.println("MASTER_BRIDGE -> refreshToken : " + MasterBridgeHandler.this.token);
                // System.out.println("MASTER_BRIDGE -> refreshToken : Schedule new token refresh in 45 minutes");

                scheduleTokenRefresh(TOKEN_REFRESH_SECONDS_BEFORE_EXPIRY);

                return;

            } else {
                System.out.println("MASTER_BRIDGE -> refreshToken : Cannot login to Gateway. Schedule refreshToken in 5 seconds");
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to get token");
                scheduleTokenRefresh(TOKEN_REFRESH_SECONDS);
            }
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    private void scheduleTokenRefresh(long delay) {
        tokenRefreshLock.lock();
        try {
            tokenRefreshTimer = SCHEDULER.schedule(this::refreshToken, delay, TimeUnit.SECONDS);
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    static void setToken(final MasterBridgeHandler masterBridgeHandler, final String token) {
        masterBridgeHandler.token = token;
    }

    public boolean isInstalled() {
        ThingStatusInfo statusInfo = thing.getStatusInfo();

        if (statusInfo.getStatus() == ThingStatus.ONLINE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void dispose() {
        // System.out.println("MASTER_BRIDGE -> dispose");

        tokenRefreshLock.lock();
        try {
            if (tokenRefreshTimer != null) {
                // System.out.println("MASTER_BRIDGE -> dispose : Cancelling token refresh.");
                tokenRefreshTimer.cancel(true);
                this.token = null;
            }
        } finally {
            tokenRefreshLock.unlock();
        }

        dataRefreshLock.lock();
        try {
            if (dataRefreshTimer != null) {
                // System.out.println("MASTER_BRIDGE -> dispose : Cancelling data refresh.");
                dataRefreshTimer.cancel(true);
            }
        } finally {
            dataRefreshLock.unlock();
        }

        // System.out.println("MASTER_BRIDGE -> dispose : Cancelling socket refresh.");
        this.socketIsRunning = false;
        if (this.socket != null) {
            try {
                this.socket.close();
                // System.out.println("MASTER_BRIDGE -> dispose : Close socket connexion.");

            } catch (IOException e) {
                this.socket = null;
                // System.out.println("Close socket connexion exception : " + e);
            }
        }

        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // System.out.println("MASTER_BRIDGE -> handleCommand (" + (String) this.getConfig().get("masterAddress") + "):
        // " + channelUID.getAsString() + " " + command.toString());

        String id = channelUID.getId();
        try {
            switch (id) {
                case "state": {
                    return;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public @Nullable String getGatewayToken() {
        Configuration config = getThing().getConfiguration();
        String userName = (String) config.get("user");
        String password = (String) config.get("password");
        String loginCommand = "/login?username=" + userName + "&password=" + password;

        JsonObject result = this.connector.postAndGetResponse(loginCommand);

        if (result != null) {
            if (result.has("success") && result.get("success").getAsBoolean()) {
                return result.get("token").getAsString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public @Nullable JsonObject sendCommand(final String message) {
        if (this.token == null) {
            // System.out.println("MASTER_BRIDGE -> sendCommand : Token is null wait for renew");
            refreshToken();

            while (this.token == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // System.out.println("Wait exception " + e);
                }
            }
        }

        String messageWithToken;
        JsonObject result = null;

        if (!message.contains("?")) {
            messageWithToken = String.valueOf(message) + "?token=" + this.token;
        } else {
            messageWithToken = String.valueOf(message) + "&token=" + this.token;
        }

        result = this.connector.postAndGetResponse(messageWithToken);

        if (result.has("success") && result.get("success").getAsBoolean()) {
            result.remove("success");

        } else if (result.has("error")) {
            String resultError = result.get("error").getAsString();

            if (resultError == "token") {
                // System.out.println("MASTER_BRIDGE -> sendCommand : Token expired");
                refreshToken();
                result = null;

            } else if (resultError == "server") {
                // System.out.println("MASTER_BRIDGE -> sendCommand : Server not respond");
                result = null;
            }
        }

        return result;
        // if (this.token == null) {
        // System.out.println("MASTER_BRIDGE -> sendCommand : Token is null wait for renew");
        // refreshToken();
        //
        // while (this.token == null) {
        // try {
        // wait();
        // } catch (InterruptedException e) {
        // System.out.println("Wait exception " + e);
        // }
        // }
        // }
        //
        // String fullMessage;
        // if (!message.contains("?")) {
        // fullMessage = String.valueOf(message) + "?token=" + this.token;
        // } else {
        // fullMessage = String.valueOf(message) + "&token=" + this.token;
        // }
        //
        // if (!this.connector.authenficationOk(fullMessage)) {
        // System.out.println("MASTER_BRIDGE -> sendCommand : Cannot make authentification for : " + message);
        // this.token = null;
        // refreshToken();
        //
        // while (this.token == null) {
        // try {
        // wait();
        // } catch (InterruptedException e) {
        // System.out.println("Wait exception " + e);
        // }
        // }
        // }
        //
        // return this.connector.postAndGetResponse(fullMessage);
    }

    private void readStream(Socket client, StringBuffer body) throws IOException, UnsupportedEncodingException {
        String line;
        int length = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while ((line = in.readLine()) != null) {
            if (line.equals("")) {
                break;
            }
            if (!line.startsWith("Content-Length: ")) {
                continue;
            }
            int index = line.indexOf(58) + 1;
            String len = line.substring(index).trim();
            length = Integer.parseInt(len);
        }

        if (length > 0) {
            int read;
            while ((read = in.read()) != -1) {
                body.append((char) read);
                if (body.length() == length) {
                    break;
                }
            }
        }

        // System.out.println("body: " + body.toString() + "\n");
        client.getOutputStream().write(httpResponse.getBytes("UTF-8"));

        in.close();
    }
}
