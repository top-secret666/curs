package triangulation.db;

import triangulation.model.Point;
import triangulation.model.Triangle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_FILE = "triangulation.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        initTables();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found. Add sqlite-jdbc.jar to classpath.", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
    }

    private void initTables() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS sessions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "point_count INTEGER NOT NULL DEFAULT 0,"
                    + "triangle_count INTEGER NOT NULL DEFAULT 0,"
                    + "created_at TEXT DEFAULT (datetime('now','localtime'))"
                    + ")");
            st.execute("CREATE TABLE IF NOT EXISTS points ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "session_id INTEGER NOT NULL,"
                    + "idx INTEGER NOT NULL,"
                    + "x REAL NOT NULL,"
                    + "y REAL NOT NULL,"
                    + "FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE"
                    + ")");
            st.execute("CREATE TABLE IF NOT EXISTS triangles ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "session_id INTEGER NOT NULL,"
                    + "idx INTEGER NOT NULL,"
                    + "p1x REAL, p1y REAL,"
                    + "p2x REAL, p2y REAL,"
                    + "p3x REAL, p3y REAL,"
                    + "FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE"
                    + ")");
        } catch (SQLException e) {
            System.err.println("DB init error: " + e.getMessage());
        }
    }

    public int saveSession(String name, List<Point> points, List<Triangle> triangles) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                int sessionId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sessions(name, point_count, triangle_count) VALUES(?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setInt(2, points.size());
                    ps.setInt(3, triangles.size());
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    sessionId = rs.getInt(1);
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO points(session_id,idx,x,y) VALUES(?,?,?,?)")) {
                    for (int i = 0; i < points.size(); i++) {
                        Point p = points.get(i);
                        ps.setInt(1, sessionId);
                        ps.setInt(2, i);
                        ps.setDouble(3, p.x);
                        ps.setDouble(4, p.y);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO triangles(session_id,idx,p1x,p1y,p2x,p2y,p3x,p3y) VALUES(?,?,?,?,?,?,?,?)")) {
                    for (int i = 0; i < triangles.size(); i++) {
                        Triangle t = triangles.get(i);
                        ps.setInt(1, sessionId);
                        ps.setInt(2, i);
                        ps.setDouble(3, t.p1.x);
                        ps.setDouble(4, t.p1.y);
                        ps.setDouble(5, t.p2.x);
                        ps.setDouble(6, t.p2.y);
                        ps.setDouble(7, t.p3.x);
                        ps.setDouble(8, t.p3.y);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
                return sessionId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<SessionInfo> getAllSessions() throws SQLException {
        List<SessionInfo> list = new ArrayList<>();
        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, name, point_count, triangle_count, created_at FROM sessions ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new SessionInfo(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("point_count"),
                        rs.getInt("triangle_count"),
                        rs.getString("created_at")));
            }
        }
        return list;
    }

    public List<Point> loadPoints(int sessionId) throws SQLException {
        List<Point> list = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT x, y FROM points WHERE session_id=? ORDER BY idx")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Point(rs.getDouble("x"), rs.getDouble("y")));
            }
        }
        return list;
    }

    public List<Triangle> loadTriangles(int sessionId) throws SQLException {
        List<Triangle> list = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT p1x,p1y,p2x,p2y,p3x,p3y FROM triangles WHERE session_id=? ORDER BY idx")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Triangle(
                            new Point(rs.getDouble("p1x"), rs.getDouble("p1y")),
                            new Point(rs.getDouble("p2x"), rs.getDouble("p2y")),
                            new Point(rs.getDouble("p3x"), rs.getDouble("p3y"))));
            }
        }
        return list;
    }

    public void deleteSession(int sessionId) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                exec(c, "DELETE FROM triangles WHERE session_id=?", sessionId);
                exec(c, "DELETE FROM points WHERE session_id=?", sessionId);
                exec(c, "DELETE FROM sessions WHERE id=?", sessionId);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void exec(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static class SessionInfo {
        public final int id;
        public final String name;
        public final int pointCount;
        public final int triangleCount;
        public final String createdAt;

        public SessionInfo(int id, String name, int pointCount, int triangleCount, String createdAt) {
            this.id = id;
            this.name = name;
            this.pointCount = pointCount;
            this.triangleCount = triangleCount;
            this.createdAt = createdAt;
        }
    }
}
