 package edu.missouri.drone;

 import edu.missouri.frame.Area;
 import edu.missouri.frame.Detectable;
 import edu.missouri.frame.Input;
 import edu.missouri.frame.Option;
 import edu.missouri.geom.Point;
 import edu.missouri.geom.Polygon;
 import edu.missouri.geom.*;
 import org.apache.commons.math3.special.Erf;

 import java.awt.*;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public abstract class Drone {

    // From the DJI spec sheet on the Mavic Pro:
    public static final double ASPECT_RATIO             = 3.0/4.0;                           // ratio
    public static final double FOV_WIDTH                = Math.toRadians(78.8);              // radians
    public static final double FOV_HEIGHT               = 1.0/ASPECT_RATIO * FOV_WIDTH;      // radians
    public static final double MAX_TRAVEL_DISTANCE      = 12874.8;                           // meters
    public static final double ASCENT_SPEED             = 5.0;                               // meters per second
    public static final double DESCENT_SPEED            = 3.0;                               // meters per second
    public static final double MAX_SPEED                = 40;                                // meters per second
    public static final double MAX_FLIGHT_TIME          = 1620;                              // seconds
    public static final double REALISTIC_FLIGHT_TIME    = 1260;                              // seconds
    public static final double TOTAL_ENERGY             = 157183.2;                          // joules

    // The energy model we're using is for a different drone (the Iris), so we have to fit it to the Mavic Pro.
    // The Mavic is a much nicer drone - weighs half as much with higher quality motors and
    // This one is calculated such that the total travel distance given by DJI
    // costs about the total energy of the drone at the optimal speed (15.5 m/s, apparently)
    public static final double EFFICIENCY_FACTOR  = 0.4;


    private Point location;

    private double heading;
    private Area area;
    private List<Capture> captures;
    private List<Point> route;

    public Drone(Area area) {
        route = new ArrayList<>();
        reroute(area, Option.energyBudget);
    }

    protected class Capture {

        public List<Detectable> detectables;
        public Polygon polygon;
        public double height;

        public Capture(Polygon p, List<Detectable> d, double h) {
            this.polygon = p;
            this.detectables = d;
            this.height = h;
        }
    }

    protected Polygon getPolygon() {
        return area.toPolygon();
    }
    protected Point getTargetEnd() {
        return area.getEnd();
    }
    protected Point getLocation() {
        return location;
    }
    public double getHeading() {
        return heading;
    }
    public void setHeading(double heading) {
        this.heading = heading;
    }
    protected List<Capture> getCaptures() {
        return captures;
    }

    protected Capture scan() {
        // these are backwards for reasons
        double h = 2 * getLocation().z() * Math.sin(FOV_WIDTH/2);
        double w = 2 * getLocation().z() * Math.sin(FOV_HEIGHT/2);
        Polygon p = new Polygon(
                new Point(getLocation().x() + w/2, getLocation().y() + h/2),
                new Point(getLocation().x() + w/2, getLocation().y() - h/2),
                new Point(getLocation().x() - w/2, getLocation().y() - h/2),
                new Point(getLocation().x() - w/2, getLocation().y() + h/2)
        ).rotate(heading);
        List<Detectable> detected = area.getDetectables(p, getLocation().z());
        Capture c = new Capture(p, detected, getLocation().z());
        captures.add(c);
        return c;
    }

    protected Polygon scanArea(Point p) {
        double h = 2 * p.z() * Math.sin(FOV_WIDTH/2);
        double w = 2 * p.z() * Math.sin(FOV_HEIGHT/2);
        return new Polygon(
                new Point(p.x() + w/2, p.y() + h/2),
                new Point(p.x() + w/2, p.y() - h/2),
                new Point(p.x() - w/2, p.y() - h/2),
                new Point(p.x() - w/2, p.y() + h/2)
        );
    }

    public void moveTo(Point p) {
        route.add(p);
        this.location = p;
    }

    public void reroute(Area area) {
        route = new ArrayList<>();
        this.area = area;
        moveTo(area.getStart());
        this.setHeading(0.0);
        this.captures = new CopyOnWriteArrayList<>();
        route();
        route.add(0, area.getStart());
    }
    public void reroute() {
        route = new ArrayList<>();
        moveTo(area.getStart());
        this.setHeading(0.0);
        this.captures = new CopyOnWriteArrayList<>();
        route();
        route.add(0, area.getStart());
    }

    public void reroute(double energy) {
        Option.cruiseAltitude = Option.minCruiseAltitude;
        reroute();
        while(energyUsed() > energy && Option.cruiseAltitude < Option.maxAltitude) {
            Option.cruiseAltitude += 1;
            reroute();
        }
    }

    public void reroute(Area area, double energyBudget) {
        Option.cruiseAltitude = Option.minCruiseAltitude;
        reroute(area);
        double e = energyUsed();
        while(e > energyBudget && Option.cruiseAltitude < Option.maxAltitude) {
            Option.cruiseAltitude += 1;
            reroute(area);
            e = energyUsed();
        }
    }

    public abstract void route();
    public abstract void visualize(Graphics g);


    public boolean predict(Detectable d) {
        return guess(lowestDetected(d));
    }

    public Detectable lowestDetected(Detectable p) {
        List<Detectable> superlist = new ArrayList<>();
        for(Capture c: captures) superlist.addAll(c.detectables);
        List<Detectable> finds = Util.occurencesOf(p, superlist);
        Detectable best = null;
        for(Detectable f: finds) if(best == null || f.detectedFrom() < best.detectedFrom()) best = f;
        if(best == null) return null;
        return best;
    }


    public boolean guess(Detectable d) {
        if(d == null) return false; // we didn't see it; we have no idea
        return d.confidence() > Option.confidenceThreshold;
    }


    public void draw(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setStroke(new BasicStroke(3));

        if(route == null || route.isEmpty()) {
            g.setColor(Color.RED);
            g.drawString("That algorithm cannot handle this area.", 5, (int) (area.getHeight()-5));
            return;
        }

        for(Capture p: captures) {
            for(Detectable d: p.detectables) {
                if(predict(d)) g.setColor(Color.CYAN);
                else g.setColor(Color.ORANGE);
                g.drawOval(d.ix()-6, d.iy()-6, 12, 12);

                if(Input.mouse().sqDistance(d) < 100) {
                    if(guess(d)) g.setColor(new Color(0, 80, 80));
                    else g.setColor(new Color(100, 40, 0));
                    g.drawString(
                            String.format("(%dm: %2.0f%%)", (int) d.detectedFrom(), d.confidence() *100),
                            d.ix()+5,
                            (int) (d.iy()+d.detectedFrom() + 20)
                    );
                }
            }
        }

        for(Capture p: captures) {
            g.setColor(new Color(100, 100, 100, 100));
            g.fillPolygon(p.polygon.toAWTPolygon());
        }

        for(Line l: Line.arrayFromPoints(route.toArray(new Point[0]))) {

            Color colorA = Color.getHSBColor((float) ((l.a().iz() % Option.maxAltitude)/Option.maxAltitude), 1f, .65f);
            Color colorB = Color.getHSBColor((float) ((l.b().iz() % Option.maxAltitude)/Option.maxAltitude), 1f, .65f);

            g.setPaint(new GradientPaint(
                    l.a().ix(), l.a().iy(), new Color(colorA.getRed(), colorA.getGreen(), colorA.getBlue(), 180),
                    l.b().ix(), l.b().iy(), new Color(colorB.getRed(), colorB.getGreen(), colorB.getBlue(), 180)));

            g.drawLine(l.a().ix(), l.a().iy(), l.b().ix(), l.b().iy());
        }

        g.setStroke(new BasicStroke(1));
        g.fillOval(getLocation().ix(), getLocation().iy(), 5, 5);
        visualize(g);
    }

    public static List<Point> subdivide(List<Point> points) {
        List<Point> result = new ArrayList<>();
        for(Line l: Line.arrayFromPoints(points.toArray(new Point[0]))) result.addAll(l.toSubpoints(Option.defaultImageHeight()));
        result.add(points.get(points.size()-1));
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    // Metrics:

    public double length() {
        if(route == null || route.size() <= 3) return -1;
        Line[] lines = Line.arrayFromPoints(route.toArray(new Point[0]));
        double sum = 0;
        for(Line l: lines) {
            sum += l.length();
        }
        return sum;
    }

    public double angularLength() {
        if(route == null || route.size() <= 3) return -1;
        Angle[] lines = Angle.arrayFromPoints(route.toArray(new Point[0]));
        double sum = 0;
        for(Angle a: lines) sum += Math.toDegrees(Math.abs(a.measure() - Math.PI));
        return sum;
    }

    public double areaCovered() {
        if(route == null || route.size() <= 3) return -1;
        Line[] lines = Line.arrayFromPoints(route.toArray(new Point[0]));
        double covered = 0;
        double eligible = 0;

        for(int i = 0; i < area.getWidth(); i += 5) {
            for(int j = 0; j < area.getHeight(); j += 5) {

                Point p = new Point(i, j);
                if(! area.contains(p.x(), p.y())) {
                    continue;
                } else eligible++;
                boolean success = false;

                for(Line l: lines) {
                    if(p.distance(l) < Option.defaultImageWidth()) {
                        success = true;
                        break;
                    }
                }
                if(success) covered++;
            }
        }
        return covered / eligible;
    }

    public double objectAccuracy() {
        double correct = 0;
        for(Detectable d: area.getDetectables()) {
            if(predict(d) == d.real()) correct++;
        }
        return correct/area.getDetectables().size();
    }

    public double objectPrecision() {
        double truePos = 0;
        double falsePos = 0;
        for(Detectable d: area.getDetectables()) {
            if(predict(d) && d.real()) truePos++;
            if(predict(d) && ! d.real()) falsePos++;
        }
        return truePos / (truePos + falsePos);
    }

    public double objectRecall() {

        double truePos = 0;
        double falseNeg = 0;
        for(Detectable d: area.getDetectables()) {
            if(predict(d) && d.real()) truePos++;
            if(! predict(d) && d.real()) falseNeg++;
        }
        return truePos / (truePos + falseNeg);
    }

    public int splitDecisions() {
        // Detectables where zooming in made them look different
        List<Detectable> superlist = new ArrayList<>();
        for(Capture c: captures) superlist.addAll(c.detectables);
        int result = 0;
        for(Detectable d: area.getDetectables()) {

            List<Detectable> finds = Util.occurencesOf(d, superlist);
            if(finds.size() < 2) continue;
            boolean expectedReal = guess(finds.get(0));
            // I think this is the first time I've ever used the xor operator in Java
            for(Detectable f: finds) if(guess(f) != expectedReal) {
                result++;
                break;
            }
        }
        return result;
    }

    public double sumConfidences() {
        double sum = 0;
        double n = 0;
        for(Detectable d: area.getDetectables()) {
            Detectable h = lowestDetected(d);
            if(d.real()) n++;
            if(h == null) continue;
            sum += (h.real())? h.confidence() : -h.confidence();
        }
        return sum;
    }

    // Energy calculations
    // credit to DiFranco and Buttazzo

    public double energyUsed() {
        double cruiseSpeed = Option.cruiseSpeed;

        Line[] lines = Line.arrayFromPoints(route.toArray(new Point[0]));

        double speedIn, speedOut;
        double result = 0.0;

        for(int i = 0; i < lines.length; i++) {
            // We could probably use some linear algebra to avoid the trig functions here,
            // but I don't trust myself to do that.

            if(i == 0) speedIn = 0;
            else {
                Angle aIn = new Angle(lines[i-1].a(), lines[i].a(), lines[i].b());
                speedIn = (! Util.within(Math.PI / 2, 3 * Math.PI / 2, aIn.measure())) ? 0 : Math.abs(Math.cos(aIn.measure()) * cruiseSpeed);
            }

            if(i >= lines.length-1) speedOut = 0;
            else {
                Angle aOut = new Angle(lines[i].a(), lines[i].b(), lines[i+1].b());
                speedOut = (! Util.within(Math.PI / 2, 3 * Math.PI / 2, aOut.measure())) ? 0 : Math.abs(Math.cos(aOut.measure()) * cruiseSpeed);
            }

            result += legEnergy(lines[i], speedIn + 0.01, cruiseSpeed + 0.01, speedOut + 0.01);
        }
        return result;
    }

    public static double legEnergy(Line path, double startSpeed, double cruiseSpeed, double endSpeed) {
        double dist = path.length2D();

        // These calculations are made for a drone with max speed 15 m/s
        // The Mavic is quite a bit bigger so we're going to (naively) scale things up
        startSpeed *= 15/MAX_SPEED;
        cruiseSpeed *= 15/MAX_SPEED;
        endSpeed *= 15/MAX_SPEED;

        double borderDist = accDist(startSpeed, cruiseSpeed) + decDist(cruiseSpeed, endSpeed);
        while(borderDist > dist && cruiseSpeed > 0) {
            cruiseSpeed -= 0.5;
            borderDist = accDist(startSpeed, cruiseSpeed) + decDist(cruiseSpeed, endSpeed);
        }

        // at distances around 1 meter we start having some problems
        if(cruiseSpeed < 0) {
//            System.err.println("Distance " + dist + " too short to calculate energy cost");
            return EFFICIENCY_FACTOR * cruiseEnergy(dist, cruiseSpeed);
        }

        double result =  EFFICIENCY_FACTOR * accEnergy(startSpeed, cruiseSpeed)
              + EFFICIENCY_FACTOR * decEnergy(cruiseSpeed,endSpeed)
              + EFFICIENCY_FACTOR * cruiseEnergy(dist - borderDist, cruiseSpeed);

        if(path.dz() < 0) result += 210 * path.dz() / DESCENT_SPEED * EFFICIENCY_FACTOR;
        if(path.dz() > 0) result += 250 * path.dz() / ASCENT_SPEED  * EFFICIENCY_FACTOR;

        return result;
    }

    private static double accEnergy(double vIn, double vOut) {
        double tIn  = accTime(vIn);
        double tOut = accTime(vOut);
        return (Math.pow(tOut, 2.35)/2.35 + 220*tOut)
                - (Math.pow(tIn, 2.35)/2.35 + 220*tIn);
    }
    private static double accTime(double v) {
        return -4 * Math.log(16.138/(1.138+v) - 1) + 11;
    }
    private static double accDist(double vIn, double vOut) {
        double tIn  = accTime(vIn);
        double tOut = accTime(vOut);
        return (64.552*Math.log(15.6426+Math.pow(Math.E, 0.25*tOut)) - 1.138*tOut)
             - (64.552*Math.log(15.6426+Math.pow(Math.E, 0.25*tIn )) - 1.138*tIn);
    }

    public static double decEnergy(double vIn, double vOut) {
        double tIn = decTime(vIn);
        double tOut = decTime(vOut);
        return (313.769*tOut-24.349*Math.pow(tOut, 2)+2.53967*Math.pow(tOut, 3)-0.1105*Math.pow(tOut, 4)+0.0017*Math.pow(tOut, 5))
             - (313.769*tIn -24.349*Math.pow(tIn , 2)+2.53967*Math.pow(tIn , 3)-0.1105*Math.pow(tIn , 4)+0.0017*Math.pow(tIn , 5));
    }
    private static double decTime(double v) {
        return Math.sqrt(Math.log((v + 0.1)/15.1)/-0.02);
    }
    private static double decDist(double vIn, double vOut) {
        double tIn = decTime(vIn);
        double tOut = decTime(vOut);
        return ((-0.1*tOut)+94.6252*Erf.erf(0.141421 * tOut))-((-0.1*tIn)+94.6252*Erf.erf(0.141421*tIn));
    }

    public static double cruiseEnergy(double dist, double speed) {
        return (Math.pow(.25*speed, 4) - 8*Math.pow(.25*speed, 2) + 220) * dist/speed;
    }
}
