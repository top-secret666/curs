package triangulation;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.application.Application;

import java.io.*;
import java.util.*;

public class TriangulationUI extends VBox {
    private TextArea inputArea;
    private TextArea outputArea;
    private Canvas canvas;
    private List<Point> points = new ArrayList<>();
    private List<Triangle> triangles = new ArrayList<>();

    public TriangulationUI(Stage stage) {
        setSpacing(10);
        setStyle("-fx-padding: 20;");

        Label inputLabel = new Label("Ввод данных:");
        inputArea = new TextArea();
        inputArea.setPromptText("Введите координаты точек (x y) по одной в строке");
        Button loadBtn = new Button("Загрузить из файла");
        loadBtn.setOnAction(e -> loadFromFile(stage));

        Button triangulateBtn = new Button("Построить триангуляцию");
        triangulateBtn.setOnAction(e -> triangulate());

        Label outputLabel = new Label("Результат:");
        outputArea = new TextArea();
        outputArea.setEditable(false);
        Button saveBtn = new Button("Сохранить в файл");
        saveBtn.setOnAction(e -> saveToFile(stage));

        canvas = new Canvas(600, 400);
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-border-color: #aaa; -fx-background-color: #fff;");

        getChildren().addAll(inputLabel, inputArea, loadBtn, triangulateBtn, outputLabel, outputArea, saveBtn, new Label("Визуализация:"), canvasPane);
    }

    private void loadFromFile(Stage stage) {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                inputArea.setText(br.lines().reduce("", (a, b) -> a + b + "\n"));
            } catch (IOException ex) {
                showError("Ошибка чтения файла");
            }
        }
    }

    private void saveToFile(Stage stage) {
        FileChooser fc = new FileChooser();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.print(outputArea.getText());
            } catch (IOException ex) {
                showError("Ошибка записи файла");
            }
        }
    }

    private void triangulate() {
        points.clear();
        triangles.clear();
        String[] lines = inputArea.getText().split("\n");
        for (String line : lines) {
            String[] parts = line.trim().split("\s+");
            if (parts.length == 2) {
                try {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    points.add(new Point(x, y));
                } catch (NumberFormatException ignored) {}
            }
        }
        // Жадный алгоритм (placeholder)
        if (points.size() >= 3) {
            triangles = GreedyTriangulation.triangulate(points);
            StringBuilder sb = new StringBuilder();
            for (Triangle t : triangles) {
                sb.append("Triangle: ").append(t).append("\n");
            }
            outputArea.setText(sb.toString());
            drawTriangles();
        } else {
            showError("Недостаточно точек для триангуляции");
        }
    }

    private void drawTriangles() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2);
        for (Triangle t : triangles) {
            gc.strokeLine(t.p1.x, t.p1.y, t.p2.x, t.p2.y);
            gc.strokeLine(t.p2.x, t.p2.y, t.p3.x, t.p3.y);
            gc.strokeLine(t.p3.x, t.p3.y, t.p1.x, t.p1.y);
        }
        gc.setFill(Color.RED);
        for (Point p : points) {
            gc.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.showAndWait();
    }

    static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        @Override
        public String toString() { return "(" + x + "," + y + ")"; }
    }

    static class Triangle {
        Point p1, p2, p3;
        Triangle(Point p1, Point p2, Point p3) {
            this.p1 = p1; this.p2 = p2; this.p3 = p3;
        }
        @Override
        public String toString() {
            return p1 + "-" + p2 + "-" + p3;
        }
    }
}
