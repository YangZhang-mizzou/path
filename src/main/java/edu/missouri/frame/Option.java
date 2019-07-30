package edu.missouri.frame;

import static edu.missouri.drone.Drone.FOV_HEIGHT;
import static edu.missouri.drone.Drone.FOV_WIDTH;

public class Option {

    public static double cruiseAltitude = 30;
    public static double minCruiseAltitude = 10;
    public static double cruiseSpeed = 15.5;
    public static double maxAltitude = 500;

    public static double defaultImageHeight() {
        return 2 * cruiseAltitude * Math.sin(FOV_HEIGHT / 2);
    }

    public static double defaultImageWidth() {
        return 2 * cruiseAltitude * Math.sin(FOV_WIDTH / 2);
    }

    public static int distributor = Area.RANDOM;
    public static int numObjects = 40;
    public static double confidenceThreshold = 0.5;
    public static double energyBudget = 100000;
}