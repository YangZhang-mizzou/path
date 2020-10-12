package edu.missouri.geom;


import java.awt.*;
import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("unused")
public class Line {

    private final double MAX_VALUE = 10000;

    private final Point a;
    private final Point b;
    private final int type;

    public static final int SEGMENT = 0;
    public static final int RAY = 1;
    public static final int INFINITE = 2;

    // All Lines are defined by two points
    public Line(Point a, Point b) {
        this.a = a;
        this.b = b;
        this.type = SEGMENT;
    }
    public Line(Point a, Point b, int type) {
        this.a = a;
        this.b = b;
        this.type = type;
    }
    public Line(Line l, int type) {
        this(l.a, l.b, type);
    }
    public Line(Point a, double m) {
        this(a, new Point(a.x() + 1, a.y() + m));
    }
    public Line(Point a, double m, int type) {
        this(a, new Point(a.x() + 1, a.y() + m), type);
    }

    public static Line parallelTo(Line l, Point p) {
        return new Line(p, new Point(p.x()+l.dx(), p.y()+l.dy()), INFINITE);
    }

    public static Line perpendicularTo(Line l, Point p) {
        double m = l.slope(); // this could be 0!
        Line k;
        if(m != 0) k = new Line(p, new Point(p.x() + 1, p.y() - 1/l.slope()), Line.INFINITE);
        else k = new Line(p, new Point(p.x(), p.y()+1), Line.INFINITE);
        Point r = new Line(l, Line.INFINITE).intersection(k);
        return new Line(p, r);
    }

    public static Line infPerpendicularTo(Line l, Point p) {
        double m = l.slope(); // this could be 0!
        Line k;
        if(m != 0) k = new Line(p, new Point(p.x() + 1, p.y() - 1/l.slope()), Line.INFINITE);
        else k = new Line(p, new Point(p.x(), p.y()+1), Line.INFINITE);
        return k;
    }

    public double dx()      { return b.x()  - a.x(); }
    public double dy()      { return b.y()  - a.y(); }
    public double dz()      { return b.z()  - a.z(); }
    public double dn(int n) { return b.n(n) - a.n(n); }
    public Point a() { return a; }
    public Point b() { return b; }

    public Point intersection(Line l) {

        // Cramer's Rule - not the best, but it works
        double det1 = Util.det(new double[][] {{l.dx(), l.dy()}, {-dx(), -dy()}});
        double det2 = Util.det(new double[][] {{l.dx(), l.dy()}, {a.x() - l.a().x(), a.y() - l.a().y()}});

        if(det1 == 0) return null;

        double t = det2/det1;
        Point p = new Point(a.x() + t*dx(), a.y() + t*dy());

        // Segment checking. A ray or "infinite" line can be created with arbitrarily far Points.
        if(type == SEGMENT) {
            if (!Util.within(a().x(), b().x(), p.x(), .5)) return null;
            if (!Util.within(a().y(), b().y(), p.y(), .5)) return null;
        } else if(type == RAY) {
            if(dx() > 0 && p.x() < a().x()) return null;
            if(dx() < 0 && p.x() > a().x()) return null;
            if(dy() > 0 && p.y() < a().y()) return null;
            if(dy() < 0 && p.y() > a().y()) return null;
        }

        if(l.type == SEGMENT) {
            if (!Util.within(l.a().x(), l.b().x(), p.x())) return null;
            if (!Util.within(l.a().y(), l.b().y(), p.y())) return null;
        } else if(l.type == RAY) {
            if(l.dx() > 0 && p.x() < l.a().x()) return null;
            if(l.dx() < 0 && p.x() > l.a().x()) return null;
            if(l.dy() > 0 && p.y() < l.a().y()) return null;
            if(l.dy() < 0 && p.y() > l.a().y()) return null;
        }

        return p;
    }

    public String toString() {
        return String.format("(%.2f, %.2f)-(%.2f, %.2f)", a.x(), a.y(), b.x(), b.y());
    }

    public void render(Graphics g) {
        if(type == SEGMENT) g.drawLine(a.ix(), a.iy(), b.ix(), b.iy());
        if(type == RAY) g.drawLine(a.ix(), a.iy(), b.ix()+10000, (int) (b.iy()+10000*slope()));
        if(type == INFINITE) g.drawLine(a.ix()-10000, (int) (a.iy()-10000*slope()), b.ix()+10000, (int) (b.iy()+10000*slope()));
    }

    public Collection<? extends Point> toSubpoints(double segLength) {
        if(! (type == SEGMENT)) throw new ArithmeticException("you can't split an infinite ray or line into subpoints");

        int n = (int) (length()/segLength) + 1; // one extra for the remainder, but not another for the last point
        double dxi = dx()/n;
        double dyi = dy()/n;
        Point[] result = new Point[n];

        for(int i = 0; i < n; i++) {
            result[i] = new Point(a.x() + i*dxi, a.y() + i*dyi);
        }
        return Arrays.asList(result);
    }

    public Point midpoint() {
        return new Point((a.x()+b.x())/2.0, (a.y() + b.y())/2.0);
    }

    public Line rotate(double theta) {
        Point m = midpoint();
        Point a2 = new Point(a.x() - m.x(), a.y() - m.y());
        Point b2 = new Point(b.x() - m.x(), b.y() - m.y());

        a2 = a2.rotate(theta);
        b2 = b2.rotate(theta);

        a2 = new Point(a2.x() + m.x(), a2.y() + m.y());
        b2 = new Point(b2.x() + m.x(), b2.y() + m.y());

        return new Line(a2, b2);
    }

    public boolean contains(Point p) {
        return p.distance(this) < 1;
    }

    public boolean parallel(Line l) {
        return Util.approx(l.slope(), slope(), 0.2);
    }
    public boolean parallel(Line l, double t) {
        return Util.approx(l.slope(), slope(), t);
    }
    public boolean perpendicular(Line l) { return Util.approx(l.slope(), -1.0/slope(), 0.001);}

    public double length() {
        return a.distance(b);
    }
    public double length2D() {
        return Math.sqrt(dx()*dx() + dy()*dy());
    }

    public double slope() { return dy() / dx(); }
    public double measure() { return a.bearing(b); }

    public Point closest(Point p) {
        Point r = intersection(new Line(p, -1/slope(), Line.INFINITE));
        if(r == null) {
            if(p.distance(a) < p.distance(b)) return a;
            return b;
        } else return r;
    }

    public boolean inSpan(Point p) {
        double len = a.sqDistance(b);
        double t = ((p.ix() - a.ix()) * (b.ix() - a.ix()) + (p.iy() - a.iy()) * (b.iy() - a.iy())) / len;
        return (t < 1.0 && t > 0);
    }

    public static Line[] arrayFromPoints(Point[] points) {
        Line[] result = new Line[points.length-1];
        for(int i = 0; i < points.length-1; i++) {
            result[i] = new Line(points[i], points[i+1]);
        }
        return result;
    }

    @Override
    public boolean equals(Object l) {
        if(! (l instanceof  Line)) return false;
        Line k = (Line) l;
        return k.a.equals(a) && k.b.equals(b);
    }
}
