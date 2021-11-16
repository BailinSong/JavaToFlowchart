package ooo.reindeer.tools.java.flowchart;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executors;

/**
 * @ClassName HttpServer
 * @Author songbailin
 * @Date 2021/3/16 13:38
 * @Version 1.0
 * @Description TODO
 */
public class FlowChartServer {

    static String staticPath = "/static";

    public static void main(String[] args) throws IOException {

        int port=28080;

        if (args.length>0) {
            port = Integer.parseInt(args[0]);
        }

        InetSocketAddress addr = new InetSocketAddress(port);
        HttpServer server = HttpServer.create(addr, 0);

        server.createContext("/", exchange -> {
            System.out.println("HttpPath = " + exchange.getRequestURI().getPath());
            if (exchange.getRequestURI().getPath().isEmpty() || exchange.getRequestURI().getPath().equals("/")) {
                sendData(staticPath + "/index.html", exchange);
            } else if (exchange.getRequestURI().getPath().equals("/api/parse/part/")) {
                if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {

                    exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=utf-8");

                    String postString = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);

                    TreeNode tree = TreeBuilder.parseString(postString);
                    FlowchartBuilder flowchartBuilder = new DotBuilder();
                    String r = flowchartBuilder.builder(tree);

                    System.out.println("code = " + postString + "\nr = [" + r + "]");

                    exchange.sendResponseHeaders(200, r.getBytes(StandardCharsets.UTF_8).length);
                    exchange.getResponseBody().write(r.getBytes(StandardCharsets.UTF_8));
                    exchange.getResponseBody().flush();
                } else {
                    exchange.sendResponseHeaders(204, 0);
                }
            } else {
                sendData(staticPath + exchange.getRequestURI().getPath(), exchange);

            }
            exchange.close();

        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server is listening on port "+port);
    }

    private static void sendData(String path, HttpExchange exchange) throws IOException {

        System.out.println("path = " + path);
        try (InputStream in = FlowChartServer.class.getResourceAsStream(path)) {
            if (in == null) {
                System.out.println("in = " + in);
                exchange.sendResponseHeaders(404, 0);
            } else {
                System.out.println("in = " + in);
                if(path.endsWith("html")){
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                }else {
                    exchange.getResponseHeaders().set("Content-Type", "*/*");
                }
                exchange.sendResponseHeaders(200, 0);
                byte[] bytes = new byte[1024];
                int size = -1;
                while ((size = in.read(bytes)) != -1) {
//                    System.out.println("Arrays.toString(bytes = " + Arrays.toString(bytes));
                    exchange.getResponseBody().write(bytes, 0, size);
                }
                exchange.getResponseBody().flush();

            }
        }
    }

}
