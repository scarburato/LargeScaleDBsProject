package londonSafeTravel.client.gui;

import londonSafeTravel.client.DisruptionsRequest;
import londonSafeTravel.client.QueryPointRequest;
import londonSafeTravel.client.RoutingRequest;
import londonSafeTravel.schema.graph.Disruption;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainApp {
    private static final double MAX_LAT = 51.7314463;
    private static final double MIN_LAT = 51.2268448;
    private static final double MAX_LON = 0.399670;
    private static final double MIN_LON = -0.6125035;

    private JTextField textFieldSearch;
    private JButton buttonSearch;
    private JPanel rootPanel;
    private JXMapViewer mapViewer;
    private JButton buttonRefresh;
    private JCheckBox showDisruptionsCheckBox;

    public static class SwingWaypoint extends DefaultWaypoint {
        private final JButton button;
        private final String text;

        public SwingWaypoint(String text, GeoPosition coord) {
            super(coord);
            this.text = text;
            button = new JButton(text.substring(0, 1));
            button.setSize(24, 24);
            button.setPreferredSize(new Dimension(24, 24));
            button.addMouseListener(new SwingWaypointMouseListener());
            button.setVisible(true);
        }
        JButton getButton() {
            return button;
        }

        private class SwingWaypointMouseListener implements MouseListener {

            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(button, "You clicked on " + text);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        }
    }
    public static class SwingWaypointOverlayPainter extends WaypointPainter<SwingWaypoint> {

        @Override
        protected void doPaint(Graphics2D g, JXMapViewer jxMapViewer, int width, int height) {
            for (SwingWaypoint swingWaypoint : getWaypoints()) {
                Point2D point = jxMapViewer.getTileFactory().geoToPixel(
                        swingWaypoint.getPosition(), jxMapViewer.getZoom());
                Rectangle rectangle = jxMapViewer.getViewportBounds();
                int buttonX = (int)(point.getX() - rectangle.getX());
                int buttonY = (int)(point.getY() - rectangle.getY());
                JButton button = swingWaypoint.getButton();
                button.setLocation(buttonX - button.getWidth() / 2, buttonY - button.getHeight() / 2);
            }
        }
    }
    public static class RoutePainter implements Painter<JXMapViewer> {
        private Color color = Color.RED;
        private boolean antiAlias = true;

        private List<GeoPosition> track;

        /**
         * @param track the track
         */
        public RoutePainter(List<GeoPosition> track) {
            // copy the list so that changes in the
            // original list do not have an effect here
            this.track = new ArrayList<GeoPosition>(track);
        }

        @Override
        public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
            g = (Graphics2D) g.create();

            // convert from viewport to world bitmap
            Rectangle rect = map.getViewportBounds();
            g.translate(-rect.x, -rect.y);

            if (antiAlias)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // do the drawing
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(4));

            drawRoute(g, map);

            // do the drawing again
            g.setColor(color);
            g.setStroke(new BasicStroke(2));

            drawRoute(g, map);

            g.dispose();
        }

        /**
         * @param g   the graphics object
         * @param map the map
         */
        private void drawRoute(Graphics2D g, JXMapViewer map) {
            int lastX = 0;
            int lastY = 0;

            boolean first = true;

            for (GeoPosition gp : track) {
                // convert geo-coordinate to world bitmap pixel
                Point2D pt = map.getTileFactory().geoToPixel(gp, map.getZoom());

                if (first) {
                    first = false;
                } else {
                    g.drawLine(lastX, lastY, (int) pt.getX(), (int) pt.getY());
                }

                lastX = (int) pt.getX();
                lastY = (int) pt.getY();
            }
        }
    }

    public MainApp() {
        // Create a TileFactoryInfo for OpenStreetMap
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        // Use 2 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(2);

        // Set the focus
        GeoPosition london = new GeoPosition(51.5067, -0.1269);

        mapViewer.setZoom(7);
        mapViewer.setAddressLocation(london);

        // Add interactions
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));


        mapViewer.addMouseListener(new MouseListener() {
            londonSafeTravel.schema.graph.Point start = null;
            londonSafeTravel.schema.graph.Point end = null;
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() != MouseEvent.BUTTON3)
                    return;

                System.out.println("CLICK!");

                var coordinates = mapViewer.convertPointToGeoPosition(e.getPoint());
                if(coordinates.getLatitude() > MAX_LAT || coordinates.getLatitude() < MIN_LAT ||
                    coordinates.getLongitude() > MAX_LON || coordinates.getLongitude() < MIN_LON)
                {
                    System.out.println("Skipping click outside of bounds");
                    return;
                }


                QueryPointRequest request = null;
                try {
                    request = new QueryPointRequest(
                            "localhost:8080", coordinates.getLatitude(), coordinates.getLongitude());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

                if(start == null)
                    start = request.getPoint();
                else {
                    end = request.getPoint();
                    System.out.println("Routing begins!");
                    System.out.println(start);
                    System.out.println(end);

                    // Create a track from the geo-positions
                    try {
                        List<GeoPosition> track = new RoutingRequest(
                                "localhost:8080", start.getId(), end.getId()).getRouteGeo();

                        System.out.println("Routing completed " + track.size() + " hops!");

                        // Set the focus
                        mapViewer.zoomToBestFit(new HashSet<GeoPosition>(track), 0.7);

                        RoutePainter routePainter = new RoutePainter(track);
                        mapViewer.setOverlayPainter(routePainter);

                        // Set the focus
                        mapViewer.zoomToBestFit(new HashSet<GeoPosition>(track), 0.7);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    start = null;
                    end = null;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        tileFactory.addTileListener(new TileListener() {

            @Override
            public void tileLoaded(Tile tile) {
                //if (tileFactory.getPendingTiles() == 0) {
                //    System.out.println("All tiles loaded!");
                //}

            }
        });
        mapViewer.setTileFactory(tileFactory);

        showDisruptionsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                    DisruptionsRequest request = null;
                    try {
                        request = new DisruptionsRequest(
                                "localhost:8080");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    ArrayList<Disruption> disruptions = null;
                    try {
                        disruptions = new DisruptionsRequest(
                                "localhost:8080").getDisruptions();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }

                    disruptions.forEach(disruption -> {
                        System.out.println(disruption.id + disruption.centrum);
                    });
                    Set<DisruptionWaypoint> waypoints = disruptions.stream().map(
                            disruption -> new DisruptionWaypoint(
                                    disruption.id,
                                    new GeoPosition(
                                            disruption.centrum.getLatitude(),
                                            disruption.centrum.getLongitude())))
                            .collect(Collectors.toSet());

                    var swingWaypointPainter = new WaypointPainter<DefaultWaypoint>();
                    swingWaypointPainter.setWaypoints(waypoints);

                    mapViewer.setOverlayPainter(swingWaypointPainter);

                    var mouseListener = new MouseAdapter() {
                        private boolean isOnWaypoint(Point point, DisruptionWaypoint waypoint) {
                            var gp_pt = mapViewer.getTileFactory().geoToPixel(
                                    waypoint.getPosition(), mapViewer.getZoom()
                            );

                            //convert to screen
                            Rectangle rect = mapViewer.getViewportBounds();
                            Point converted_gp_pt = new Point(
                                    (int) gp_pt.getX() - rect.x - 5,
                                    (int) gp_pt.getY() - rect.y - 35);

                            // hitbox
                            Rectangle hitbox = new Rectangle(converted_gp_pt, new Dimension(10, 40));

                            return hitbox.contains(point);
                        }
                        @Override
                        public void mouseClicked(MouseEvent me) {
                            for(var waypoint : waypoints) {
                                //check if near the mouse
                                if (isOnWaypoint(me.getPoint(), waypoint))
                                    JOptionPane.showMessageDialog(
                                            mapViewer,
                                            "You clicked on " + waypoint.getText());

                            }
                        }

                        @Override
                        public void mouseMoved(MouseEvent me) {
                            for(var waypoint : waypoints) {
                                if (!isOnWaypoint(me.getPoint(), waypoint))
                                    continue;

                                mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                return;
                            }

                            mapViewer.setCursor(Cursor.getDefaultCursor());
                        }
                    };

                    mapViewer.addMouseListener(mouseListener);
                    mapViewer.addMouseMotionListener(mouseListener);
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainApp");
        frame.setContentPane(new MainApp().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
