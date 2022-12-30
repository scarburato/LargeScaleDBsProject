package londonSafeTravel.dbms.graph;

import londonSafeTravel.schema.graph.Point;
import londonSafeTravel.schema.graph.Way;
import org.neo4j.driver.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class ManageWay {

        private final Driver driver;

        public ManageWay(String uri, String user, String password) {
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        }

        public void addWays(Collection<Way> ways) {
            try(Session session = driver.session()){
                session.writeTransaction(tx -> createWays(tx, ways));
            }
        }
        private Void createWays(Transaction tx, Collection<Way> ways){
            ways.forEach(way -> {
                tx.run(
                        "MERGE (p1: Point {id: $id1})" +
                                "MERGE (p2: Point {id: $id2})" +
                                "MERGE (p1)-[:CONNECTS {name: $name}]->(p2)"+
                                "MERGE (p2)-[:CONNECTS {name: $name}]->(p1)"+
                                "SET p1.coord = point({longitude: $lon1, latitude: $lat1})" +
                                "SET p2.coord = point({longitude: $lon2, latitude: $lat2})", // @TODO Se oneway=yes and foot=no allora non serve l'inverso!
                        parameters(
                                "id1", way.p1.getId(),
                                "lat1", way.p1.getLocation().getLatitude(),
                                "lon1", way.p1.getLocation().getLongitude(),
                                "id2", way.p2.getId(),
                                "lat2", way.p2.getLocation().getLatitude(),
                                "lon2", way.p2.getLocation().getLongitude(),
                                "name", way.name
                        )
                );
            });


            // Perché ci stanno due try sulla session innestate???
//            try( Session session=driver.session() ) {
//                tx.run(
//                        "MATCH (p1:Point),(p2:Point) WHERE p1.id=$id1 AND p2.id=$id2" +
//                                "CREATE (p1)-[r:TO {name: $name, maxSpeed: $maxSpeed}]->(p2) RETURN type(r)",
//                        parameters(
//                                "id1",p1.getId(), "id2", p2.getId(),
//                                "name",name,"maxSpeed", maxSpeed)
//                );
//            }
            return null;
        }

    public static void main(String[] argv){
        ManageWay test= new ManageWay("neo4j://localhost:7687", "neo4j", "pass");
        Point p1=new Point(2,101.00,201.00);
        Point p2=new Point(1,100.00,200.00);
        Way w=new Way();
        w.p1 = p1;
        w.p2 = p2;
        w.name = "Via Diotisalvi";
        test.addWays(List.of(w));
    }
}
