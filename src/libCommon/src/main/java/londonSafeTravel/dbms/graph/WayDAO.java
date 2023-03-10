package londonSafeTravel.dbms.graph;

import londonSafeTravel.schema.graph.Point;
import londonSafeTravel.schema.graph.Way;
import org.neo4j.driver.*;

import java.util.Collection;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class WayDAO {

    private final Driver driver;

    public WayDAO(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public WayDAO(Driver driver) {
        this.driver = driver;
    }

    public static void main(String[] argv) {
        WayDAO test = new WayDAO("neo4j://localhost:7687", "neo4j", "pass");
        Point p1 = new Point(2, 101.00, 201.00);
        Point p2 = new Point(1, 100.00, 200.00);
        Way w = new Way();
        w.p1 = p1;
        w.p2 = p2;
        w.name = "Via Diotisalvi";
        test.addWays(List.of(w));
    }

    public void addWays(Collection<Way> ways) {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> createWays(tx, ways));
        }
    }

    private void createWays(TransactionContext tx, Collection<Way> ways) {
        ways.forEach(way -> {
            tx.run(
                    "MERGE (p1: Point {id: $id1})" +
                            "MERGE (p2: Point {id: $id2})" +
                            "MERGE (p1)-[:CONNECTS {" +
                            "   name: $name, class: $class, maxspeed: $speed," +
                            "   crossTimeFoot: $crossFoot, crossTimeBicycle: $crossBicycle, crossTimeMotorVehicle: $crossMotorVehicle" +
                            "}]->(p2)" +
                            "MERGE (p2)-[:CONNECTS {" +
                            "   name: $name, class: $class, maxspeed: $speed," +
                            "   crossTimeFoot: $crossFoot, crossTimeBicycle: $crossBicycle, crossTimeMotorVehicle: $crossMotorVehicle" +
                            "}]->(p1) " +
                            "ON CREATE SET p1.coord = point({longitude: $lon1, latitude: $lat1}) " +
                            "ON CREATE SET p1.lat = $lat1 " +
                            "ON CREATE SET p1.lon = $lon1 " +
                            "ON CREATE SET p2.coord = point({longitude: $lon2, latitude: $lat2})" +
                            "ON CREATE SET p2.lat = $lat2, p2.lon = $lon2",
                    parameters(
                            "id1", way.p1.getId(),
                            "lat1", way.p1.getLocation().getLatitude(),
                            "lon1", way.p1.getLocation().getLongitude(),
                            "id2", way.p2.getId(),
                            "lat2", way.p2.getLocation().getLatitude(),
                            "lon2", way.p2.getLocation().getLongitude(),
                            //"wid", way.id,
                            "name", way.name,
                            "crossFoot", way.crossTimes.get("foot"),
                            "crossBicycle", way.crossTimes.get("bicycle"),
                            "crossMotorVehicle", way.crossTimes.get("motor_vehicle"),
                            "speed", way.maxSpeed,
                            "class", way.roadClass
                    )
            );
        });
    }
}
