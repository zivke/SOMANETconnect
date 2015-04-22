package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MotorTuningWebSocketAdapter extends WebSocketAdapter {
    private static final int MB = 1024 * 1024;
    private static final String MOTOR_CONTROL_ATTR = "motor_control";
    private static final String ID_ATTR = "id";
    private static final String TYPE_ATTR = "type";
    private final static Logger logger = Logger.getLogger(MotorTuningWebSocketAdapter.class.getName());

    private XscopeSocket xscopeSocket;

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        getSession().getPolicy().setMaxTextMessageSize(10 * MB);
        getSession().setIdleTimeout(-1);
        this.xscopeSocket = new XscopeSocket("127.0.0.1", "10101", getRemote());
        logger.info("Socket connected to " + session.getRemoteAddress());
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);

        // Parse request string
        JSONRPC2Request request;

        try {
            request = JSONRPC2Request.parse(message);
        } catch (JSONRPC2ParseException e) {
            logger.error(e.getMessage());
            return;
        }

        try {
            switch (request.getMethod()) {
                case Constants.START_XSCOPE:
                    if (!xscopeSocket.getListen()) {
                        xscopeSocket.setRequestId(request.getID());
                        (new Thread(xscopeSocket)).start();
                    }
                    break;
                case Constants.STOP_XSCOPE:
                    xscopeSocket.close();
                    break;
                case Constants.LIST:
                    ListCommand listCommand = new ListCommand();
                    Util.sendWebSocketResultResponse(getRemote(), listCommand.getDeviceList(), request.getID());
                    break;
                case Constants.SEND:
                    String motorControlParams = (String) request.getNamedParams().get(MOTOR_CONTROL_ATTR);
                    xscopeSocket.sendData(motorControlParams);
                    break;
                case Constants.START_MOTOR:
                    if (!xscopeSocket.getListen()) {
                        xscopeSocket.setRequestId(request.getID());
                        (new Thread(xscopeSocket)).start();
                        startExampleApp();
                    }
                    break;
                case Constants.STOP_MOTOR:
                    if (xscopeSocket.getListen()) {
                        xscopeSocket.close();
                        stopExampleApp();
                    }
                    break;
                case Constants.ERASE_FIRMWARE:
                    String deviceId = (String) request.getNamedParams().get(ID_ATTR);
                    String deviceType = (String) request.getNamedParams().get(TYPE_ATTR);
                    SystemProcess process = eraseFirmware(deviceId, deviceType);
                    if (process.getResult() == 0) {
                        Util.sendWebSocketResultResponse(getRemote(), Constants.ERASE_FIRMWARE, request.getID());
                    } else {
                        logger.error(process.getError());
                        Util.sendWebSocketErrorResponse(getRemote(), JSONRPC2Error.INTERNAL_ERROR, request.getID());
                    }
                    break;
                default:
                    Util.sendWebSocketErrorResponse(getRemote(), JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            Util.sendWebSocketErrorResponse(getRemote(), JSONRPC2Error.INTERNAL_ERROR, request.getID());
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        logger.info("Socket Closed: [" + statusCode + "] " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        logger.error(cause);
    }

    private void startExampleApp() throws IOException {
        List<String> command = new ArrayList<>();
        command.add("./example_app_control.sh");
        command.add("-s");
        ProcessBuilder processBuilder = new ProcessBuilder().command(command).redirectErrorStream(true).inheritIO();
        processBuilder.start();
    }

    private void stopExampleApp() throws IOException {
        List<String> command = new ArrayList<>();
        command.add("./example_app_control.sh");
        command.add("-t");
        ProcessBuilder processBuilder = new ProcessBuilder().command(command).redirectErrorStream(true).inheritIO();
        processBuilder.start();
    }

    private SystemProcess eraseFirmware(String deviceId, String deviceType) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("./erase_firmware.sh");
        command.add("-i");
        command.add(deviceId);
        command.add("-t");
        command.add(deviceType);
        return new SystemProcess(command);
    }
}
