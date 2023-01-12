package londonSafeTravel.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import londonSafeTravel.dbms.graph.ManageRouting;
import londonSafeTravel.schema.graph.Point;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RoutingHandler implements HttpHandler {
    ManageRouting manageRouting;
    public RoutingHandler() {
        manageRouting = new ManageRouting("neo4j://localhost:7687", "neo4j", "pass");
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var uriParsed = new URIBuilder(exchange.getRequestURI());

        long start = Long.parseLong(uriParsed.getFirstQueryParam("start").getValue());
        long end = Long.parseLong(uriParsed.getFirstQueryParam("end").getValue());

        // @TODO Handle errors here
        System.out.println(start + "\t" + end);
        var route = manageRouting.route(start, end);
        String json = new Gson().toJson(route);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.length());

        exchange.getResponseBody().write(json.getBytes());
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        exchange.close();
    }
}