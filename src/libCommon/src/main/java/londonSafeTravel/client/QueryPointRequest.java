package londonSafeTravel.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import londonSafeTravel.schema.graph.Point;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class QueryPointRequest {
    public Point getPoint() {
        return point;
    }

    private Point point;

    public QueryPointRequest(String hostname, double latitude, double longitude) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(
                "http://" + hostname + "/query.json?latitude=" + latitude + "&longitude=" + longitude
        ).openConnection();

        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestMethod("GET");
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);

        con.connect();

        int status = con.getResponseCode();
        if(status != 200)
            throw new Exception("errore " + status);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer content = new StringBuffer();

        point = new Gson().fromJson(in, Point.class);
    }

    GeoPosition getPosition(){
        return new GeoPosition(
                point.location.getLatitude(),
                point.location.getLongitude()
        );
    }
}
