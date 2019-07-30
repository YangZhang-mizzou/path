package edu.missouri.drone.variable_height;

import edu.missouri.drone.Drone;
import edu.missouri.drone.static_height.JiaoDrone;
import edu.missouri.drone.static_height.PlowDrone;
import edu.missouri.frame.Area;
import edu.missouri.frame.Detectable;
import edu.missouri.frame.Option;
import edu.missouri.geom.Line;
import edu.missouri.geom.Point;
import edu.missouri.geom.Polygon;

import java.awt.Graphics;
import java.util.*;

public class ImprovedDirectDrone extends Drone {


    Queue<Polygon> regions = new LinkedList<>();
    double thoroughness = 1.0;

    public ImprovedDirectDrone(Area area) {
        super(area);
        regions.addAll(new JiaoDrone(area).decompose(getPolygon(), 500));
    }

    public void route() {
        List<Point> done = new ArrayList<>();

        double overviewAltitude = Option.cruiseAltitude;
        setHeading(getPolygon().widthLine().measure() + Math.PI/2.0);

        Queue<Point> currentPoints = new LinkedList<>(Drone.subdivide(PlowDrone.plan(getPolygon(), getLocation())));

        while(! currentPoints.isEmpty()) {
            Point p = currentPoints.remove();
            p = new Point(p.x(), p.y(), overviewAltitude);
            moveTo(p);
            Capture c = scan();

            List<Point> toDo = new ArrayList<>();
            for(Detectable d: c.detectables) {
                if (done.contains(d)) continue;
                done.add(d);
                double k = altitudeNeeded(d);
                if (k >= overviewAltitude) continue;
                Point q = scanArea(new Point(d.x(), d.y(), k-10)).closest(getLocation());
                Point p2 = new Point(q.x(), q.y(), k);
                toDo.add(p2);
            }

            for(Point q: heuristicTSP(toDo, getLocation(), currentPoints.isEmpty()? null : currentPoints.peek())) {
                moveTo(q);
                scan();
            }
        }
    }

    private double altitudeNeeded(Detectable d) {
        double k = d.detectedFrom() * Math.abs(d.confidence() - Option.confidenceThreshold) * (1 / thoroughness);
        return Math.max(k, Option.minCruiseAltitude);
    }

    @Override
    public void visualize(Graphics g) {

    }

    public List<Point> heuristicTSP(List<? extends Point> points, Point start, Point end) {

        if(end == null) end = getTargetEnd();

        final Point finalEnd = end;
        Collections.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point point, Point t1) {
                return (int) (point.distance(finalEnd) - t1.distance(finalEnd));
            }
        });

        List<Point> result = new ArrayList<>();
        result.add(start);
        result.add(end);


        for(Point p: points) {
            int bestIndex = 1;
            double bestLength = Double.MAX_VALUE;
            for(int j = 1; j < result.size()-1; j++) {
                result.add(j, p);
                double d = pathLength(result);
                if(d < bestLength) {
                    bestLength = d;
                    bestIndex = j;
                }
                result.remove(p);
            }
            result.add(bestIndex, p);
        }
        result.remove(start);
        result.remove(end);
        return result;
    }

    public void reroute(double energy) {
        Option.cruiseAltitude = Option.minCruiseAltitude;
        thoroughness = 1.0;
        reroute();
        while(energyUsed() > energy && Option.cruiseAltitude < Option.maxAltitude) {
            Option.cruiseAltitude += 1;
            thoroughness -= 1.0/Option.maxAltitude;
            reroute();
        }
    }

    public void reroute(Area area, double energyBudget) {
        Option.cruiseAltitude = Option.minCruiseAltitude;
        thoroughness = 1.0;
        reroute(area);
        double e = energyUsed();
        while(e > energyBudget && Option.cruiseAltitude < Option.maxAltitude) {
            Option.cruiseAltitude += 1;
            thoroughness -= 1.0/Option.maxAltitude;
            reroute(area);
            e = energyUsed();
        }
    }

    private double pathLength(List<Point> points) {
        double sum = 0;
        for(Line l: Line.arrayFromPoints(points.toArray(new Point[0]))) {
            sum += l.length();
        }
        return sum;
    }
}
