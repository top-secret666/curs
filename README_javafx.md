# Инструкция по подключению JavaFX

1. Скачайте JavaFX SDK с https://gluonhq.com/products/javafx/
2. Распакуйте архив, например в C:\javafx-sdk

3. Для компиляции (рекомендуется):

> Важно: добавьте `-encoding UTF-8`, иначе русский текст в UI может превратиться в “кракозябры”.

```powershell
javac -encoding UTF-8 --module-path C:\javafx-sdk\lib --add-modules javafx.controls,javafx.fxml -d out src\triangulation\*.java
```

4. Для запуска:

```powershell
java --module-path C:\javafx-sdk\lib --add-modules javafx.controls,javafx.fxml -cp "out;resources" triangulation.Main
```

_Путь к JavaFX SDK замените на ваш. В Windows в classpath используется `;`._

---

Если используете IDE, настройте путь к JavaFX в настройках проекта.