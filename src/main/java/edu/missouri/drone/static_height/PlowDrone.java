package edu.missouri.drone.static_height;

import edu.missouri.drone.Drone;
import edu.missouri.frame.Area;
import edu.missouri.frame.Option;
import edu.missouri.geom.*;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import static edu.missouri.frame.Option.*;

public class PlowDrone extends Drone {

    public PlowDrone(Area area) {
        super(area);
    }

    public void route() {
        setHeading(getPolygon().widthLine().measure() + Math.PI/2);//set rotate angle
        for(Point p: subdivide(plan(getPolygon(), getLocation()))) {//get all the way points(start and end of each line)
            moveTo(new Point(p.x(), p.y(), cruiseAltitude));
            scan();
        }

    }

    public static List<Point> plan(Polygon poly, Point start) {
        Line widthLine = poly.widthLine();
        double theta1 = -widthLine.a().bearing(widthLine.b());
        double theta2 = -widthLine.b().bearing(widthLine.a());
        List<Point> option1 = PlowDrone.planTheta(poly, theta1, false);
        double score1 = option1.get(0).distance(start);
        List<Point> option2 =  PlowDrone.planTheta(poly, theta1, true);
        double score2 = option2.get(0).distance(start);
        List<Point> option3 =  PlowDrone.planTheta(poly, theta2, false);
        double score3 = option3.get(0).distance(start);
        List<Point> option4 =  PlowDrone.planTheta(poly, theta2, true);
        double score4 = option4.get(0).distance(start);

        double min = Util.min(score1, score2, score3, score4);
        List<Point> choice;
        if(min == score1) choice = option1;
        else if(min == score2) choice = option2;
        else if(min == score3) choice = option3;
        else if(min == score4) choice = option4;
        else throw new ArithmeticException();
        return choice;
    }

    public static List<Point> plan(Polygon original) {
        Line widthLine = original.widthLine();
        double theta = -widthLine.a().bearing(widthLine.b());
        return planTheta(original, theta, false);
    }


