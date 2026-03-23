package triangulation.algorithm;

import triangulation.model.Point;
import triangulation.model.Triangle;

import java.util.*;

/**
 * Жадный алгоритм триангуляции:
 * 1. Генерируем все возможные рёбра между точками.
 * 2. Сортируем рёбра по длине (от короткого к длинному).
 * 3. Добавляем ребро, только если оно не пересекает уже добавленные.
 * 4. Из добавленных рёбер формируем треугольники.
 */
public class GreedyTriangulation {

    public static List<Triangle> triangulate(List<Point> points) {
        List<Triangle> triangles = new ArrayList<>();
        int n = points.size();
        if (n < 3) return triangles;

        List<Point> unique = new ArrayList<>();
        for (Point p : points) {
            boolean dup = false;
            for (Point u : unique) {
                if (Math.abs(p.x - u.x) < 1e-9 && Math.abs(p.y - u.y) < 1e-9) {
                    dup = true;
                    break;
                }
            }
            if (!dup) unique.add(p);
        }
        n = unique.size();
        if (n < 3) return triangles;

        List<int[]> allEdges = new ArrayList<>();
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                allEdges.add(new int[]{i, j});

        allEdges.sort((a, b) -> {
            double da = dist(unique.get(a[0]), unique.get(a[1]));
            double db = dist(unique.get(b[0]), unique.get(b[1]));
            return Double.compare(da, db);
        });

        List<int[]> accepted = new ArrayList<>();
        for (int[] edge : allEdges) {
            if (!intersectsAny(edge, accepted, unique))
                accepted.add(edge);
        }

        Set<String> edgeSet = new HashSet<>();
        for (int[] e : accepted)
            edgeSet.add(edgeKey(e[0], e[1]));

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (!edgeSet.contains(edgeKey(i, j))) continue;
                for (int k = j + 1; k < n; k++) {
                    if (!edgeSet.contains(edgeKey(i, k)) || !edgeSet.contains(edgeKey(j, k))) continue;
                    Point pi = unique.get(i);
                    Point pj = unique.get(j);
                    Point pk = unique.get(k);
                    boolean empty = true;
                    for (int m = 0; m < n; m++) {
                        if (m == i || m == j || m == k) continue;
                        if (pointInTriangle(unique.get(m), pi, pj, pk)) {
                            empty = false;
                            break;
                        }
                    }
                    if (empty) triangles.add(new Triangle(pi, pj, pk));
                }
            }
        }
        return triangles;
    }

    private static double dist(Point a, Point b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static String edgeKey(int i, int j) {
        return Math.min(i, j) + "," + Math.max(i, j);
    }

    private static boolean intersectsAny(int[] newEdge, List<int[]> accepted, List<Point> pts) {
        Point a = pts.get(newEdge[0]);
        Point b = pts.get(newEdge[1]);
        for (int[] e : accepted) {
            if (newEdge[0] == e[0] || newEdge[0] == e[1] ||
                newEdge[1] == e[0] || newEdge[1] == e[1]) continue;
            if (segmentsIntersect(a, b, pts.get(e[0]), pts.get(e[1]))) return true;
        }
        return false;
    }

    private static boolean segmentsIntersect(Point p1, Point p2, Point p3, Point p4) {
        double d1 = cross(p3, p4, p1);
        double d2 = cross(p3, p4, p2);
        double d3 = cross(p1, p2, p3);
        double d4 = cross(p1, p2, p4);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
               ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static boolean pointInTriangle(Point p, Point a, Point b, Point c) {
        double d1 = cross(a, b, p);
        double d2 = cross(b, c, p);
        double d3 = cross(c, a, p);
        boolean hasNeg = (d1 < -1e-9) || (d2 < -1e-9) || (d3 < -1e-9);
        boolean hasPos = (d1 >  1e-9) || (d2 >  1e-9) || (d3 >  1e-9);
        return !(hasNeg && hasPos)
               && Math.abs(d1) > 1e-9 && Math.abs(d2) > 1e-9 && Math.abs(d3) > 1e-9;
    }
}
