package SOMANETconnect.systemprocess;

import SOMANETconnect.miscellaneous.Constants;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Used for synchronous process execution. Once the process has finished, its result code, output and error streams are
 * available via getters.
 */
public class SystemProcess {
    class StreamReader extends Thread {
        InputStream is;
        String output;

        // Reads everything from the input stream until it is empty
        StreamReader(InputStream is) {
            this.is = is;
            this.output = "";
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = readLineWithTerm(br)) != null)
                    output += line;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        public String getOutput() {
            return output;
        }
    }

    private static final Logger logger = Logger.getLogger(SystemProcess.class.getName());

    private int result;
    private String output;
    private String error;

    public SystemProcess(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder().command(command);
        processBuilder.environment().putAll(Constants.environmentVariables);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            logger.error("An I/O error occurred during process executing.");
            throw e;
        }

        StreamReader outputReader = new StreamReader(process.getInputStream());
        outputReader.start();
        StreamReader errorReader = new StreamReader(process.getErrorStream());
        errorReader.start();

        try {
            result = process.waitFor();
        } catch (InterruptedException e) {
            // NO-OP
        }

        output = outputReader.getOutput();
        error = errorReader.getOutput();
    }

    public int getResult() {
        return result;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    private static String readLineWithTerm(BufferedReader reader) throws IOException {
        int code;
        StringBuilder line = new StringBuilder();

        while ((code = reader.read()) != -1) {
            char ch = (char) code;

            line.append(ch);

            if (ch == '\n') {
                break;
            } else if (ch == '\r') {
                reader.mark(1);
                ch = (char) reader.read();

                if (ch == '\n') {
                    line.append(ch);
                } else {
                    reader.reset();
                }

                break;
            }
        }

        return (line.length() == 0 ? null : line.toString());
    }
}