    public static List<Point> planTheta(Polygon original, double theta, boolean upsideDown) {
        double imageWidth = Option.defaultImageWidth();
        double imageHeight = Option.defaultImageHeight();
        double alt = cruiseAltitude;;
        Polygon poly = original.rotate(theta);
        List<Point> result = new ArrayList<>();

        double maxX = poly.rightmost().x() - imageWidth/2.0;
        double minX = poly.leftmost().x() + imageWidth/2.0;
        double PointY = poly.getTopPoint().y();
        double totalx = maxX - minX + imageWidth; //d
        double Lx = imageWidth;
        int m = (int)Math.ceil(totalx/Lx);
        if(m % 2 != 0){
            m = m + 1;
        }
        double dx = (totalx - Lx)/(m-1);
        int i = upsideDown? 1:0;
        int direction = i;
        for(double x = minX; x < maxX -1.0 ; x += dx) {
            Point iUp1 = poly.top(x - imageWidth/2.0);
            Point iDown1 = poly.bottom(x - imageWidth/2.0);
            Point iUp2 = poly.top(x - imageWidth/2.0 + 1.0);
            Point iDown2 = poly.bottom(x - imageWidth/2.0 + 1.0);
            double yUp;
            double yDown;
            yUp = iUp1.y();
            Point iUp = iUp1;
            yDown = iDown1.y();
            Point iDown = iDown1;

            Angle angleLeft = new Angle(iUp2,iUp1,iDown1);
            Angle angleRight = new Angle(iUp1,iDown1,iDown2);
            double d = yDown - yUp;
            double yLeft = yUp;
            double yRight = yDown;
            if(angleLeft.measure() > 0.5*Math.PI){
                yLeft = yLeft - 1.0 / Math.tan(Math.PI - angleLeft.measure()) * imageWidth ;
                d = d + 1.0 / Math.tan(Math.PI - angleLeft.measure()) * imageWidth;
            }
            if(angleRight.measure() > 0.5*Math.PI){
                yRight = yRight + 1.0 / Math.tan(Math.PI - angleRight.measure()) * imageWidth;
                d = d + 1.0 / Math.tan(Math.PI - angleRight.measure()) * imageWidth;
            }

            //two direction along the longest side.
        if(direction == 0 ){
            if(i%2 == 0) {
                //result.add(new Point(x ,yLeft + imageHeight/2.0  , alt));
                if (angleLeft.measure()>0.5*Math.PI){
                    if(x == minX){
                        result.add(new Point(x , yUp + imageHeight*0.5, alt));
                    }
                    else {
                        result.add(new Point(x , yUp + imageHeight*(0.5+Math.cos(angleLeft.measure()))
                                + imageWidth/Math.sin(angleLeft.measure() )  , alt));
                    }
                        result.add(new Point(x , yRight - imageHeight/2.0, alt));

                }
                else {
                    if(x == minX){
                        result.add(new Point(x , yUp + imageHeight*0.5, alt));
                    }
                    else {
                        result.add(new Point(x , yUp + imageHeight*0.5
                                + imageWidth/Math.sin(angleLeft.measure() )  , alt));
                    }

                    result.add(new Point(x , yRight - imageHeight/2.0, alt));
                }

            } else {
                if (angleLeft.measure()>0.5*Math.PI) {
                    result.add(new Point(x , yRight - imageHeight/2.0, alt));
                    //result.add(new Point(x , yLeft + imageHeight/2.0  , alt));
                    result.add(new Point(x , yUp + imageHeight*(0.5+Math.cos(angleLeft.measure()))
                            + imageWidth/Math.sin(angleLeft.measure() )  , alt));
                }
                else {
                    result.add(new Point(x , yRight - imageHeight/2.0, alt));
                    //result.add(new Point(x , yLeft + imageHeight/2.0  , alt));
                    result.add(new Point(x , yUp + imageHeight*0.5
                            + imageWidth/Math.sin(angleLeft.measure() )  , alt));
                }
            }
            }

        else {
                if(i%2 == 1) {
                    if (angleRight.measure()>0.5*Math.PI){
                        if(x == minX){
                            result.add(new Point(x , yDown - imageHeight*0.5, alt));
                        }
                        else {
                            result.add(new Point(x , yDown - imageHeight*(0.5+Math.cos(angleRight.measure()))
                                    - imageWidth/Math.sin(angleRight.measure() )  , alt));
                        }
                        result.add(new Point(x , yLeft + imageHeight/2.0, alt));
                    }
                    else {
                        if(x == minX){
                            result.add(new Point(x , yDown - imageHeight*0.5, alt));
                        }
                        else {
                            result.add(new Point(x , yDown - imageHeight*0.5
                                    - imageWidth/Math.sin(angleRight.measure() )  , alt));
                        }
                        result.add(new Point(x , yLeft + imageHeight/2.0, alt));
                    }
                }
                else {
                    if (angleRight.measure()>0.5*Math.PI) {
                        result.add(new Point(x , yLeft + imageHeight/2.0, alt));
                        //result.add(new Point(x , yLeft + imageHeight/2.0  , alt));
                        result.add(new Point(x , yDown - imageHeight*(0.5+Math.cos(angleRight.measure()))
                                - imageWidth/Math.sin(angleRight.measure() )  , alt));
                    }
                    else {
                        result.add(new Point(x , yLeft + imageHeight/2.0, alt));
                        //result.add(new Point(x , yLeft + imageHeight/2.0  , alt));
                        result.add(new Point(x , yDown - imageHeight*0.5
                                - imageWidth/Math.sin(angleRight.measure() )  , alt));
                    }

                }
            }
            i++;

        }

//         There's usually a remainder...
        Point iUp1 = poly.top(maxX - imageWidth/2.0);
        Point iDown1 = poly.bottom(maxX - imageWidth/2.0);
        Point iUp2 = poly.top(maxX - imageWidth/2.0 + 1.0);
        Point iDown2 = poly.bottom(maxX - imageWidth/2.0 + 1.0);
        double yUp;
        double yDown;
        yUp = iUp1.y();
        Point iUp = iUp1;
        yDown = iDown1.y();
        Point iDown = iDown1;

        Angle angleLeft = new Angle(iUp2,iUp1,iDown1);
        Angle angleRight = new Angle(iUp1,iDown1,iDown2);
        double d = yDown - yUp;
        double yLeft = yUp;
        double yRight = yDown;

        if(iUp != null && iDown != null) {
            if (i % 2 == 0) {
                if(angleLeft.measure() > 0.5*Math.PI) {
                    yLeft = yLeft - 1.0 / Math.tan(Math.PI - angleLeft.measure()) * imageWidth;
                }
                result.add(new Point(maxX, yLeft + imageHeight/2.0, alt));
                if (yLeft+imageHeight> PointY){
                    result.add(new Point(maxX, yLeft + imageHeight/2.0  , alt));
                }
                else {
                    result.add(new Point(maxX, PointY - imageHeight/2.0 , alt));
                }

            } else {
                if(angleRight.measure() > 0.5*Math.PI){
                    yRight = yRight + 1.0 / Math.tan(Math.PI - angleRight.measure()) * imageWidth;
                }
                result.add(new Point(maxX, yRight - imageHeight/2.0, alt));
                if (yRight-imageHeight< PointY) {
                    result.add(new Point(maxX, yRight -imageHeight/2.0, alt));
                }
                else
                {
                    result.add(new Point(maxX, PointY + imageHeight / 2.0, alt));
                }
                //result.add(new Point(maxX, yUp + imageHeight/2.0 , alt));
            }
        }

        if(result.isEmpty()) {
            result.add(new Point(poly.leftmost().x(),  upsideDown? poly.upmost().y() : poly.downmost().y()));
            result.add(new Point(poly.rightmost().x(), upsideDown? poly.downmost().y() : poly.upmost().y()));
        }

        Point center = poly.center();
        return rotateAll(result, -theta, new Point(center.x(), center.y(), cruiseAltitude));
    }

