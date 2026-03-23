package triangulation;

import java.util.*;

/**
 * Жадный алгоритм триангуляции:
 * 1. Генерируем все возможные рёбра между точками.
 * 2. Сортируем рёбра по длине (от короткого к длинному).
 * 3. Добавляем ребро, только если оно не пересекает уже добавленные.
 * 4. Из добавленных рёбер формируем треугольники.
 */
public class GreedyTriangulation {

    public static List<TriangulationUI.Triangle> triangulate(List<TriangulationUI.Point> points) {
        List<TriangulationUI.Triangle> triangles = new ArrayList<>();
        int n = points.size();
        if (n < 3) return triangles;

        // Удаляем дубликаты
        List<TriangulationUI.Point> unique = new ArrayList<>();
        for (TriangulationUI.Point p : points) {
            boolean dup = false;
            for (TriangulationUI.Point u : unique) {
                if (Math.abs(p.x - u.x) < 1e-9 && Math.abs(p.y - u.y) < 1e-9) {
                    dup = true;
                    break;
                }
            }
            if (!dup) unique.add(p);
        }
        n = unique.size();
        if (n < 3) return triangles;

        // 1. Генерируем все рёбра
        List<int[]> allEdges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                allEdges.add(new int[]{i, j});
            }
        }

        // 2. Сортируем по длине
        allEdges.sort((a, b) -> {
            double da = dist(unique.get(a[0]), unique.get(a[1]));
            double db = dist(unique.get(b[0]), unique.get(b[1]));
            return Double.compare(da, db);
        });

        // 3. Жадно добавляем рёбра без пересечений
        List<int[]> accepted = new ArrayList<>();
        for (int[] edge : allEdges) {
            if (!intersectsAny(edge, accepted, unique)) {
                accepted.add(edge);
            }
        }

        // 4. Формируем треугольники из принятых рёбер.
        // Условие "правильного" треугольника триангуляции: все 3 ребра приняты,
        // И внутри треугольника нет ни одной другой точки из набора.
        Set<String> edgeSet = new HashSet<>();
        for (int[] e : accepted) {
            edgeSet.add(edgeKey(e[0], e[1]));
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (!edgeSet.contains(edgeKey(i, j))) continue;
                for (int k = j + 1; k < n; k++) {
                    if (!edgeSet.contains(edgeKey(i, k)) || !edgeSet.contains(edgeKey(j, k))) continue;
                    TriangulationUI.Point pi = unique.get(i);
                    TriangulationUI.Point pj = unique.get(j);
                    TriangulationUI.Point pk = unique.get(k);
                    // Проверяем, что внутри треугольника нет других точек
                    boolean empty = true;
                    for (int m = 0; m < n; m++) {
                        if (m == i || m == j || m == k) continue;
                        if (pointInTriangle(unique.get(m), pi, pj, pk)) {
                            empty = false;
                            break;
                        }
                    }
                    if (empty) {
                        triangles.add(new TriangulationUI.Triangle(pi, pj, pk));
                    }
                }
            }
        }

        return triangles;
    }

    /** Расстояние между двумя точками */
    private static double dist(TriangulationUI.Point a, TriangulationUI.Point b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Ключ ребра для HashSet */
    private static String edgeKey(int i, int j) {
        return Math.min(i, j) + "," + Math.max(i, j);
    }

    /** Проверяет, пересекает ли новое ребро хотя бы одно из уже принятых */
    private static boolean intersectsAny(int[] newEdge, List<int[]> accepted,
                                         List<TriangulationUI.Point> pts) {
        TriangulationUI.Point a = pts.get(newEdge[0]);
        TriangulationUI.Point b = pts.get(newEdge[1]);
        for (int[] e : accepted) {
            // Рёбра с общей вершиной не считаем пересекающимися
            if (newEdge[0] == e[0] || newEdge[0] == e[1] ||
                newEdge[1] == e[0] || newEdge[1] == e[1]) continue;
            TriangulationUI.Point c = pts.get(e[0]);
            TriangulationUI.Point d = pts.get(e[1]);
            if (segmentsIntersect(a, b, c, d)) return true;
        }
        return false;
    }

    /** Проверка пересечения двух отрезков (строго, без касания) */
    private static boolean segmentsIntersect(TriangulationUI.Point p1, TriangulationUI.Point p2,
                                              TriangulationUI.Point p3, TriangulationUI.Point p4) {
        double d1 = cross(p3, p4, p1);
        double d2 = cross(p3, p4, p2);
        double d3 = cross(p1, p2, p3);
        double d4 = cross(p1, p2, p4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return false;
    }

    /** Векторное произведение (AB x AC) */
    private static double cross(TriangulationUI.Point a, TriangulationUI.Point b,
                                TriangulationUI.Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    /**
     * Проверяет, находится ли точка p строго внутри треугольника (a, b, c).
     * Использует знаки барицентрических координат (метод знаков).
     */
    private static boolean pointInTriangle(TriangulationUI.Point p,
                                           TriangulationUI.Point a,
                                           TriangulationUI.Point b,
                                           TriangulationUI.Point c) {
        double d1 = cross(a, b, p);
        double d2 = cross(b, c, p);
        double d3 = cross(c, a, p);
        boolean hasNeg = (d1 < -1e-9) || (d2 < -1e-9) || (d3 < -1e-9);
        boolean hasPos = (d1 >  1e-9) || (d2 >  1e-9) || (d3 >  1e-9);
        // Строго внутри — все знаки одинаковы и нет нулей (точка не на ребре)
        return !(hasNeg && hasPos)
               && Math.abs(d1) > 1e-9 && Math.abs(d2) > 1e-9 && Math.abs(d3) > 1e-9;
    }
}
