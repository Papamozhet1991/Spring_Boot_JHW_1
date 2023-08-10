import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Server {

    public final int NUMBER_THREADS = 64;

    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private final ExecutorService threadPool;
    private Socket socket;
    private final ConcurrentHashMap<String, Map<String, Handler>> handlerMap;

    public Server() {
        threadPool = Executors.newFixedThreadPool(NUMBER_THREADS);
        handlerMap = new ConcurrentHashMap<>();
    }

    public void acceptClient(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                socket = serverSocket.accept();
                System.out.println("\n" + socket);
                threadPool.execute(this::workServer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    public void workServer() {

        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            while (true) {
                Request request = createRequest(in, out);
                Handler handler = handlerMap.get(request.getMethod()).get(request.getPath());
                System.out.println("handler: " + handler);

                final var path = request.getPath();
                if (!validPaths.contains(path)) {
                    error404(out);
                    return;
                }

                createResponse(request, out);
                System.out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Request createRequest(BufferedReader in, BufferedOutputStream out) throws IOException {
        var requestLine = "";
        do {
            requestLine = in.readLine();
        } while (requestLine == null);

        System.out.println("\n" + requestLine);
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            out.write(("Не верный запрос").getBytes());
            System.out.println("Не верный запрос");
            socket.close();
        }

        String heading;
        Map<String, String> headers = new HashMap<>();
        while (!(heading = in.readLine()).equals("")) {
            var indexOf = heading.indexOf(":");
            var nameHeader = heading.substring(0, indexOf);
            var valueHeader = heading.substring(indexOf + 2);
            headers.put(nameHeader, valueHeader);
        }
        Request request = new Request(parts[0], parts[1], headers, socket.getInputStream());
        System.out.println("request: " + request);
        out.flush();
        return request;
    }

    public void createResponse(Request request, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", request.getPath());
        final var mimeType = Files.probeContentType(filePath);

        if (request.getPath().equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
            out.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: "
                    + content.length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
            out.write(content);
            out.flush();
            return;
        }

        final var length = Files.size(filePath);
        out.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length
                + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    public void error404(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlerMap.containsKey(method)) {
            handlerMap.get(method).put(path, handler);
        } else {
            handlerMap.put(method, new ConcurrentHashMap<>(Map.of(path, handler)));
        }
        System.out.println(handlerMap);
    }
}