    public static List<Point> planBack(Polygon original, int StartIndex, int EndIndex) {
        double imageWidth = Option.defaultImageWidth();
        double imageHeight = Option.defaultImageHeight();
        double alt = cruiseAltitude;
        Point[] originPoints = original.toPoints();
        Point startOrigin = originPoints[StartIndex];
        Point endOrigin = originPoints[EndIndex];
        Line line = new Line(startOrigin,endOrigin);
        Point farthestPoint = original.farthest(line);
        Line widthLine = Line.perpendicularTo(line,farthestPoint);
        double theta = -widthLine.a().bearing(widthLine.b());
        Polygon poly = original.rotate(theta);

        Point[] points = poly.toPoints();
        List<Point> result = new ArrayList<>();
        Point start = points[StartIndex];
        Point end= points[EndIndex];
        double xStart = start.x() - imageWidth*0.5;
        double xEnd = end.x() - imageWidth*0.5;
        boolean flag = start.y()<end.y()?true:false;
        double yStart;
        double yEnd;
        if (flag){
            yStart = start.y() + imageHeight*0.5;
            yEnd = end.y() - imageHeight*0.5;
        }
        else {
            yStart = start.y() - imageHeight*0.5;
            yEnd = end.y() + imageHeight*0.5;
        }
        result.add(new Point(xStart,yStart,alt));
        result.add(new Point(xEnd,yEnd,alt));

        Point center = poly.center();
        return rotateAll(result, -theta, new Point(center.x(), center.y(), cruiseAltitude));
    }


    public static double traversalAltitude(Polygon p, double budget) {
        // first, non-coverage costs
        // cost to go from length to length is relative to perimeter
        // cost for the remainder segment is relative to area/width
        double testBudget = budget - p.perimeter()/4.0 - 2*p.reimannArea(500)/p.width();
        double width = p.reimannArea(500) / (testBudget);


        testBudget = budget - p.perimeter()/4.0 - 2*p.reimannArea(width)/p.width();
        return p.reimannArea(width) / (testBudget * 2 * Math.sin(Drone.FOV_HEIGHT));

    }

    private static List<Point> rotateAll(List<Point> points, double theta, Point c) {
        List<Point> result = new ArrayList<>();

        for (Point point : points) {
            result.add(new Point(
                    ((point.x() - c.x()) * Math.cos(theta) - (point.y() - c.y()) * Math.sin(theta)) + c.x(),
                    ((point.x() - c.x()) * Math.sin(theta) + (point.y() - c.y()) * Math.cos(theta)) + c.y(),
                    c.z()));
        }
        return result;
    }

    public void visualize(Graphics g) {
        // nothing special to visualize
    }

    @Override
    public String toString() {
        return "Plowlike";
    }
}
