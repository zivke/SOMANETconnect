package SOMANETconnect;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
                while ((line = br.readLine()) != null)
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

    public SystemProcess(String command) throws IOException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
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
}
