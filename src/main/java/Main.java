import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {

    public static final int PORT = 8888;

    public static void main(String[] args) {

        Server server = new Server();

        server.addHandler("GET", "/spring.svg", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    server.createResponse(request, responseStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        server.addHandler("POST", "/resources.html", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    server.createResponse(request, responseStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        server.acceptClient(PORT);
    }
}