package edu.missouri.frame;

import edu.missouri.geom.Point;
import edu.missouri.geom.Polygon;

public class Positions {

    public static Point[] random(int n, Polygon poly) {
        Point[] result = new Point[n];

        for(int i = 0; i < n; i++) {
            Point p = new Point(
                    Math.random() * poly.getBounds().width + poly.leftmost().x(),
                    Math.random() * poly.getBounds().height + poly.upmost().y()
            );
            if(! poly.contains(p)) i--;
            else result[i] = p;
        }
        return result;
    }

    public static Point[] clustered(int n, Polygon poly) {
        Point[] result = new Point[n];

        int i = 0;
        while(i < n) {
            int clusterSize = Math.min((int) (Math.random()*8 + 2), n - i);
            double clusterWidth = poly.width() / 8 * Math.random() + 5;
            Point center = random(1, poly)[0];

            for(int j = 0; j < clusterSize; j++){
                Point p = random(1, center.toSquare(clusterWidth))[0];

                if(! poly.contains(p)) j--;
                else {
                    result[i] = p;
                    i++;
                }
            }
        }
        return result;
    }
}
