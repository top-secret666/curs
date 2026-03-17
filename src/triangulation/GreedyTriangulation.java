package triangulation;

import java.util.*;

public class GreedyTriangulation {
    public static List<TriangulationUI.Triangle> triangulate(List<TriangulationUI.Point> points) {
        List<TriangulationUI.Triangle> triangles = new ArrayList<>();
        // Простейший жадный алгоритм: соединяем первые три точки, затем по очереди остальные
        if (points.size() < 3) return triangles;
        for (int i = 0; i < points.size() - 2; i++) {
            triangles.add(new TriangulationUI.Triangle(points.get(i), points.get(i+1), points.get(i+2)));
        }
        return triangles;
    }
}
