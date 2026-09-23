# Триангуляция — Жадный алгоритм

STUDY JavaFX-приложение для построения триангуляции множества точек на плоскости с визуализацией.

<img width="1356" height="892" alt="image" src="https://github.com/user-attachments/assets/df8baa48-a8b5-4e57-9ebe-6332a65b7bc1" />

## Функционал

- **Ввод точек** — кликом мыши по холсту (режим рисования) или вводом координат вручную
- **Загрузка из файла** — формат `txt` с парами `x y` на строке, строки с `#` — комментарии
- **Сохранение в файл** — координаты точек, файл можно снова открыть в приложении
- **Жадный алгоритм** — расставляет рёбра от коротких к длинным без пересечений
- **Авто-триангуляция** — строится автоматически при добавлении каждой точки
- **Случайные точки** — генерация от 10 до 20 случайных точек
- **База данных** — сохранение и загрузка сессий через SQLite

## Структура проекта

```
src/triangulation/
  Main.java                   — точка входа JavaFX
  model/
    Point.java                — модель точки (x, y)
    Triangle.java             — модель треугольника (p1, p2, p3)
  algorithm/
    GreedyTriangulation.java  — жадный алгоритм
  db/
    DatabaseManager.java      — SQLite: сохранение/загрузка сессий
  ui/
    TriangulationUI.java      — весь интерфейс
resources/
  style.css                   — тема оформления
  sample_input.txt            — пример входных данных (25 точек)
lib/
  sqlite-jdbc.jar             — драйвер SQLite
  slf4j-api.jar               — логгер (зависимость SQLite JDBC)
  slf4j-nop.jar               — заглушка логгера
```

## Запуск

### Компиляция

```powershell
$sources = Get-ChildItem src\triangulation -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -cp "lib\sqlite-jdbc.jar" --module-path "C:\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -d out $sources
```

### Запуск

```powershell
java --module-path "C:\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml `
     -cp "out;resources;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar" `
     triangulation.Main
```

## Зависимости

- Java 17+
- [JavaFX SDK](https://openjfx.io/) (путь `C:\javafx-sdk\lib`)
- [SQLite JDBC 3.45.1](https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/)
- [SLF4J 2.0.9](https://www.slf4j.org/)

## Формат входного файла

```
# Комментарий (строки с # игнорируются)
50 200
80 90
120 260
```


---
