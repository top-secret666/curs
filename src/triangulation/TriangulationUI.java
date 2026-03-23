package triangulation;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TriangulationUI extends HBox {

    // ─── данные ───────────────────────────────────────────────────────────────
    private final List<Point>    points    = new ArrayList<>();
    private final List<Triangle> triangles = new ArrayList<>();

    // ─── состояние ────────────────────────────────────────────────────────────
    private boolean mouseMode  = true;   // включён по умолчанию
    private boolean autoTriang = true;
    private double  mapMinX, mapMinY, mapScale, mapOffX, mapOffY;
    private boolean mapReady   = false;

    // ─── UI-компоненты ────────────────────────────────────────────────────────
    private TextArea     inputArea;
    private TextArea     outputArea;
    private Canvas       canvas;
    private Label        statusLabel;
    private Label        validationLabel;
    private Label        coordLabel;
    private Label        pointCountLabel;
    private ToggleButton mouseModeBtn;

    // ──────────────────────────────────────────────────────────────────────────
    public TriangulationUI(Stage stage) {
        setSpacing(0);
        setPadding(Insets.EMPTY);

        VBox leftPanel  = buildLeftPanel(stage);
        VBox rightPanel = buildRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        getChildren().addAll(leftPanel, rightPanel);
    }

    // ═══════════════════════════ ЛЕВАЯ ПАНЕЛЬ ═════════════════════════════════
    private VBox buildLeftPanel(Stage stage) {
        VBox root = new VBox(0);
        root.setPrefWidth(306);
        root.setMinWidth(260);
        root.setMaxWidth(360);
        root.getStyleClass().add("left-panel");

        // ── Шапка ─────────────────────────────────────────────────────────────
        VBox header = new VBox(2);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(22, 20, 18, 20));
        Label icon  = new Label("△");
        icon.getStyleClass().add("app-icon");
        Label title = new Label("Триангуляция");
        title.getStyleClass().add("title-label");
        Label sub   = new Label("Жадный алгоритм");
        sub.getStyleClass().add("subtitle-label");
        header.getChildren().addAll(icon, title, sub);

        // ── Скроллируемый контент ──────────────────────────────────────────
        VBox content = new VBox(12);
        content.setPadding(new Insets(14, 14, 20, 14));

        // — Режим рисования —
        mouseModeBtn = new ToggleButton("✏  Режим рисования  ·  АКТИВЕН");
        mouseModeBtn.setSelected(true);
        mouseModeBtn.setMaxWidth(Double.MAX_VALUE);
        mouseModeBtn.getStyleClass().add("mode-toggle");
        mouseModeBtn.selectedProperty().addListener((obs, o, n) -> {
            mouseMode = n;
            mouseModeBtn.setText(n ? "✏  Режим рисования  ·  АКТИВЕН"
                                   : "✏  Режим рисования  ·  выключен");
            if (!mouseMode) redrawFromInput();
            else redraw();
        });

        CheckBox autoCheck = new CheckBox("Авто-триангуляция при добавлении");
        autoCheck.setSelected(true);
        autoCheck.getStyleClass().add("auto-check");
        autoCheck.selectedProperty().addListener((obs, o, n) -> autoTriang = n);

        Label drawHint = new Label("ЛКМ — поставить точку   ·   ПКМ — убрать последнюю");
        drawHint.getStyleClass().add("hint-label");
        drawHint.setWrapText(true);

        VBox modeCard = new VBox(8, mouseModeBtn, autoCheck, drawHint);
        modeCard.getStyleClass().add("card");

        // ── Ввод координат ────────────────────────────────────────────────────
        Label inputLabel = new Label("КООРДИНАТЫ  ( x  y )");
        inputLabel.getStyleClass().add("section-label");

        inputArea = new TextArea();
        inputArea.setPromptText("100 100\n200 100\n150 200\n...");
        inputArea.setPrefRowCount(7);
        inputArea.setWrapText(false);
        inputArea.textProperty().addListener((obs, o, n) -> validateInput(n));

        validationLabel = new Label("");
        validationLabel.getStyleClass().add("error-text");
        validationLabel.setWrapText(true);

        Button loadBtn  = new Button("📂  Загрузить файл");
        Button clearBtn = new Button("✕  Очистить");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.getStyleClass().add("btn-secondary");
        clearBtn.getStyleClass().add("btn-danger");
        HBox.setHgrow(loadBtn,  Priority.ALWAYS);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        loadBtn.setOnAction(e -> loadFromFile(stage));
        clearBtn.setOnAction(e -> clearAll());
        HBox fileRow = new HBox(8, loadBtn, clearBtn);

        Button undoBtn = new Button("↩  Удалить последнюю точку");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.getStyleClass().add("btn-outline");
        undoBtn.setOnAction(e -> removeLastPoint());

        pointCountLabel = new Label("Точек на холсте: 0");
        pointCountLabel.getStyleClass().add("point-count-label");

        VBox inputCard = new VBox(8, inputLabel, inputArea, validationLabel, fileRow, undoBtn, pointCountLabel);
        inputCard.getStyleClass().add("card");

        // ── Действия ──────────────────────────────────────────────────────────
        Button triangulateBtn = new Button("▶   Построить триангуляцию");
        triangulateBtn.getStyleClass().add("btn-primary");
        triangulateBtn.setMaxWidth(Double.MAX_VALUE);
        triangulateBtn.setOnAction(e -> triangulate());

        Button randomBtn = new Button("⚄   Случайные точки");
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.getStyleClass().add("btn-outline");
        randomBtn.setOnAction(e -> generateRandom());

        // ── Результат ─────────────────────────────────────────────────────────
        Label outputLabel = new Label("РЕЗУЛЬТАТ");
        outputLabel.getStyleClass().add("section-label");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(5);
        outputArea.setPromptText("Здесь появятся треугольники...");

        Button saveBtn = new Button("💾  Сохранить результат");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().add("btn-secondary");
        saveBtn.setOnAction(e -> saveToFile(stage));

        VBox outputCard = new VBox(8, outputLabel, outputArea, saveBtn);
        outputCard.getStyleClass().add("card");

        content.getChildren().addAll(modeCard, inputCard, triangulateBtn, randomBtn, outputCard);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("left-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    // ─── Случайные точки ──────────────────────────────────────────────────────
    private void generateRandom() {
        Random rnd = new Random();
        int count = 10 + rnd.nextInt(11);
        StringBuilder sb = new StringBuilder(inputArea.getText().trim());
        for (int i = 0; i < count; i++) {
            int x = 50 + rnd.nextInt(451);
            int y = 50 + rnd.nextInt(451);
            if (sb.length() > 0) sb.append("\n");
            sb.append(x).append(" ").append(y);
            points.add(new Point(x, y));
        }
        inputArea.setText(sb.toString());
        updatePointCount();
        if (points.size() >= 3 && !areAllCollinear(points)) triangulateInternal();
        redraw();
        updateStatus("Добавлено " + count + " случайных точек  ·  Всего: " + points.size(), false);
    }

    private void updatePointCount() {
        if (pointCountLabel != null)
            pointCountLabel.setText("Точек на холсте: " + points.size());
    }

    // ═══════════════════════════ ПРАВАЯ ПАНЕЛЬ ════════════════════════════════
    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("right-panel");

        // ── Тулбар ────────────────────────────────────────────────────────────
        Label canvasTitle = new Label("Визуализация");
        canvasTitle.getStyleClass().add("canvas-title");

        coordLabel = new Label("—");
        coordLabel.getStyleClass().add("coord-label");

        Button undoToolBtn  = new Button("↩  Отмена");
        Button clearToolBtn = new Button("✕  Очистить");
        undoToolBtn.getStyleClass().addAll("toolbar-btn");
        clearToolBtn.getStyleClass().addAll("toolbar-btn", "toolbar-btn-danger");
        undoToolBtn.setOnAction(e -> removeLastPoint());
        clearToolBtn.setOnAction(e -> clearAll());

        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("canvas-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox tleft  = new HBox(canvasTitle);
        tleft.setAlignment(Pos.CENTER_LEFT);
        HBox tright = new HBox(10, coordLabel, undoToolBtn, clearToolBtn);
        tright.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(tleft,  Priority.ALWAYS);
        HBox.setHgrow(tright, Priority.ALWAYS);
        toolbar.getChildren().addAll(tleft, tright);

        // ── Canvas ────────────────────────────────────────────────────────────
        Pane canvasHolder = new Pane();
        canvasHolder.getStyleClass().add("canvas-holder");
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        canvas = new Canvas();
        canvas.widthProperty() .bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvas.widthProperty() .addListener(e -> redraw());
        canvas.heightProperty().addListener(e -> redraw());
        canvasHolder.getChildren().add(canvas);

        // ── Обработчики мыши ──────────────────────────────────────────────────
        canvas.setOnMouseClicked(ev -> {
            if (!mouseMode) return;
            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                addMousePoint(ev.getX(), ev.getY());
            } else if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                removeLastPoint();
            }
        });

        canvas.setOnMouseMoved(ev -> {
            if (mapReady && mapScale > 0) {
                double dataX = (ev.getX() - mapOffX) / mapScale + mapMinX;
                double dataY = (ev.getY() - mapOffY) / mapScale + mapMinY;
                coordLabel.setText(String.format("x: %.0f   y: %.0f", dataX, dataY));
            } else {
                coordLabel.setText(String.format("x: %.0f   y: %.0f", ev.getX(), ev.getY()));
            }
            if (mouseMode) drawCrosshair(ev.getX(), ev.getY());
        });

        canvas.setOnMouseExited(ev -> {
            coordLabel.setText("—");
            redraw();
        });

        // ── Статус-бар ────────────────────────────────────────────────────────
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("Готово  ·  кликните по холсту для добавления точек");
        statusLabel.getStyleClass().add("status-label");
        statusBar.getChildren().add(statusLabel);

        panel.getChildren().addAll(toolbar, canvasHolder, statusBar);

        // Первая отрисовка после layout
        canvasHolder.layoutBoundsProperty().addListener(
            (obs, o, n) -> { if (n.getWidth() > 0) redraw(); });

        return panel;
    }

    // ──────────────────────────── МЫШЬ ────────────────────────────────────────
    private void addMousePoint(double screenX, double screenY) {
        // Перевод экранных coords в data-coords
        double dataX, dataY;
        if (mapReady && mapScale > 0) {
            dataX = (screenX - mapOffX) / mapScale + mapMinX;
            dataY = (screenY - mapOffY) / mapScale + mapMinY;
        } else {
            dataX = screenX;
            dataY = screenY;
        }

        // Добавляем в поле ввода
        String existing = inputArea.getText().trim();
        String newLine  = String.format("%.0f %.0f", dataX, dataY);
        inputArea.setText(existing.isEmpty() ? newLine : existing + "\n" + newLine);

        points.add(new Point(dataX, dataY));
        updatePointCount();
        redraw();

        if (autoTriang && points.size() >= 3 && !areAllCollinear(points)) {
            triangulateInternal();
        }

        updateStatus("Точка (" + (int)dataX + ", " + (int)dataY
                + ")  ·  Всего: " + points.size(), false);
    }

    private void removeLastPoint() {
        if (points.isEmpty()) return;
        points.remove(points.size() - 1);
        String text = inputArea.getText().trim();
        int lastNl = text.lastIndexOf('\n');
        inputArea.setText(lastNl >= 0 ? text.substring(0, lastNl) : "");
        triangles.clear();
        if (autoTriang && points.size() >= 3 && !areAllCollinear(points)) {
            triangulateInternal();
        }
        updatePointCount();
        redraw();
        updateStatus("Точка удалена  ·  Осталось: " + points.size(), false);
    }

    private void clearAll() {
        inputArea.clear();
        outputArea.clear();
        points.clear();
        triangles.clear();
        mapReady = false;
        updatePointCount();
        redraw();
        updateStatus("Холст очищен", false);
    }

    // ──────────────────────────── ВАЛИДАЦИЯ ───────────────────────────────────
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
            String trimmed = line.trim().replace(',', '.').replace('\t', ' ');
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] parts = trimmed.split("\\s+");
            if (parts.length != 2) {
                errors.add("Стр. " + lineNum + ": нужно 2 числа, найдено " + parts.length);
                continue;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    errors.add("Стр. " + lineNum + ": NaN/Infinity");
                    continue;
                }
                if (x < -10000 || x > 10000 || y < -10000 || y > 10000) {
                    errors.add("Стр. " + lineNum + ": вне диапазона [-10000, 10000]");
                    continue;
                }
                String key = Math.round(x * 100) + ";" + Math.round(y * 100);
                if (seen.contains(key)) {
                    errors.add("Стр. " + lineNum + ": дубликат (" + (int)x + ", " + (int)y + ")");
                    continue;
                }
                seen.add(key);
                validCount++;
            } catch (NumberFormatException e) {
                errors.add("Стр. " + lineNum + ": не числа");
            }
        }
        inputArea.getStyleClass().removeAll("validation-error", "validation-ok");
        if (!errors.isEmpty()) {
            validationLabel.getStyleClass().remove("ok-text");
            validationLabel.getStyleClass().add("error-text");
            String msg = String.join("\n", errors.subList(0, Math.min(3, errors.size())));
            if (errors.size() > 3) msg += "\n...ещё " + (errors.size() - 3);
            validationLabel.setText("⚠ " + msg);
            inputArea.getStyleClass().add("validation-error");
        } else if (validCount < 3) {
            validationLabel.getStyleClass().remove("ok-text");
            validationLabel.getStyleClass().add("error-text");
            validationLabel.setText("⚠ Нужно минимум 3 точки (сейчас: " + validCount + ")");
            inputArea.getStyleClass().add("validation-error");
        } else {
            validationLabel.getStyleClass().remove("error-text");
            validationLabel.getStyleClass().add("ok-text");
            validationLabel.setText("✓ " + validCount + " точек готово");
            inputArea.getStyleClass().add("validation-ok");
        }
    }

    // ──────────────────────────── ФАЙЛЫ ───────────────────────────────────────
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
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                inputArea.setText(sb.toString().trim());
                updateStatus("Файл загружен: " + file.getName(), false);
            } catch (IOException ex) {
                showError("Ошибка чтения файла: " + ex.getMessage());
            }
        }
    }

    private void saveToFile(Stage stage) {
        if (outputArea.getText().isEmpty()) {
            showError("Нет данных. Сначала постройте триангуляцию.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить результат");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                pw.print(outputArea.getText());
                updateStatus("Сохранено: " + file.getName(), false);
            } catch (IOException ex) {
                showError("Ошибка записи: " + ex.getMessage());
            }
        }
    }

    // ──────────────────────────── ТРИАНГУЛЯЦИЯ ────────────────────────────────
    /** Пересчёт из текстового поля (не мышь) */
    private void redrawFromInput() {
        points.clear();
        triangles.clear();
        parsePointsFromText();
        updatePointCount();
        redraw();
    }

    private void parsePointsFromText() {
        String[] lines = inputArea.getText().split("\n");
        for (String line : lines) {
            String t = line.trim().replace(',', '.').replace('\t', ' ');
            if (t.isEmpty() || t.startsWith("#")) continue;
            String[] parts = t.split("\\s+");
            if (parts.length != 2) continue;
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                if (!Double.isNaN(x) && !Double.isInfinite(x)
                        && !Double.isNaN(y) && !Double.isInfinite(y))
                    points.add(new Point(x, y));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void triangulate() {
        if (!mouseMode) {
            points.clear();
            triangles.clear();
            List<String> errors = new ArrayList<>();
            String[] lines = inputArea.getText().split("\n");
            int lineNum = 0;
            for (String line : lines) {
                lineNum++;
                String t = line.trim().replace(',', '.').replace('\t', ' ');
                if (t.isEmpty() || t.startsWith("#")) continue;
                String[] parts = t.split("\\s+");
                if (parts.length != 2) { errors.add("Стр. " + lineNum + ": нужно 2 числа"); continue; }
                try {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                        errors.add("Стр. " + lineNum + ": NaN/Infinity"); continue;
                    }
                    points.add(new Point(x, y));
                } catch (NumberFormatException e) { errors.add("Стр. " + lineNum + ": не числа"); }
            }
            if (!errors.isEmpty()) { showError("Ошибки:\n" + String.join("\n", errors)); return; }
        }

        if (points.size() < 3) { showError("Нужно минимум 3 точки.\nСейчас: " + points.size()); return; }
        if (areAllCollinear(points)) { showError("Все точки на одной прямой — триангуляция невозможна."); return; }

        triangulateInternal();
    }

    private void triangulateInternal() {
        long t0 = System.nanoTime();
        triangles.clear();
        triangles.addAll(GreedyTriangulation.triangulate(points));
        long ms = (System.nanoTime() - t0) / 1_000_000;

        StringBuilder sb = new StringBuilder();
        sb.append("=== Жадная триангуляция ===\n");
        sb.append("Точек:        ").append(points.size()).append("\n");
        sb.append("Треугольников:").append(triangles.size()).append("\n");
        sb.append("Время:        ").append(ms).append(" мс\n");
        sb.append("───────────────────────────\n");
        for (int i = 0; i < triangles.size(); i++)
            sb.append("△ ").append(i + 1).append(": ").append(triangles.get(i)).append("\n");
        outputArea.setText(sb.toString());
        redraw();
        updateStatus("Триангуляция: " + triangles.size() + " △ из "
                + points.size() + " точек за " + ms + " мс", false);
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

    // ──────────────────────────── РИСОВАНИЕ ───────────────────────────────────
    private void redraw() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;
        if (points.isEmpty()) { drawEmpty(); return; }
        drawScene(null, -1, -1);
    }

    private void drawCrosshair(double cx, double cy) {
        if (canvas.getWidth() <= 0) return;
        drawScene(new double[]{cx, cy}, cx, cy);
    }

    private void drawScene(double[] crosshair, double cx, double cy) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth(), H = canvas.getHeight();

        // Фон
        gc.setFill(Color.web("#f0f4f8"));
        gc.fillRect(0, 0, W, H);

        // Мелкая сетка
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(1.0);
        for (double x = 0; x < W; x += 40) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 40) gc.strokeLine(0, y, W, y);
        // Крупная сетка
        gc.setStroke(Color.web("#cbd5e1"));
        gc.setLineWidth(1.0);
        for (double x = 0; x < W; x += 200) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 200) gc.strokeLine(0, y, W, y);

        if (points.isEmpty()) return;

        // ── Масштаб ──────────────────────────────────────────────────────────
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : points) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        double dW = Math.max(maxX - minX, 1);
        double dH = Math.max(maxY - minY, 1);
        double pad    = 55;
        double scaleX = (W - 2 * pad) / dW;
        double scaleY = (H - 2 * pad) / dH;
        double scale  = Math.min(scaleX, scaleY);
        double offX   = pad + ((W - 2 * pad) - dW * scale) / 2;
        double offY   = pad + ((H - 2 * pad) - dH * scale) / 2;

        mapMinX = minX; mapMinY = minY;
        mapScale = scale; mapOffX = offX; mapOffY = offY;
        mapReady = true;

        // ── Заливка треугольников ─────────────────────────────────────────────
        Color[] palFill = {
            Color.web("#3b82f6", 0.18), Color.web("#10b981", 0.18),
            Color.web("#8b5cf6", 0.18), Color.web("#f59e0b", 0.18),
            Color.web("#ef4444", 0.18), Color.web("#06b6d4", 0.18),
            Color.web("#ec4899", 0.18), Color.web("#14b8a6", 0.18)
        };
        Color[] palEdge = {
            Color.web("#2563eb"), Color.web("#059669"),
            Color.web("#7c3aed"), Color.web("#d97706"),
            Color.web("#dc2626"), Color.web("#0891b2"),
            Color.web("#db2777"), Color.web("#0f766e")
        };
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t = triangles.get(i);
            double[] xs = { sx(t.p1, minX, scale, offX), sx(t.p2, minX, scale, offX), sx(t.p3, minX, scale, offX) };
            double[] ys = { sy(t.p1, minY, scale, offY), sy(t.p2, minY, scale, offY), sy(t.p3, minY, scale, offY) };
            gc.setFill(palFill[i % palFill.length]);
            gc.fillPolygon(xs, ys, 3);
        }

        // ── Рёбра ────────────────────────────────────────────────────────────
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t = triangles.get(i);
            double x1 = sx(t.p1, minX, scale, offX), y1 = sy(t.p1, minY, scale, offY);
            double x2 = sx(t.p2, minX, scale, offX), y2 = sy(t.p2, minY, scale, offY);
            double x3 = sx(t.p3, minX, scale, offX), y3 = sy(t.p3, minY, scale, offY);
            gc.setStroke(palEdge[i % palEdge.length].deriveColor(0, 1, 1, 0.80));
            gc.setLineWidth(1.8);
            gc.strokeLine(x1, y1, x2, y2);
            gc.strokeLine(x2, y2, x3, y3);
            gc.strokeLine(x3, y3, x1, y1);
        }

        // ── Точки ────────────────────────────────────────────────────────────
        for (int i = 0; i < points.size(); i++) {
            Point  p  = points.get(i);
            double px = sx(p, minX, scale, offX);
            double py = sy(p, minY, scale, offY);
            // Тень
            gc.setFill(Color.web("#000000", 0.10));
            gc.fillOval(px - 7, py - 5, 14, 14);
            // Заливка
            gc.setFill(Color.web("#ef4444"));
            gc.fillOval(px - 6, py - 6, 12, 12);
            // Блик
            gc.setFill(Color.web("#ffffff", 0.50));
            gc.fillOval(px - 4, py - 5, 5, 4);
            // Обводка
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(px - 6, py - 6, 12, 12);
            // Номер
            gc.setFill(Color.web("#1e40af"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(String.valueOf(i + 1), px + 8, py - 4);
        }

        // ── Прицел мыши ──────────────────────────────────────────────────────
        if (crosshair != null && mouseMode) {
            gc.setStroke(Color.web("#3b82f6", 0.45));
            gc.setLineWidth(1);
            gc.setLineDashes(5, 5);
            gc.strokeLine(cx, 0, cx, H);
            gc.strokeLine(0, cy, W, cy);
            gc.setLineDashes();
            // Кольцо
            gc.setStroke(Color.web("#3b82f6", 0.75));
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - 8, cy - 8, 16, 16);
            // Центр
            gc.setFill(Color.web("#3b82f6", 0.55));
            gc.fillOval(cx - 3, cy - 3, 6, 6);
            // Tooltip с координатами у курсора
            if (mapReady && mapScale > 0) {
                double dx = (cx - mapOffX) / mapScale + mapMinX;
                double dy = (cy - mapOffY) / mapScale + mapMinY;
                String coordStr = String.format("(%.0f, %.0f)", dx, dy);
                double tw = coordStr.length() * 6.5;
                double tx = cx + 14, ty = cy - 10;
                if (tx + tw + 8 > W - 4) tx = cx - tw - 18;
                if (ty - 14 < 4) ty = cy + 22;
                gc.setFill(Color.web("#1e293b", 0.82));
                gc.fillRoundRect(tx - 4, ty - 14, tw + 8, 18, 5, 5);
                gc.setFill(Color.web("#f1f5f9"));
                gc.setFont(Font.font("Segoe UI", 11));
                gc.fillText(coordStr, tx, ty);
            }
        }

        // ── Подсказка (только пока мало точек) ────────────────────────────────
        if (mouseMode && points.size() < 3) {
            String tip = points.size() == 0 ? "ЛКМ — поставить точку   ·   ПКМ — убрать последнюю"
                                            : "Нужно ещё " + (3 - points.size()) + " точки для триангуляции";
            double tw = tip.length() * 6.5;
            gc.setFill(Color.web("#1e293b", 0.72));
            gc.fillRoundRect(W / 2 - tw / 2 - 10, H - 46, tw + 20, 26, 8, 8);
            gc.setFill(Color.web("#e2e8f0"));
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(tip, W / 2, H - 28);
            gc.setTextAlign(TextAlignment.LEFT);
        }

        // ── HUD: кол-во точек и треугольников ────────────────────────────────
        String hudPts  = "●  " + points.size() + " точек";
        String hudTri  = "△  " + triangles.size() + " треугольников";
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        double pw = hudPts.length() * 7.2;
        double tw2 = hudTri.length() * 7.2;
        double hudW = Math.max(pw, tw2) + 24;
        double hudX = W - hudW - 12, hudY = 12;
        gc.setFill(Color.web("#1e293b", 0.78));
        gc.fillRoundRect(hudX, hudY, hudW, 50, 10, 10);
        gc.setFill(Color.web("#60a5fa"));
        gc.fillText(hudPts, hudX + 12, hudY + 20);
        gc.setFill(Color.web("#34d399"));
        gc.fillText(hudTri, hudX + 12, hudY + 40);
    }

    private void drawEmpty() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth(), H = canvas.getHeight();

        // Фон
        gc.setFill(Color.web("#eef2f8"));
        gc.fillRect(0, 0, W, H);

        // Мелкая сетка
        gc.setStroke(Color.web("#dde4ef"));
        gc.setLineWidth(1.0);
        for (double x = 0; x < W; x += 40) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 40) gc.strokeLine(0, y, W, y);
        // Крупная сетка
        gc.setStroke(Color.web("#c8d3e5"));
        for (double x = 0; x < W; x += 200) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 200) gc.strokeLine(0, y, W, y);

        double cx = W / 2, cy = H / 2;

        // Большой круг-подложка
        gc.setFill(Color.web("#dbeafe", 0.60));
        gc.fillOval(cx - 56, cy - 92, 112, 112);
        gc.setStroke(Color.web("#93c5fd"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - 56, cy - 92, 112, 112);

        // Икона «плюс»
        gc.setFill(Color.web("#2563eb"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 52));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("+", cx, cy - 22);

        // Основной заголовок
        gc.setFill(Color.web("#1e3a5f"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        gc.fillText(mouseMode ? "Кликните, чтобы поставить точку"
                              : "Введите координаты слева", cx, cy + 52);

        // Подсказка
        gc.setFill(Color.web("#64748b"));
        gc.setFont(Font.font("Segoe UI", 13));
        gc.fillText(mouseMode
                ? "ЛКМ — добавить   ·   ПКМ — удалить последнюю"
                : "Формат: x y   (по одной паре на строку)", cx, cy + 78);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static double sx(Point p, double minX, double scale, double offX) {
        return (p.x - minX) * scale + offX;
    }
    private static double sy(Point p, double minY, double scale, double offY) {
        return (p.y - minY) * scale + offY;
    }

    // ──────────────────────────── УТИЛИТЫ ─────────────────────────────────────
    private void updateStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #ecf0f1;");
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        updateStatus("Ошибка: " + msg.split("\n")[0], true);
    }

    // ──────────────────────────── INNER CLASSES ───────────────────────────────
    static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        @Override public String toString() { return String.format("(%.1f, %.1f)", x, y); }
    }

    static class Triangle {
        Point p1, p2, p3;
        Triangle(Point p1, Point p2, Point p3) { this.p1 = p1; this.p2 = p2; this.p3 = p3; }
        @Override public String toString() { return p1 + " — " + p2 + " — " + p3; }
    }
}
