package edu.missouri.frame;

import edu.missouri.drone.Drone;
import edu.missouri.drone.variable_height.ImprovedDirectDrone;
import edu.missouri.geom.CordtoGPS;
import edu.missouri.geom.Point;
import edu.missouri.geom.csvCreate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ReadFlightParameters {
    public static double height = 10;
    public static double overlap;
    public static Point[] vertices;
    public static GePoint startPoint;


    public List<GePoint> wayPoints;
    public List<Double> altitudes;
    public List<Boolean> isTurnings;

    public static Area area;
    public static Drone drone;

    public ReadFlightParameters(List<GePoint> GPSvertices, GePoint GPSstartPoint, double height, double overlap){

        this.overlap = overlap;
        this.startPoint = GPSstartPoint;
        int verticesNum = GPSvertices.size();
        Point[] verticesTmp = new Point[verticesNum];
        for(int i = 0;i<verticesNum;i++){
            List<Point> points = new ArrayList<Point>();
            verticesTmp[i] = GPSToCord(GPSvertices.get(i),GPSstartPoint);
        }
        this.vertices = verticesTmp;
        this.height = height;

        planPath();
    }

    public void planPath(){
        area = Area.readPolygonFromCSV();
        drone = new ImprovedDirectDrone(area);
        Map<Point,Boolean> maps = ((ImprovedDirectDrone) drone).routes();
        Iterator<Map.Entry<Point,Boolean>> entries = maps.entrySet().iterator();
        List<Point> waypoints = new ArrayList<>();
        List<Point> turningPoints = new ArrayList<>();
        while (entries.hasNext()){
            Map.Entry<Point,Boolean> entry  = entries.next();
            if(entry.getValue()==true){
                turningPoints.add(entry.getKey());
            }
            waypoints.add(entry.getKey());
        }

        List<CordtoGPS> coordinates = new csvCreate(waypoints,turningPoints).getCoordinates();
        List heights = new ArrayList<Double>();
        List wayPoints = new ArrayList<Point>();
        List isTurnings = new ArrayList<Boolean>();
        // return parameters

        for(CordtoGPS coordinate: coordinates){
            heights.add(coordinate.getAltitude());
            wayPoints.add(new GePoint(coordinate.getLatitude(),coordinate.getLongitude()));
            isTurnings.add(coordinate.isTurning());

        }
        this.altitudes = heights;
        this.isTurnings = isTurnings;
        this.wayPoints = wayPoints;

    }

    public Point GPSToCord(GePoint target,GePoint standard){
        double[] tmpPoint = GPStoCord.getCord(target,standard);
        return new Point(tmpPoint[1],tmpPoint[0]);
    }

    public List<GePoint> getWaypoints(){ return wayPoints; }
    public List<Double> getHeight(){
        return altitudes;
    }
    public List<Boolean> getIsTurning(){
        return isTurnings;
    }
}
