//GMapsFX
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.*;
import com.lynden.gmapsfx.service.directions.*;

//GeoIP
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

//JavaFX
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

//JSON
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//Connecting
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import netscape.javascript.JSObject;

/* Uses apache commons version: 2.4 */
import org.apache.commons.io.IOUtils;

//ImageHandling
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Map implements MapComponentInitializedListener, DirectionsServiceCallback {

    public static URL imageUrl;
    public static Image mapImage;
    public static ImageView image2;

    public static int zoom;
    public static int scale;
    public static Scene scene;
    public static Location ipLocation;
    public static JSONArray garageArray;
    public static String origin;
    public static String destination;
    public static VBox mapViewVBox;
    static GoogleMapView mapView;
    GoogleMap map;
    protected DirectionsPane directions;
    DirectionsRenderer renderer;

    //Gets the Interactive or Static Google Map, applies all of the necessary elements (Markers, waypoints, routes etc.), possibly from external sources (internet)
    public static void map() {
        //default
        BufferedImage bufferedImage = null;
        ipLocation = geoIPLookUp();
        try {
            bufferedImage = ImageIO.read(new File("maps-icon.png")); //default fallback
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Parser for the JSON from a URL
        if(ipLocation != null) { origin = ipLocation.latitude + "," + ipLocation.longitude; }
        else { origin = "Rotterdam"; }
        destination = "Middelharnis";
        String mapUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=AIzaSyCPwbsx2oWVLVbZJnlbEgEFtj0mQy4ES1c";
        String garageUrl = "http://opendata.technolution.nl/opendata/parkingdata/v1";

        //Process for generating the ImageMapView
        if (zoom == 0) {
            zoom = 11;
        }
        if (scene != null) {
            Main.width = (int) scene.getWidth();
            Main.height = (int) scene.getHeight();
        } else {
            Main.width = 1000;
            Main.height = 800;
        }
        scale = 2;
        String maptype = "roadmap";

        try {
            //Gets all of the garages and sets them as markers
            String markers = "";
            String parsedGarage = IOUtils.toString((new URL(garageUrl)));
            JSONObject parsedGarageObject = (JSONObject) JSONValue.parseWithException(parsedGarage);
            garageArray = (JSONArray) parsedGarageObject.get("parkingFacilities");
            for(int i = 0; i < garageArray.size(); i++){
                JSONObject garage = (JSONObject) garageArray.get(i);
                String name = (String) garage.get("name");
                if(garage.get("locationForDisplay") != null) {
                    JSONObject displayLocation = (JSONObject) garage.get("locationForDisplay");
                    Double latitude = (Double) displayLocation.get("latitude");
                    Double longitude = (Double) displayLocation.get("longitude");
                    if (displayLocation != null) {
                        latitude = (Double) displayLocation.get("latitude");
                        longitude = (Double) displayLocation.get("longitude");
                    }
                    if (latitude > 51.8 && latitude < 52 && longitude > 4 && longitude < 4.75) {
                        String location = latitude + "," + longitude + "|";
                        markers += location;
                    }
                }
            }

            //Gets all of the waypoints in the route to the destination
            String route = "|" + ipLocation.latitude + "," + ipLocation.longitude + "|";
            String parsedMap = IOUtils.toString(new URL(mapUrl));
            JSONObject parsedMapObject = (JSONObject) JSONValue.parseWithException(parsedMap);
            JSONArray routesArray = (JSONArray) parsedMapObject.get("routes");
            JSONObject legsArray = (JSONObject) routesArray.get(0);
            JSONArray legs = (JSONArray) legsArray.get("legs");
            JSONObject stepsArray = (JSONObject) legs.get(0);
            JSONArray steps = (JSONArray) stepsArray.get("steps");

            for (Object obj : steps) {
                JSONObject startArray = (JSONObject) obj;
                JSONObject start = (JSONObject) startArray.get("start_location");
                Double startLat = (Double) start.get("lat");
                Double startlng = (Double) start.get("lng");
                String startLoc = startLat + "," + startlng;
                route += startLoc + "|";
            }

            JSONObject endArray = (JSONObject) steps.get(steps.size() -1);
            JSONObject end = (JSONObject) endArray.get("end_location");
            Double endLat = (Double) end.get("lat");
            Double endLng = (Double) end.get("lng");
            String endLoc = endLat + "," + endLng;
            route += endLoc;

            //Retrieves the correct image                                    //center=" + ipLocation.latitude + "," + ipLocation.longitude + "&zoom=" + zoom + "&
            imageUrl = new URL("https://maps.googleapis.com/maps/api/staticmap?zoom=" + zoom + "&size=" + Main.width + "x" + Main.height + "&scale=" + scale + "&maptype=" + maptype + "&markers=" + ipLocation.latitude + "," + ipLocation.longitude + "|" + markers + "&markers=color:blue%7Clabel:Destination%7C" + endLoc + "&path=color:0x6464FF|weight:5" + route);
            bufferedImage = ImageIO.read(imageUrl);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        mapImage = SwingFXUtils.toFXImage(bufferedImage, null);
    }

    //Converts the coordinates into an address
    public String coordinatesToAddress(Float lat, Float lng) throws IOException, org.json.simple.parser.ParseException {

        URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&sensor=true");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        String formattedAddress = "";

        try {
            InputStream in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String result, line = reader.readLine();
            result = line;
            while ((line = reader.readLine()) != null) { result += line; }

            JSONParser parser = new JSONParser();
            JSONObject rsp = (JSONObject) parser.parse(result);

            if (rsp.containsKey("results")) {
                JSONArray matches = (JSONArray) rsp.get("results");
                JSONObject data = (JSONObject) matches.get(0);
                formattedAddress = (String) data.get("formatted_address");
            }
            return formattedAddress;
        }
        finally {
            urlConnection.disconnect();
            return formattedAddress;
        }
    }

    //Adds Markers to the map
    @Override
    public void mapInitialized() {
        MapOptions mapOptions = new MapOptions();
        mapOptions.center(new LatLong(ipLocation.latitude, ipLocation.longitude))
                .mapType(MapTypeIdEnum.ROADMAP)
                .overviewMapControl(true)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(true)
                .streetViewControl(false)
                .zoomControl(true)
                .zoom(12);
        map = mapView.createMap(mapOptions);
        Marker userLocation = new Marker(new MarkerOptions().position(new LatLong(ipLocation.latitude, ipLocation.longitude)).title("User").visible(true));
        map.addMarker(userLocation);
        LatLong latLongOrigin = new LatLong(ipLocation.latitude, ipLocation.longitude);

        directions = mapView.getDirec();
        DirectionsService ds = new DirectionsService();
        renderer = new DirectionsRenderer(true, map, directions);

        DirectionsWaypoint[] dw = new DirectionsWaypoint[0];
//        dw[0] = new DirectionsWaypoint("Utrecht");
//        dw[1] = new DirectionsWaypoint("Eindhoven");

        for(int i = 0; i < garageArray.size(); i++){
            JSONObject garage = (JSONObject) garageArray.get(i);
            String name = (String) garage.get("name");
            if(garage.get("locationForDisplay") != null) {
                JSONObject displayLocation = (JSONObject) garage.get("locationForDisplay");
                Double latitude = (Double) displayLocation.get("latitude");
                Double longitude = (Double) displayLocation.get("longitude");
                if (latitude > 51.8 && latitude < 52 && longitude > 4 && longitude < 4.75) {
                    LatLong latLongDestination = new LatLong(latitude, longitude);
                    Marker marker = new Marker(new MarkerOptions().position(new LatLong(latitude, longitude)).title(name).visible(true));
                    map.addMarker(marker);
                    InfoWindow info = new InfoWindow(new InfoWindowOptions().content("<p>" + name + "</p>"));
                    info.setPosition(new LatLong(latitude, longitude));
                    map.addUIEventHandler(marker, UIEventType.click, (JSObject obj) -> {
                        info.open(map);
                        chartDisplay(getGarageArea(name));
                        try {
                            DirectionsRequest dr = new DirectionsRequest(
                                    coordinatesToAddress(ipLocation.latitude, ipLocation.longitude),
                                    coordinatesToAddress(latitude.floatValue(), longitude.floatValue()),
                                    TravelModes.DRIVING, dw);
                            ds.getRoute(dr, this, renderer);
                        } catch (IOException | ParseException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    //Gets the area where the parkinggarage is located
    public String getGarageArea(String garageName) {
        ResultSet results = SQL.getDBResults("jdbc:mysql://127.0.0.1:3306/" + Main.DatabaseName, "root", "root", "select Gebied from parkeergarages WHERE GarageNaam='" + garageName + "'");
        try {
            while(results.next()) {
                return results.getString("Gebied");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Displays the correct piechart for the area in a subview
    public void chartDisplay(String areaName) {
        VBox subScene = new VBox();
        System.out.println(areaName);
        Button closeButton = new Button();
        closeButton.setText("X");
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                mapViewVBox.getChildren().remove(subScene);
            }
        });
        subScene.getChildren().addAll(closeButton, PieChartX.getSubScene(areaName));
        mapViewVBox.getChildren().add(subScene);
    }

    //Handles received directions
    @Override
    public void directionsReceived(DirectionsResult directionsResult, DirectionStatus directionStatus) {
        if(1==1){
            DirectionsResult e = directionsResult;
            DirectionStatus f = directionStatus;
        }
    }

    //Tries to find out the user location based on IP, returns a Location object
    public static Location geoIPLookUp() {
        LookupService cl = null;
        Location location = null;
        try {
            for(int j = 0; j < 2; j++) { //Tries to use the City database first. If the IP is not found, it falls back to the Country database.
                String db = null;
                if (j == 0) { db = "GeoLiteCity.dat"; }
                if (j == 1) { db = "GeoLiteCountry.dat"; }
                cl = new LookupService(db, LookupService.GEOIP_STANDARD);
                for (int i = 0; i < 3; i++) { //Tries getting the IP locally to locate the device. If it fails, it falls back to two webservices for the external IP.
                    if (location == null) {
                        String ip = null;
                        if (i > 0) {
                            URL checkIp = null;
                            if (i == 1) {
                                checkIp = new URL("http://ifconfig.me/ip");
                            }
                            if (i == 2) {
                                checkIp = new URL("http://checkip.amazonaws.com");
                            }
                            BufferedReader in = new BufferedReader(new InputStreamReader(checkIp.openStream()));
                            ip = in.readLine();
                        }
                        if (i == 0) {
                            ip = InetAddress.getLocalHost().getHostAddress();
                        }
                        location = cl.getLocation(ip);
                        System.out.println(i);
                    }
                }
                cl.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return location;
    }
}