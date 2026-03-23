package triangulation;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TriangulationUI extends VBox {
    private TextArea inputArea;
    private TextArea outputArea;
    private Canvas canvas;
    private Label statusLabel;
    private Label validationLabel;
    private List<Point> points = new ArrayList<>();
    private List<Triangle> triangles = new ArrayList<>();

    public TriangulationUI(Stage stage) {
        setSpacing(16);
        setPadding(new Insets(20, 24, 20, 24));

        // ===== Заголовок =====
        Label title = new Label("\u25B2 Триангуляция — Жадный алгоритм");
        title.getStyleClass().add("title-label");

        // ===== Левая часть: ввод =====
        Label inputLabel = new Label("Ввод точек (x y):");
        inputLabel.getStyleClass().add("section-label");

        inputArea = new TextArea();
        inputArea.setPromptText("Введите координаты точек:\n100 100\n200 100\n150 200\n...");
        inputArea.setPrefRowCount(8);
        inputArea.setWrapText(false);

        // Валидация в реальном времени
        validationLabel = new Label("");
        validationLabel.getStyleClass().add("error-text");
        validationLabel.setWrapText(true);
        inputArea.textProperty().addListener((obs, oldVal, newVal) -> validateInput(newVal));

        HBox inputButtons = new HBox(10);
        inputButtons.setAlignment(Pos.CENTER_LEFT);
        Button loadBtn = new Button("\uD83D\uDCC2 Загрузить из файла");
        loadBtn.setOnAction(e -> loadFromFile(stage));
        Button clearBtn = new Button("\u2715 Очистить");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setOnAction(e -> {
            inputArea.clear();
            outputArea.clear();
            points.clear();
            triangles.clear();
            clearCanvas();
            updateStatus("Готово к вводу данных", false);
        });
        inputButtons.getChildren().addAll(loadBtn, clearBtn);

        VBox inputCard = new VBox(10, inputLabel, inputArea, validationLabel, inputButtons);
        inputCard.getStyleClass().add("card");

        // ===== Кнопка триангуляции =====
        Button triangulateBtn = new Button("\u25B6 Построить триангуляцию");
        triangulateBtn.getStyleClass().add("btn-primary");
        triangulateBtn.setMaxWidth(Double.MAX_VALUE);
        triangulateBtn.setOnAction(e -> triangulate());

        // ===== Правая часть: результат =====
        Label outputLabel = new Label("Результат:");
        outputLabel.getStyleClass().add("section-label");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(6);
        outputArea.setPromptText("Здесь появятся результаты триангуляции...");

        HBox outputButtons = new HBox(10);
        outputButtons.setAlignment(Pos.CENTER_LEFT);
        Button saveBtn = new Button("\uD83D\uDCBE Сохранить в файл");
        saveBtn.getStyleClass().add("btn-secondary");
        saveBtn.setOnAction(e -> saveToFile(stage));
        outputButtons.getChildren().add(saveBtn);

        VBox outputCard = new VBox(10, outputLabel, outputArea, outputButtons);
        outputCard.getStyleClass().add("card");

        // ===== Визуализация =====
        Label vizLabel = new Label("Визуализация:");
        vizLabel.getStyleClass().add("section-label");

        canvas = new Canvas(700, 420);
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.getStyleClass().add("canvas-pane");
        canvasPane.setPadding(new Insets(8));
        clearCanvas();

        VBox vizCard = new VBox(10, vizLabel, canvasPane);
        vizCard.getStyleClass().add("card");

        // ===== Статус-бар =====
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("Готово к вводу данных");
        statusLabel.getStyleClass().add("status-label");
        statusBar.getChildren().add(statusLabel);

        getChildren().addAll(title, inputCard, triangulateBtn, outputCard, vizCard, statusBar);
        VBox.setVgrow(vizCard, Priority.ALWAYS);
    }

    // ===== Валидация ввода в реальном времени =====
    private void validateInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            validationLabel.setText("");
            inputArea.getStyleClass().removeAll("validation-error", "validation-ok");
            return;
        }

        List<String> errors = new ArrayList<>();
        String[] lines = text.split("\n");
        int validCount = 0;
        Set<String> seen = new HashSet<>();
        int lineNum = 0;

        for (String line : lines) {
            lineNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Заменяем запятую на точку, табы на пробелы
            trimmed = trimmed.replace(',', '.').replace('\t', ' ');
            String[] parts = trimmed.split("\\s+");

            if (parts.length != 2) {
                errors.add("Строка " + lineNum + ": ожидается 2 числа (x y), найдено " + parts.length);
                continue;
            }

            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                if (Double.isNaN(x) || Double.isInfinite(x) ||
                    Double.isNaN(y) || Double.isInfinite(y)) {
                    errors.add("Строка " + lineNum + ": недопустимое значение (NaN/Infinity)");
                    continue;
                }

                if (x < -10000 || x > 10000 || y < -10000 || y > 10000) {
                    errors.add("Строка " + lineNum + ": координаты вне диапазона [-10000, 10000]");
                    continue;
                }

                String key = Math.round(x * 100) + ";" + Math.round(y * 100);
                if (seen.contains(key)) {
                    errors.add("Строка " + lineNum + ": дубликат точки (" + x + ", " + y + ")");
                    continue;
                }
                seen.add(key);
                validCount++;
            } catch (NumberFormatException e) {
                errors.add("Строка " + lineNum + ": \"" + trimmed + "\" — не числа");
            }
        }

        inputArea.getStyleClass().removeAll("validation-error", "validation-ok");

        if (!errors.isEmpty()) {
            String msg = String.join("\n", errors);
            if (errors.size() > 3) {
                msg = String.join("\n", errors.subList(0, 3)) + "\n...ещё " + (errors.size() - 3) + " ошибок";
            }
            validationLabel.setText("\u26A0 " + msg);
            inputArea.getStyleClass().add("validation-error");
        } else if (validCount < 3) {
            validationLabel.setText("\u26A0 Нужно минимум 3 точки для триангуляции (сейчас: " + validCount + ")");
            inputArea.getStyleClass().add("validation-error");
        } else {
            validationLabel.setText("\u2713 " + validCount + " точек готово к триангуляции");
            validationLabel.setStyle("-fx-text-fill: #27ae60;");
            inputArea.getStyleClass().add("validation-ok");
        }
    }

    private void loadFromFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Открыть файл с точками");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                inputArea.setText(sb.toString().trim());
                updateStatus("Файл загружен: " + file.getName(), false);
            } catch (IOException ex) {
                showError("Ошибка чтения файла: " + ex.getMessage());
            }
        }
    }

    private void saveToFile(Stage stage) {
        if (outputArea.getText().isEmpty()) {
            showError("Нет данных для сохранения. Сначала постройте триангуляцию.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить результат");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                pw.print(outputArea.getText());
                updateStatus("Результат сохранён: " + file.getName(), false);
            } catch (IOException ex) {
                showError("Ошибка записи файла: " + ex.getMessage());
            }
        }
    }

    private void triangulate() {
        points.clear();
        triangles.clear();

        // Парсинг с валидацией
        List<String> errors = new ArrayList<>();
        String[] lines = inputArea.getText().split("\n");
        int lineNum = 0;

        for (String line : lines) {
            lineNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            trimmed = trimmed.replace(',', '.').replace('\t', ' ');
            String[] parts = trimmed.split("\\s+");

            if (parts.length != 2) {
                errors.add("Строка " + lineNum + ": ожидается 2 числа, получено " + parts.length);
                continue;
            }

            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                if (Double.isNaN(x) || Double.isInfinite(x) ||
                    Double.isNaN(y) || Double.isInfinite(y)) {
                    errors.add("Строка " + lineNum + ": недопустимое значение");
                    continue;
                }

                points.add(new Point(x, y));
            } catch (NumberFormatException e) {
                errors.add("Строка " + lineNum + ": \"" + trimmed + "\" — не числа");
            }
        }

        if (!errors.isEmpty()) {
            showError("Ошибки ввода:\n" + String.join("\n", errors));
            return;
        }

        if (points.size() < 3) {
            showError("Нужно минимум 3 точки для триангуляции.\nСейчас введено: " + points.size());
            return;
        }

        // Проверка коллинеарности
        if (areAllCollinear(points)) {
            showError("Все точки лежат на одной прямой — триангуляция невозможна.");
            return;
        }

        long startTime = System.nanoTime();
        triangles = GreedyTriangulation.triangulate(points);
        long elapsed = (System.nanoTime() - startTime) / 1_000_000;

        StringBuilder sb = new StringBuilder();
        sb.append("=== Результат жадной триангуляции ===\n");
        sb.append("Точек: ").append(points.size()).append("\n");
        sb.append("Треугольников: ").append(triangles.size()).append("\n");
        sb.append("Время: ").append(elapsed).append(" мс\n");
        sb.append("=====================================\n\n");
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t = triangles.get(i);
            sb.append("△ ").append(i + 1).append(": ").append(t).append("\n");
        }
        outputArea.setText(sb.toString());
        drawTriangles();
        updateStatus("Триангуляция: " + triangles.size() + " треугольников из "
                + points.size() + " точек за " + elapsed + " мс", false);
    }

    private boolean areAllCollinear(List<Point> pts) {
        if (pts.size() < 3) return true;
        Point a = pts.get(0), b = pts.get(1);
        for (int i = 2; i < pts.size(); i++) {
            Point c = pts.get(i);
            double cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
            if (Math.abs(cross) > 1e-6) return false;
        }
        return true;
    }

    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#f8f9fa"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Сетка
        gc.setStroke(Color.web("#e9ecef"));
        gc.setLineWidth(0.5);
        for (double x = 0; x < canvas.getWidth(); x += 50) {
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }
        for (double y = 0; y < canvas.getHeight(); y += 50) {
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }

        gc.setFill(Color.web("#adb5bd"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Здесь будет визуализация", canvas.getWidth() / 2 - 80, canvas.getHeight() / 2);
    }

    private void drawTriangles() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#f8f9fa"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (points.isEmpty()) return;

        // Вычисляем bounding box и масштаб для авто-подгонки
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        double dataW = maxX - minX;
        double dataH = maxY - minY;
        if (dataW < 1) dataW = 1;
        if (dataH < 1) dataH = 1;

        double pad = 40;
        double scaleX = (canvas.getWidth() - 2 * pad) / dataW;
        double scaleY = (canvas.getHeight() - 2 * pad) / dataH;
        double scale = Math.min(scaleX, scaleY);
        double offX = pad + ((canvas.getWidth() - 2 * pad) - dataW * scale) / 2;
        double offY = pad + ((canvas.getHeight() - 2 * pad) - dataH * scale) / 2;

        // Сетка
        gc.setStroke(Color.web("#e9ecef"));
        gc.setLineWidth(0.5);
        for (double x = 0; x < canvas.getWidth(); x += 50) {
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }
        for (double y = 0; y < canvas.getHeight(); y += 50) {
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }

        // Заливка треугольников (полупрозрачная)
        Color[] fills = {
            Color.web("#3498db", 0.12), Color.web("#2ecc71", 0.12),
            Color.web("#e74c3c", 0.12), Color.web("#9b59b6", 0.12),
            Color.web("#f39c12", 0.12), Color.web("#1abc9c", 0.12)
        };
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t = triangles.get(i);
            double[] xs = {
                (t.p1.x - minX) * scale + offX,
                (t.p2.x - minX) * scale + offX,
                (t.p3.x - minX) * scale + offX
            };
            double[] ys = {
                (t.p1.y - minY) * scale + offY,
                (t.p2.y - minY) * scale + offY,
                (t.p3.y - minY) * scale + offY
            };
            gc.setFill(fills[i % fills.length]);
            gc.fillPolygon(xs, ys, 3);
        }

        // Рёбра
        gc.setStroke(Color.web("#2c3e50"));
        gc.setLineWidth(2);
        for (Triangle t : triangles) {
            double x1 = (t.p1.x - minX) * scale + offX;
            double y1 = (t.p1.y - minY) * scale + offY;
            double x2 = (t.p2.x - minX) * scale + offX;
            double y2 = (t.p2.y - minY) * scale + offY;
            double x3 = (t.p3.x - minX) * scale + offX;
            double y3 = (t.p3.y - minY) * scale + offY;
            gc.strokeLine(x1, y1, x2, y2);
            gc.strokeLine(x2, y2, x3, y3);
            gc.strokeLine(x3, y3, x1, y1);
        }

        // Точки
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            double sx = (p.x - minX) * scale + offX;
            double sy = (p.y - minY) * scale + offY;

            // Тень
            gc.setFill(Color.web("#000000", 0.15));
            gc.fillOval(sx - 6, sy - 5, 12, 12);
            // Точка
            gc.setFill(Color.web("#e74c3c"));
            gc.fillOval(sx - 5, sy - 5, 10, 10);
            // Белая обводка
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(sx - 5, sy - 5, 10, 10);

            // Подпись номера
            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(String.valueOf(i + 1), sx + 7, sy - 7);
        }

        // Легенда
        gc.setFill(Color.web("#2c3e50"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Точек: " + points.size() + "  |  Треугольников: " + triangles.size(), 10, canvas.getHeight() - 8);
    }

    private void updateStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #ecf0f1;");
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        updateStatus("Ошибка: " + msg.split("\n")[0], true);
    }

    static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        @Override
        public String toString() { return String.format("(%.1f, %.1f)", x, y); }
    }

    static class Triangle {
        Point p1, p2, p3;
        Triangle(Point p1, Point p2, Point p3) {
            this.p1 = p1; this.p2 = p2; this.p3 = p3;
        }
        @Override
        public String toString() {
            return p1 + " — " + p2 + " — " + p3;
        }
    }
}
