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


public class Main {


    public static void main(String args[]){
        //arg[0]=GPSVertices arg[1]=GPSstartpoint arg[2]=height arg[3] = overlap
        Double p0_x = 38.9129228409671;//x-lati,y-longti
        Double p0_Y = -92.2959491063508;
        Double p1_x = 38.9129228409671;
        Double p1_Y = -92.2959491063508;
        Double p2_x = 38.9113696239793;
        Double p2_Y = -92.2960270901189;
        Double p3_x = 38.9113237082811;
        Double p3_Y = -92.2939188738332;
        Double p4_x = 38.9128514328361;
        Double p4_Y = -92.2940476198659;
        List<GePoint> GPSVertices = new ArrayList<GePoint>();
        GPSVertices.add(new GePoint(p1_x, p1_Y));
        GPSVertices.add(new GePoint(p2_x, p2_Y));
        GPSVertices.add(new GePoint(p3_x, p3_Y));
        GPSVertices.add(new GePoint(p4_x, p4_Y));
        GePoint GPSstartpoint = new GePoint(p0_x, p0_Y);
        double height = 20;
        double overlap = 0.1;
//        String[] test = new String[4];
//        test[0] = "38.9129228409671,-92.2959491063508 38.9113696239793,-92.2960270901189 38.9113237082811,-92.2939188738332 38.9128514328361,-92.2940476198659";
//        test[1] = "38.9129228409671,-92.2959491063508";
//        test[2] = "20";
//        test[3] = "0";
//        String verticeString = test[0];
//        String startPointString = test[1];
//        String heightString = test[2];
//        String overlapString = test[3];
//        List<GePoint> GPSVertices = splitPointString(verticeString);
//        GePoint GPSstartpoint = splitPointString(startPointString).get(0);
//        double height = Double.parseDouble(heightString);
//        double overlap = Double.parseDouble(overlapString);


// below are output
        ReadFlightParameters model = new ReadFlightParameters(GPSVertices, GPSstartpoint, height, overlap);
        List<GePoint> wayPoints = model.getWaypoints();
        List<Boolean> isTurnings = model.getIsTurning();
        List<Double> altitudes = model.getHeight();
    }

}

