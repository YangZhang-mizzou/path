package edu.missouri.frame;


import edu.missouri.drone.Drone;
import edu.missouri.drone.static_height.HuangDrone;
import edu.missouri.drone.variable_height.ImprovedDirectDrone;

public class TextMain {
    public static void main(String[] args) {

        Option.numObjects = 50;
        Option.minCruiseAltitude = 20;
        Option.cruiseAltitude = Option.minCruiseAltitude;
        Option.energyBudget = Drone.TOTAL_ENERGY;
        Option.distributor = Area.CLUSTERED;

        double sum = 0.0;

        for(int i = 0; i < 400; i++) {
            try {
                Area area = new Area(4);
                Option.cruiseAltitude = Option.minCruiseAltitude;
                Drone better = new ImprovedDirectDrone(area);

                sum += better.objectAccuracy();

            } catch (Exception all) {}
        }

        System.out.println(sum / 400.0);
    }
}