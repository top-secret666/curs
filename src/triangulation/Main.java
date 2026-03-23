package triangulation;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("▲ Триангуляция — Жадный алгоритм");
        TriangulationUI root = new TriangulationUI(primaryStage);

        Scene scene = new Scene(root, 1100, 700);

        URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl == null) cssUrl = getClass().getResource("/resources/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            Path cssPath = Path.of("resources", "style.css");
            if (Files.exists(cssPath))
                scene.getStylesheets().add(cssPath.toUri().toString());
        }

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

        URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl == null) {
            cssUrl = getClass().getResource("/resources/style.css");
        }
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            Path cssPath = Path.of("resources", "style.css");
            if (Files.exists(cssPath)) {
                scene.getStylesheets().add(cssPath.toUri().toString());
            }
        }
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
