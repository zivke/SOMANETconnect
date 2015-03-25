package SOMANETconnect;

public class SomanetConnect {
    public static void main(String[] args) throws Exception {
        SomanetServer somanetServer = new SomanetServer(62522);
        somanetServer.start();
        System.out.println("SOMANETconnect successfully started on " + somanetServer.getAddress().getHostName() + ":"
                + somanetServer.getPort());
    }
}