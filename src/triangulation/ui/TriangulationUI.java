package triangulation.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import triangulation.algorithm.GreedyTriangulation;
import triangulation.db.DatabaseManager;
import triangulation.model.Point;
import triangulation.model.Triangle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TriangulationUI extends HBox {

    private final List<Point>    points    = new ArrayList<>();
    private final List<Triangle> triangles = new ArrayList<>();

    private boolean mouseMode  = true;
    private boolean autoTriang = true;
    private double  mapMinX, mapMinY, mapScale, mapOffX, mapOffY;
    private boolean mapReady   = false;

    private TextArea     inputArea;
    private TextArea     outputArea;
    private Canvas       canvas;
    private Label        statusLabel;
    private Label        validationLabel;
    private Label        coordLabel;
    private Label        pointCountLabel;
    private ToggleButton mouseModeBtn;

    public TriangulationUI(Stage stage) {
        setSpacing(0);
        setPadding(Insets.EMPTY);
        VBox leftPanel  = buildLeftPanel(stage);
        VBox rightPanel = buildRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        getChildren().addAll(leftPanel, rightPanel);
    }

    // ═══════════════════════════ LEFT PANEL ═══════════════════════════════════
    private VBox buildLeftPanel(Stage stage) {
        VBox root = new VBox(0);
        root.setPrefWidth(306);
        root.setMinWidth(260);
        root.setMaxWidth(360);
        root.getStyleClass().add("left-panel");

        // Header
        VBox header = new VBox(2);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(22, 20, 18, 20));
        Label icon  = new Label("\u25b3");
        icon.getStyleClass().add("app-icon");
        Label title = new Label("\u0422\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044f");
        title.getStyleClass().add("title-label");
        Label sub   = new Label("\u0416\u0430\u0434\u043d\u044b\u0439 \u0430\u043b\u0433\u043e\u0440\u0438\u0442\u043c");
        sub.getStyleClass().add("subtitle-label");
        header.getChildren().addAll(icon, title, sub);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14, 14, 20, 14));

        // ── Drawing mode ──────────────────────────────────────────────────────
        mouseModeBtn = new ToggleButton("\u270f  \u0420\u0435\u0436\u0438\u043c \u0440\u0438\u0441\u043e\u0432\u0430\u043d\u0438\u044f  \u00b7  \u0410\u041a\u0422\u0418\u0412\u0415\u041d");
        mouseModeBtn.setSelected(true);
        mouseModeBtn.setMaxWidth(Double.MAX_VALUE);
        mouseModeBtn.getStyleClass().add("mode-toggle");
        mouseModeBtn.selectedProperty().addListener((obs, o, n) -> {
            mouseMode = n;
            mouseModeBtn.setText(n
                    ? "\u270f  \u0420\u0435\u0436\u0438\u043c \u0440\u0438\u0441\u043e\u0432\u0430\u043d\u0438\u044f  \u00b7  \u0410\u041a\u0422\u0418\u0412\u0415\u041d"
                    : "\u270f  \u0420\u0435\u0436\u0438\u043c \u0440\u0438\u0441\u043e\u0432\u0430\u043d\u0438\u044f  \u00b7  \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d");
            if (!mouseMode) redrawFromInput();
            else redraw();
        });

        CheckBox autoCheck = new CheckBox("\u0410\u0432\u0442\u043e-\u0442\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044f \u043f\u0440\u0438 \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u0438\u0438");
        autoCheck.setSelected(true);
        autoCheck.getStyleClass().add("auto-check");
        autoCheck.selectedProperty().addListener((obs, o, n) -> autoTriang = n);

        Label drawHint = new Label("\u041b\u041a\u041c \u2014 \u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0442\u043e\u0447\u043a\u0443   \u00b7   \u041f\u041a\u041c \u2014 \u0443\u0431\u0440\u0430\u0442\u044c \u043f\u043e\u0441\u043b\u0435\u0434\u043d\u044e\u044e");
        drawHint.getStyleClass().add("hint-label");
        drawHint.setWrapText(true);

        VBox modeCard = new VBox(8, mouseModeBtn, autoCheck, drawHint);
        modeCard.getStyleClass().add("card");

        // ── Input coordinates ────────────────────────────────────────────────
        Label inputLabel = new Label("\u041a\u041e\u041e\u0420\u0414\u0418\u041d\u0410\u0422\u042b  ( x  y )");
        inputLabel.getStyleClass().add("section-label");

        inputArea = new TextArea();
        inputArea.setPromptText("100 100\n200 100\n150 200\n...");
        inputArea.setPrefRowCount(7);
        inputArea.setWrapText(false);
        inputArea.textProperty().addListener((obs, o, n) -> validateInput(n));

        validationLabel = new Label("");
        validationLabel.getStyleClass().add("error-text");
        validationLabel.setWrapText(true);

        Button loadBtn  = new Button("\ud83d\udcc2  \u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0444\u0430\u0439\u043b");
        Button clearBtn = new Button("\u2715  \u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.getStyleClass().add("btn-secondary");
        clearBtn.getStyleClass().add("btn-danger");
        HBox.setHgrow(loadBtn,  Priority.ALWAYS);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        loadBtn.setOnAction(e -> loadFromFile(stage));
        clearBtn.setOnAction(e -> clearAll());
        HBox fileRow = new HBox(8, loadBtn, clearBtn);

        Button undoBtn = new Button("\u21a9  \u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u043e\u0441\u043b\u0435\u0434\u043d\u044e\u044e \u0442\u043e\u0447\u043a\u0443");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.getStyleClass().add("btn-outline");
        undoBtn.setOnAction(e -> removeLastPoint());

        pointCountLabel = new Label("\u0422\u043e\u0447\u0435\u043a \u043d\u0430 \u0445\u043e\u043b\u0441\u0442\u0435: 0");
        pointCountLabel.getStyleClass().add("point-count-label");

        VBox inputCard = new VBox(8, inputLabel, inputArea, validationLabel, fileRow, undoBtn, pointCountLabel);
        inputCard.getStyleClass().add("card");

        // ── Actions ──────────────────────────────────────────────────────────
        Button triangulateBtn = new Button("\u25b6   \u041f\u043e\u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0442\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044e");
        triangulateBtn.getStyleClass().add("btn-primary");
        triangulateBtn.setMaxWidth(Double.MAX_VALUE);
        triangulateBtn.setOnAction(e -> triangulate());

        Button randomBtn = new Button("\u2684   \u0421\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0435 \u0442\u043e\u0447\u043a\u0438");
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.getStyleClass().add("btn-outline");
        randomBtn.setOnAction(e -> generateRandom());

        // ── Result ───────────────────────────────────────────────────────────
        Label outputLabel = new Label("\u0420\u0415\u0417\u0423\u041b\u042c\u0422\u0410\u0422");
        outputLabel.getStyleClass().add("section-label");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(5);
        outputArea.setPromptText("\u0417\u0434\u0435\u0441\u044c \u043f\u043e\u044f\u0432\u044f\u0442\u0441\u044f \u0442\u0440\u0435\u0443\u0433\u043e\u043b\u044c\u043d\u0438\u043a\u0438...");

        Button saveBtn = new Button("\ud83d\udcbe  \u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().add("btn-secondary");
        saveBtn.setOnAction(e -> saveToFile(stage));

        VBox outputCard = new VBox(8, outputLabel, outputArea, saveBtn);
        outputCard.getStyleClass().add("card");

        // ── Database ────────────────────────────────────────────────────────
        Label dbLabel = new Label("\u0411\u0410\u0417\u0410 \u0414\u0410\u041d\u041d\u042b\u0425");
        dbLabel.getStyleClass().add("section-label");

        Button dbSaveBtn = new Button("\ud83d\udcbe  \u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432 \u0411\u0414");
        dbSaveBtn.setMaxWidth(Double.MAX_VALUE);
        dbSaveBtn.getStyleClass().add("btn-secondary");
        dbSaveBtn.setOnAction(e -> saveToDatabase());

        Button dbLoadBtn = new Button("\ud83d\udccb  \u0418\u0441\u0442\u043e\u0440\u0438\u044f \u0441\u0435\u0441\u0441\u0438\u0439");
        dbLoadBtn.setMaxWidth(Double.MAX_VALUE);
        dbLoadBtn.getStyleClass().add("btn-outline");
        dbLoadBtn.setOnAction(e -> showDatabaseDialog());

        VBox dbCard = new VBox(8, dbLabel, dbSaveBtn, dbLoadBtn);
        dbCard.getStyleClass().add("card");

        content.getChildren().addAll(modeCard, inputCard, triangulateBtn, randomBtn, outputCard, dbCard);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("left-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    // ═══════════════════════════ RIGHT PANEL ══════════════════════════════════
    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("right-panel");

        Label canvasTitle = new Label("\u0412\u0438\u0437\u0443\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u044f");
        canvasTitle.getStyleClass().add("canvas-title");

        coordLabel = new Label("\u2014");
        coordLabel.getStyleClass().add("coord-label");

        Button undoToolBtn  = new Button("\u21a9  \u041e\u0442\u043c\u0435\u043d\u0430");
        Button clearToolBtn = new Button("\u2715  \u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c");
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

        Pane canvasHolder = new Pane();
        canvasHolder.getStyleClass().add("canvas-holder");
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        canvas = new Canvas();
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvas.widthProperty().addListener(e -> redraw());
        canvas.heightProperty().addListener(e -> redraw());
        canvasHolder.getChildren().add(canvas);

        canvas.setOnMouseClicked(ev -> {
            if (!mouseMode) return;
            if (ev.getButton() == MouseButton.PRIMARY) addMousePoint(ev.getX(), ev.getY());
            else if (ev.getButton() == MouseButton.SECONDARY) removeLastPoint();
        });

        canvas.setOnMouseMoved(ev -> {
            if (mapReady && mapScale > 0) {
                double dx = (ev.getX() - mapOffX) / mapScale + mapMinX;
                double dy = (ev.getY() - mapOffY) / mapScale + mapMinY;
                coordLabel.setText(String.format("x: %.0f   y: %.0f", dx, dy));
            } else {
                coordLabel.setText(String.format("x: %.0f   y: %.0f", ev.getX(), ev.getY()));
            }
            if (mouseMode) drawCrosshair(ev.getX(), ev.getY());
        });

        canvas.setOnMouseExited(ev -> {
            coordLabel.setText("\u2014");
            redraw();
        });

        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("\u0413\u043e\u0442\u043e\u0432\u043e  \u00b7  \u043a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u0445\u043e\u043b\u0441\u0442\u0443 \u0434\u043b\u044f \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u0438\u044f \u0442\u043e\u0447\u0435\u043a");
        statusLabel.getStyleClass().add("status-label");
        statusBar.getChildren().add(statusLabel);

        panel.getChildren().addAll(toolbar, canvasHolder, statusBar);
        canvasHolder.layoutBoundsProperty().addListener(
                (obs, o, n) -> { if (n.getWidth() > 0) redraw(); });
        return panel;
    }

    // ══════════════════════════ MOUSE INPUT ═══════════════════════════════════
    private void addMousePoint(double screenX, double screenY) {
        double dataX, dataY;
        if (mapReady && mapScale > 0) {
            dataX = (screenX - mapOffX) / mapScale + mapMinX;
            dataY = (screenY - mapOffY) / mapScale + mapMinY;
        } else {
            dataX = screenX;
            dataY = screenY;
        }
        String existing = inputArea.getText().trim();
        String newLine  = String.format("%.0f %.0f", dataX, dataY);
        inputArea.setText(existing.isEmpty() ? newLine : existing + "\n" + newLine);

        points.add(new Point(dataX, dataY));
        updatePointCount();
        redraw();

        if (autoTriang && points.size() >= 3 && !areAllCollinear(points))
            triangulateInternal();

        updateStatus("\u0422\u043e\u0447\u043a\u0430 (" + (int) dataX + ", " + (int) dataY
                + ")  \u00b7  \u0412\u0441\u0435\u0433\u043e: " + points.size(), false);
    }

    private void removeLastPoint() {
        if (points.isEmpty()) return;
        points.remove(points.size() - 1);
        String text = inputArea.getText().trim();
        int lastNl = text.lastIndexOf('\n');
        inputArea.setText(lastNl >= 0 ? text.substring(0, lastNl) : "");
        triangles.clear();
        if (autoTriang && points.size() >= 3 && !areAllCollinear(points))
            triangulateInternal();
        updatePointCount();
        redraw();
        updateStatus("\u0422\u043e\u0447\u043a\u0430 \u0443\u0434\u0430\u043b\u0435\u043d\u0430  \u00b7  \u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + points.size(), false);
    }

    private void clearAll() {
        inputArea.clear();
        outputArea.clear();
        points.clear();
        triangles.clear();
        mapReady = false;
        updatePointCount();
        redraw();
        updateStatus("\u0425\u043e\u043b\u0441\u0442 \u043e\u0447\u0438\u0449\u0435\u043d", false);
    }

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
        updateStatus("\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e " + count + " \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0445 \u0442\u043e\u0447\u0435\u043a  \u00b7  \u0412\u0441\u0435\u0433\u043e: " + points.size(), false);
    }

    private void updatePointCount() {
        if (pointCountLabel != null)
            pointCountLabel.setText("\u0422\u043e\u0447\u0435\u043a \u043d\u0430 \u0445\u043e\u043b\u0441\u0442\u0435: " + points.size());
    }

    // ══════════════════════════ VALIDATION ════════════════════════════════════
    private void validateInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            validationLabel.setText("");
            inputArea.getStyleClass().removeAll("validation-error", "validation-ok");
            return;
        }
        text = stripBom(text);
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
                errors.add("\u0421\u0442\u0440. " + lineNum + ": \u043d\u0443\u0436\u043d\u043e 2 \u0447\u0438\u0441\u043b\u0430, \u043d\u0430\u0439\u0434\u0435\u043d\u043e " + parts.length);
                continue;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    errors.add("\u0421\u0442\u0440. " + lineNum + ": NaN/Infinity");
                    continue;
                }
                if (x < -10000 || x > 10000 || y < -10000 || y > 10000) {
                    errors.add("\u0421\u0442\u0440. " + lineNum + ": \u0432\u043d\u0435 \u0434\u0438\u0430\u043f\u0430\u0437\u043e\u043d\u0430 [-10000, 10000]");
                    continue;
                }
                String key = Math.round(x * 100) + ";" + Math.round(y * 100);
                if (seen.contains(key)) {
                    errors.add("\u0421\u0442\u0440. " + lineNum + ": \u0434\u0443\u0431\u043b\u0438\u043a\u0430\u0442 (" + (int) x + ", " + (int) y + ")");
                    continue;
                }
                seen.add(key);
                validCount++;
            } catch (NumberFormatException e) {
                errors.add("\u0421\u0442\u0440. " + lineNum + ": \u043d\u0435 \u0447\u0438\u0441\u043b\u0430");
            }
        }
        inputArea.getStyleClass().removeAll("validation-error", "validation-ok");
        if (!errors.isEmpty()) {
            validationLabel.getStyleClass().remove("ok-text");
            validationLabel.getStyleClass().add("error-text");
            String msg = String.join("\n", errors.subList(0, Math.min(3, errors.size())));
            if (errors.size() > 3) msg += "\n...\u0435\u0449\u0451 " + (errors.size() - 3);
            validationLabel.setText("\u26a0 " + msg);
            inputArea.getStyleClass().add("validation-error");
        } else if (validCount < 3) {
            validationLabel.getStyleClass().remove("ok-text");
            validationLabel.getStyleClass().add("error-text");
            validationLabel.setText("\u26a0 \u041d\u0443\u0436\u043d\u043e \u043c\u0438\u043d\u0438\u043c\u0443\u043c 3 \u0442\u043e\u0447\u043a\u0438 (\u0441\u0435\u0439\u0447\u0430\u0441: " + validCount + ")");
            inputArea.getStyleClass().add("validation-error");
        } else {
            validationLabel.getStyleClass().remove("error-text");
            validationLabel.getStyleClass().add("ok-text");
            validationLabel.setText("\u2713 " + validCount + " \u0442\u043e\u0447\u0435\u043a \u0433\u043e\u0442\u043e\u0432\u043e");
            inputArea.getStyleClass().add("validation-ok");
        }
    }

    // ══════════════════════════ FILE I/O ══════════════════════════════════════
    private void loadFromFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0444\u0430\u0439\u043b \u0441 \u0442\u043e\u0447\u043a\u0430\u043c\u0438");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("\u0422\u0435\u043a\u0441\u0442\u043e\u0432\u044b\u0435 \u0444\u0430\u0439\u043b\u044b", "*.txt"),
                new FileChooser.ExtensionFilter("\u0412\u0441\u0435 \u0444\u0430\u0439\u043b\u044b", "*.*"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");

            String content = stripBom(sb.toString().trim());
            inputArea.setText(content);

            points.clear();
            triangles.clear();
            parsePointsFromText();
            updatePointCount();

            if (points.size() >= 3 && !areAllCollinear(points))
                triangulateInternal();

            redraw();
            updateStatus("\u0424\u0430\u0439\u043b \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d: " + file.getName()
                    + "  \u00b7  \u0422\u043e\u0447\u0435\u043a: " + points.size()
                    + (triangles.isEmpty() ? "" : "  \u00b7  \u25b3: " + triangles.size()), false);
        } catch (IOException ex) {
            showError("\u041e\u0448\u0438\u0431\u043a\u0430 \u0447\u0442\u0435\u043d\u0438\u044f \u0444\u0430\u0439\u043b\u0430: " + ex.getMessage());
        }
    }

    private void saveToFile(Stage stage) {
        if (points.isEmpty()) {
            showError("\u041d\u0435\u0442 \u0434\u0430\u043d\u043d\u044b\u0445. \u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0434\u043e\u0431\u0430\u0432\u044c\u0442\u0435 \u0442\u043e\u0447\u043a\u0438.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("\u0422\u0435\u043a\u0441\u0442\u043e\u0432\u044b\u0435 \u0444\u0430\u0439\u043b\u044b", "*.txt"));
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                pw.println("# \u0422\u043e\u0447\u043a\u0438: " + points.size()
                        + "  \u0422\u0440\u0435\u0443\u0433\u043e\u043b\u044c\u043d\u0438\u043a\u043e\u0432: " + triangles.size());
                for (Point p : points)
                    pw.println(String.format("%.0f %.0f", p.x, p.y));
                updateStatus("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e: " + file.getName()
                        + " (" + points.size() + " \u0442\u043e\u0447\u0435\u043a)", false);
            } catch (IOException ex) {
                showError("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u043f\u0438\u0441\u0438: " + ex.getMessage());
            }
        }
    }

    // ══════════════════════════ TRIANGULATION ═════════════════════════════════
    private void redrawFromInput() {
        points.clear();
        triangles.clear();
        parsePointsFromText();
        updatePointCount();
        redraw();
    }

    private void parsePointsFromText() {
        String text = stripBom(inputArea.getText());
        String[] lines = text.split("\n");
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
        points.clear();
        triangles.clear();
        List<String> errors = new ArrayList<>();
        String text = stripBom(inputArea.getText());
        String[] lines = text.split("\n");
        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            String t = line.trim().replace(',', '.').replace('\t', ' ');
            if (t.isEmpty() || t.startsWith("#")) continue;
            String[] parts = t.split("\\s+");
            if (parts.length != 2) {
                errors.add("\u0421\u0442\u0440. " + lineNum + ": \u043d\u0443\u0436\u043d\u043e 2 \u0447\u0438\u0441\u043b\u0430");
                continue;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    errors.add("\u0421\u0442\u0440. " + lineNum + ": NaN/Infinity");
                    continue;
                }
                points.add(new Point(x, y));
            } catch (NumberFormatException e) {
                errors.add("\u0421\u0442\u0440. " + lineNum + ": \u043d\u0435 \u0447\u0438\u0441\u043b\u0430");
            }
        }
        updatePointCount();
        if (!errors.isEmpty()) {
            showError("\u041e\u0448\u0438\u0431\u043a\u0438:\n" + String.join("\n", errors));
            return;
        }
        if (points.size() < 3) {
            showError("\u041d\u0443\u0436\u043d\u043e \u043c\u0438\u043d\u0438\u043c\u0443\u043c 3 \u0442\u043e\u0447\u043a\u0438.\n\u0421\u0435\u0439\u0447\u0430\u0441: " + points.size());
            return;
        }
        if (areAllCollinear(points)) {
            showError("\u0412\u0441\u0435 \u0442\u043e\u0447\u043a\u0438 \u043d\u0430 \u043e\u0434\u043d\u043e\u0439 \u043f\u0440\u044f\u043c\u043e\u0439 \u2014 \u0442\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044f \u043d\u0435\u0432\u043e\u0437\u043c\u043e\u0436\u043d\u0430.");
            return;
        }
        triangulateInternal();
    }

    private void triangulateInternal() {
        long t0 = System.nanoTime();
        triangles.clear();
        triangles.addAll(GreedyTriangulation.triangulate(points));
        long ms = (System.nanoTime() - t0) / 1_000_000;

        StringBuilder sb = new StringBuilder();
        sb.append("=== \u0416\u0430\u0434\u043d\u0430\u044f \u0442\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044f ===\n");
        sb.append("\u0422\u043e\u0447\u0435\u043a:        ").append(points.size()).append("\n");
        sb.append("\u0422\u0440\u0435\u0443\u0433\u043e\u043b\u044c\u043d\u0438\u043a\u043e\u0432:").append(triangles.size()).append("\n");
        sb.append("\u0412\u0440\u0435\u043c\u044f:        ").append(ms).append(" \u043c\u0441\n");
        sb.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n");
        for (int i = 0; i < triangles.size(); i++)
            sb.append("\u25b3 ").append(i + 1).append(": ").append(triangles.get(i)).append("\n");
        outputArea.setText(sb.toString());
        redraw();
        updateStatus("\u0422\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044f: " + triangles.size() + " \u25b3 \u0438\u0437 "
                + points.size() + " \u0442\u043e\u0447\u0435\u043a \u0437\u0430 " + ms + " \u043c\u0441", false);
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

    // ══════════════════════════ DATABASE ══════════════════════════════════════
    private void saveToDatabase() {
        if (points.isEmpty()) {
            showError("\u041d\u0435\u0442 \u0442\u043e\u0447\u0435\u043a \u0434\u043b\u044f \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f.");
            return;
        }
        String defaultName = "\u0421\u0435\u0441\u0441\u0438\u044f " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        TextInputDialog dialog = new TextInputDialog(defaultName);
        dialog.setTitle("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432 \u0431\u0430\u0437\u0443 \u0434\u0430\u043d\u043d\u044b\u0445");
        dialog.setHeaderText("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0441\u0435\u0441\u0441\u0438\u0438:");
        dialog.setContentText("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            try {
                DatabaseManager.getInstance().saveSession(name.trim(), points, triangles);
                updateStatus("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e \u0432 \u0411\u0414: " + name.trim()
                        + " (" + points.size() + " \u0442\u043e\u0447\u0435\u043a, " + triangles.size() + " \u25b3)", false);
            } catch (Exception ex) {
                showError("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f \u0432 \u0411\u0414:\n" + ex.getMessage());
            }
        });
    }

    private void showDatabaseDialog() {
        try {
            List<DatabaseManager.SessionInfo> sessions = DatabaseManager.getInstance().getAllSessions();
            if (sessions.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION,
                        "\u041d\u0435\u0442 \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0445 \u0441\u0435\u0441\u0441\u0438\u0439.", ButtonType.OK);
                a.setTitle("\u0411\u0430\u0437\u0430 \u0434\u0430\u043d\u043d\u044b\u0445");
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("\u0421\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435 \u0441\u0435\u0441\u0441\u0438\u0438");
            dialog.setHeaderText("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0435\u0441\u0441\u0438\u044e \u0434\u043b\u044f \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438:");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(520);

            VBox list = new VBox(8);
            list.setPadding(new Insets(8));

            for (DatabaseManager.SessionInfo si : sessions) {
                VBox info = new VBox(2);
                Label nameL = new Label(si.name);
                nameL.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Label detL = new Label(si.pointCount + " \u0442\u043e\u0447\u0435\u043a  \u00b7  "
                        + si.triangleCount + " \u25b3  \u00b7  " + si.createdAt);
                detL.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
                info.getChildren().addAll(nameL, detL);
                HBox.setHgrow(info, Priority.ALWAYS);

                Button loadB = new Button("\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c");
                loadB.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px;");
                loadB.setOnAction(ev -> {
                    loadFromDatabase(si.id, si.name);
                    dialog.close();
                });

                Button delB = new Button("\u0423\u0434\u0430\u043b\u0438\u0442\u044c");
                delB.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");

                HBox row = new HBox(10, info, loadB, delB);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 8;");

                delB.setOnAction(ev -> {
                    try {
                        DatabaseManager.getInstance().deleteSession(si.id);
                        list.getChildren().remove(row);
                        updateStatus("\u0421\u0435\u0441\u0441\u0438\u044f \u0443\u0434\u0430\u043b\u0435\u043d\u0430: " + si.name, false);
                    } catch (Exception ex) {
                        showError("\u041e\u0448\u0438\u0431\u043a\u0430 \u0443\u0434\u0430\u043b\u0435\u043d\u0438\u044f: " + ex.getMessage());
                    }
                });
                list.getChildren().add(row);
            }

            ScrollPane sp = new ScrollPane(list);
            sp.setFitToWidth(true);
            sp.setPrefHeight(340);
            dialog.getDialogPane().setContent(sp);
            dialog.showAndWait();
        } catch (Exception ex) {
            showError("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u0438\u0437 \u0411\u0414:\n" + ex.getMessage());
        }
    }

    private void loadFromDatabase(int sessionId, String sessionName) {
        try {
            List<Point>    dbPts = DatabaseManager.getInstance().loadPoints(sessionId);
            List<Triangle> dbTri = DatabaseManager.getInstance().loadTriangles(sessionId);

            points.clear();
            triangles.clear();
            points.addAll(dbPts);
            triangles.addAll(dbTri);

            StringBuilder sb = new StringBuilder();
            for (Point p : points)
                sb.append(String.format("%.0f %.0f\n", p.x, p.y));
            inputArea.setText(sb.toString().trim());

            if (!triangles.isEmpty()) {
                StringBuilder out = new StringBuilder();
                out.append("=== \u0416\u0430\u0434\u043d\u0430\u044f \u0442\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u044f ===\n");
                out.append("\u0422\u043e\u0447\u0435\u043a:        ").append(points.size()).append("\n");
                out.append("\u0422\u0440\u0435\u0443\u0433\u043e\u043b\u044c\u043d\u0438\u043a\u043e\u0432:").append(triangles.size()).append("\n");
                out.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n");
                for (int i = 0; i < triangles.size(); i++)
                    out.append("\u25b3 ").append(i + 1).append(": ").append(triangles.get(i)).append("\n");
                outputArea.setText(out.toString());
            }
            updatePointCount();
            redraw();
            updateStatus("\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u043e \u0438\u0437 \u0411\u0414: " + sessionName
                    + " (" + points.size() + " \u0442\u043e\u0447\u0435\u043a, " + triangles.size() + " \u25b3)", false);
        } catch (Exception ex) {
            showError("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u0438\u0437 \u0411\u0414:\n" + ex.getMessage());
        }
    }

    // ══════════════════════════ DRAWING ═══════════════════════════════════════
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

        gc.setFill(Color.web("#f0f4f8"));
        gc.fillRect(0, 0, W, H);

        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(1.0);
        for (double x = 0; x < W; x += 40) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 40) gc.strokeLine(0, y, W, y);
        gc.setStroke(Color.web("#cbd5e1"));
        for (double x = 0; x < W; x += 200) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 200) gc.strokeLine(0, y, W, y);

        if (points.isEmpty()) return;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : points) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        double dW = Math.max(maxX - minX, 1), dH = Math.max(maxY - minY, 1);
        double pad = 55;
        double scaleX = (W - 2 * pad) / dW, scaleY = (H - 2 * pad) / dH;
        double scale  = Math.min(scaleX, scaleY);
        double offX   = pad + ((W - 2 * pad) - dW * scale) / 2;
        double offY   = pad + ((H - 2 * pad) - dH * scale) / 2;

        mapMinX = minX; mapMinY = minY;
        mapScale = scale; mapOffX = offX; mapOffY = offY;
        mapReady = true;

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
            double[] xs = {sx(t.p1, minX, scale, offX), sx(t.p2, minX, scale, offX), sx(t.p3, minX, scale, offX)};
            double[] ys = {sy(t.p1, minY, scale, offY), sy(t.p2, minY, scale, offY), sy(t.p3, minY, scale, offY)};
            gc.setFill(palFill[i % palFill.length]);
            gc.fillPolygon(xs, ys, 3);
        }

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

        for (int i = 0; i < points.size(); i++) {
            Point  p  = points.get(i);
            double px = sx(p, minX, scale, offX);
            double py = sy(p, minY, scale, offY);
            gc.setFill(Color.web("#000000", 0.10));
            gc.fillOval(px - 7, py - 5, 14, 14);
            gc.setFill(Color.web("#ef4444"));
            gc.fillOval(px - 6, py - 6, 12, 12);
            gc.setFill(Color.web("#ffffff", 0.50));
            gc.fillOval(px - 4, py - 5, 5, 4);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(px - 6, py - 6, 12, 12);
            gc.setFill(Color.web("#1e40af"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(String.valueOf(i + 1), px + 8, py - 4);
        }

        if (crosshair != null && mouseMode) {
            gc.setStroke(Color.web("#3b82f6", 0.45));
            gc.setLineWidth(1);
            gc.setLineDashes(5, 5);
            gc.strokeLine(cx, 0, cx, H);
            gc.strokeLine(0, cy, W, cy);
            gc.setLineDashes();
            gc.setStroke(Color.web("#3b82f6", 0.75));
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - 8, cy - 8, 16, 16);
            gc.setFill(Color.web("#3b82f6", 0.55));
            gc.fillOval(cx - 3, cy - 3, 6, 6);
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

        if (mouseMode && points.size() < 3) {
            String tip = points.size() == 0
                    ? "\u041b\u041a\u041c \u2014 \u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0442\u043e\u0447\u043a\u0443   \u00b7   \u041f\u041a\u041c \u2014 \u0443\u0431\u0440\u0430\u0442\u044c \u043f\u043e\u0441\u043b\u0435\u0434\u043d\u044e\u044e"
                    : "\u041d\u0443\u0436\u043d\u043e \u0435\u0449\u0451 " + (3 - points.size()) + " \u0442\u043e\u0447\u043a\u0438 \u0434\u043b\u044f \u0442\u0440\u0438\u0430\u043d\u0433\u0443\u043b\u044f\u0446\u0438\u0438";
            double tw = tip.length() * 6.5;
            gc.setFill(Color.web("#1e293b", 0.72));
            gc.fillRoundRect(W / 2 - tw / 2 - 10, H - 46, tw + 20, 26, 8, 8);
            gc.setFill(Color.web("#e2e8f0"));
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(tip, W / 2, H - 28);
            gc.setTextAlign(TextAlignment.LEFT);
        }

        String hudPts = "\u25cf  " + points.size() + " \u0442\u043e\u0447\u0435\u043a";
        String hudTri = "\u25b3  " + triangles.size() + " \u0442\u0440\u0435\u0443\u0433\u043e\u043b\u044c\u043d\u0438\u043a\u043e\u0432";
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
        gc.setFill(Color.web("#eef2f8"));
        gc.fillRect(0, 0, W, H);
        gc.setStroke(Color.web("#dde4ef"));
        gc.setLineWidth(1.0);
        for (double x = 0; x < W; x += 40) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 40) gc.strokeLine(0, y, W, y);
        gc.setStroke(Color.web("#c8d3e5"));
        for (double x = 0; x < W; x += 200) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 200) gc.strokeLine(0, y, W, y);
        double cx = W / 2, cy = H / 2;
        gc.setFill(Color.web("#dbeafe", 0.60));
        gc.fillOval(cx - 56, cy - 92, 112, 112);
        gc.setStroke(Color.web("#93c5fd"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - 56, cy - 92, 112, 112);
        gc.setFill(Color.web("#2563eb"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 52));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("+", cx, cy - 22);
        gc.setFill(Color.web("#1e3a5f"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        gc.fillText(mouseMode ? "\u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0442\u043e\u0447\u043a\u0443"
                : "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b \u0441\u043b\u0435\u0432\u0430", cx, cy + 52);
        gc.setFill(Color.web("#64748b"));
        gc.setFont(Font.font("Segoe UI", 13));
        gc.fillText(mouseMode
                ? "\u041b\u041a\u041c \u2014 \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c   \u00b7   \u041f\u041a\u041c \u2014 \u0443\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u043e\u0441\u043b\u0435\u0434\u043d\u044e\u044e"
                : "\u0424\u043e\u0440\u043c\u0430\u0442: x y   (\u043f\u043e \u043e\u0434\u043d\u043e\u0439 \u043f\u0430\u0440\u0435 \u043d\u0430 \u0441\u0442\u0440\u043e\u043a\u0443)", cx, cy + 78);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static double sx(Point p, double minX, double scale, double offX) {
        return (p.x - minX) * scale + offX;
    }

    private static double sy(Point p, double minY, double scale, double offY) {
        return (p.y - minY) * scale + offY;
    }

    // ══════════════════════════ UTILITIES ═════════════════════════════════════
    private static String stripBom(String s) {
        return (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') ? s.substring(1) : s;
    }

    private void updateStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #ecf0f1;");
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("\u041e\u0448\u0438\u0431\u043a\u0430");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        updateStatus("\u041e\u0448\u0438\u0431\u043a\u0430: " + msg.split("\n")[0], true);
    }
}
