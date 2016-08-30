import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class JdkHttpServer implements Server {

    private Application application;
    private final HttpServer server;

    public JdkHttpServer(Application application) throws IOException {
        this.application = application;

        server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/css/", new StaticContentHandler());

        JdkHttpServerParamsFilter paramExtractor = new JdkHttpServerParamsFilter();

        server.createContext("/todos", new KnownContextHandler())
                .getFilters().add(paramExtractor);

        server.createContext("/toggleStatus", new KnownContextHandler())
                .getFilters().add(paramExtractor);

        server.createContext("/deleteTodo", new KnownContextHandler())
                .getFilters().add(paramExtractor);

        server.createContext("/clearTodo", new KnownContextHandler())
                .getFilters().add(paramExtractor);

        server.createContext("/", new DefaultHandler())
                .getFilters().add(paramExtractor);

        server.setExecutor(null);
    }

    private class DefaultHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String url = httpExchange.getRequestURI().toString();

            /*
              Redirect "/" to "/todos"
              For other resources, serve 404
             */
            if (url.equals("/")) {
                // TODO: Change url if possible
                new KnownContextHandler().handle(httpExchange);
            } else {
               return404(httpExchange);
            }
        }
    }

    private void return404(HttpExchange httpExchange) throws IOException {
        String response = "Error 404 File not found.";

        httpExchange.getResponseHeaders().set("Content-type", "text/plain");
        httpExchange.sendResponseHeaders(404, response.length());

        OutputStream output = httpExchange.getResponseBody();
        output.write(response.getBytes());
        output.flush();
        output.close();
    }

    private class StaticContentHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                String method = httpExchange.getRequestMethod();
                String url = httpExchange.getRequestURI().toString();

                System.out.println(url + " requested");

                if (method.equalsIgnoreCase("GET") && url.endsWith(".css")) {
                    int lastDotIdx = url.lastIndexOf(".css");
                    int lastSlash = url.lastIndexOf("/");
                    String fileName = url.substring(lastSlash + 1, lastDotIdx + 4);

                    String root = "src/";

                    Path relativePath = Paths.get(root + fileName);
                    if (Files.isReadable(relativePath)) {
                        httpExchange.getResponseHeaders().set("Content-type", "text/css");
                        httpExchange.sendResponseHeaders(200, 0);
                        OutputStream output = httpExchange.getResponseBody();

                        output.write(Files.readAllBytes(relativePath));
                        output.flush();
                        output.close();
                    } else {
                        return404(httpExchange);
                    }
                } else {
                    return404(httpExchange);
                }
            } catch (NullPointerException e) {
                httpExchange.getResponseHeaders().set("Content-type", "text/plain");
                httpExchange.sendResponseHeaders(503, 0);
            }
        }
    }

    private class KnownContextHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String method = httpExchange.getRequestMethod();
            String url = httpExchange.getRequestURI().toString();

            Map<String, String> parameters = (Map<String, String>) httpExchange.getAttribute("parameters");

            Map<String, Collection<Todo>> response = null;
            String stringResponse = "";

            try {
                response = application.handle(method, url, parameters);

                if (method.equalsIgnoreCase("POST")) {
                    httpExchange.getResponseHeaders().set("Location", "/");
                    httpExchange.sendResponseHeaders(303, -1);
                } else {
                    HashMap<String, Object> scopes = new HashMap<String, Object>();

                    Collection<Todo> allTodos = response.get("todos");
                    Collection<Todo> completedTodos = response.get("completed");

                    boolean areTodosEmpty = allTodos.isEmpty();
                    int itemsLeftUndone = allTodos.size() - completedTodos.size();
                    int itemsCompleted = completedTodos.size();
                    boolean pluralTodos = itemsLeftUndone != 1;

                    scopes.put("areTodosEmpty", areTodosEmpty);
                    scopes.put("areAllCompleted", completedTodos.isEmpty());
                    scopes.put("todosLeft", itemsLeftUndone);
                    scopes.put("todosCompleted", itemsCompleted);
                    scopes.put("pluralTodos", pluralTodos);
                    scopes.put("allTodos", allTodos);
                    scopes.put("todos", allTodos);
                    scopes.put("completed", completedTodos);

                    MustacheFactory mf = new DefaultMustacheFactory();

                    Mustache mustache = mf.compile("template.mustache");

                    httpExchange.sendResponseHeaders(200, stringResponse.getBytes().length);

                    OutputStream os = httpExchange.getResponseBody();
                    Writer writer = new OutputStreamWriter(os);
                    mustache.execute(writer, scopes).flush();

                    os.close();
                }
            } catch (Exception e) {
                stringResponse = e.getMessage();
                e.printStackTrace();

                httpExchange.sendResponseHeaders(500, stringResponse.getBytes().length);
            }
        }
    }

    @Override
    public Application getApplication() {
        return this.application;
    }

    @Override
    public void run() {
        server.start();
    }
}
