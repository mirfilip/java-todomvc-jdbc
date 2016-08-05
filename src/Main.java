import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Server server = new JdkHttpServer(new Application());

        server.run();
    }
}
