// PROG2 VT2025, Inlämningsuppgift, del 2
// Grupp 157
// Viktor Hedman vihe4638
// Dan Zavodnik daza3914
// Axel Anderson axan8987
package se.su.inlupp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

public class Gui extends Application {

    private Button newPlaceButton;
    private Button showConnectionButton;

    private Button newConnectionButton;

    private Button findPathButton;

    private Button changeConnectionButton;
    private StackPane centerPane;
    private Map<Circle, Label> circleLabelMap = new HashMap<>();

    private List<Circle> selectedPlaces = new ArrayList<>();
    private Pane overlay;
    private boolean unsavedChanges = false;
    private Graph<String> graph = new ListGraph<>();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        centerPane = new StackPane();

        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(createMenuBar(), createHbox());
        root.setTop(topContainer);

        root.setCenter(centerPane);


        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Min huvudmeny");
        stage.show();
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");

        MenuItem newMap = new MenuItem("New Map");
        newMap.setOnAction(e -> handleNewMap());

        MenuItem open = new MenuItem("Open");
        open.setOnAction(e -> handleOpen());

        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> handleSave());

        MenuItem saveImage = new MenuItem("Save Image");
        saveImage.setOnAction(e -> handleSaveImage());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> handleExit());

        fileMenu.getItems().addAll(newMap, open, save, saveImage, new SeparatorMenuItem(), exit);

        MenuBar menuBar = new MenuBar(fileMenu);
        return menuBar;
    }

    private void handleExit() {

        if (!confirmUnsavedChanges()) return;


        Platform.exit();
        System.exit(0);
    }


    private void handleSaveImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("capture.png"); // föreslaget namn

        File file = fileChooser.showSaveDialog(centerPane.getScene().getWindow());
        if (file != null) {
            WritableImage snapshot = centerPane.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                System.out.println("Snapshot saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Kunde inte spara bilden: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleSave() {
        unsavedChanges = false;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Graph File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Graph files", "*.graph")
        );

        File file = fileChooser.showSaveDialog(centerPane.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                //URL
                ImageView backgroundView = (ImageView) centerPane.getChildren().get(0);
                writer.println(backgroundView.getImage().getUrl());

                //skriver alla noder i en lång rad
                StringBuilder nodeLine = new StringBuilder();
                Pane overlay = (Pane) centerPane.getChildren().get(1);
                for (javafx.scene.Node node : overlay.getChildren()) {
                    if (node instanceof Circle) {
                        Circle circle = (Circle) node;
                        Label label = (Label) overlay.getChildren().stream()
                                .filter(n -> n instanceof Label)
                                .map(n -> (Label) n)
                                .filter(l -> l.getLayoutX() == circle.getLayoutX() + 10 &&
                                        l.getLayoutY() == circle.getLayoutY() - 5)
                                .findFirst().orElse(null);
                        if (label != null) {
                            nodeLine.append(label.getText())
                                    .append(",")
                                    .append((int) circle.getLayoutX())
                                    .append(",")
                                    .append((int) circle.getLayoutY())
                                    .append(";");
                        }
                    }
                }
                if (nodeLine.length() > 0) nodeLine.setLength(nodeLine.length() - 1);
                writer.println(nodeLine.toString());

                //skriver connections
                Set<String> writtenEdges = new HashSet<>();
                for (String fromNode : graph.getNodes()) {
                    for (Edge<String> edge : graph.getEdgesFrom(fromNode)) {
                        String toNode = edge.getDestination();
                        String edgeKey = fromNode + ";" + toNode;
                        String reverseKey = toNode + ";" + fromNode;
                        if (!writtenEdges.contains(edgeKey) && !writtenEdges.contains(reverseKey)) {
                            writer.println(fromNode + ";" + toNode + ";" + edge.getName() + ";" + edge.getWeight());
                            writtenEdges.add(edgeKey);
                            writtenEdges.add(reverseKey);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOpen() {

        if (!confirmUnsavedChanges()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Graph File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graph files", "*.txt"));

        File selectedFile = fileChooser.showOpenDialog(centerPane.getScene().getWindow());
        if (selectedFile != null) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(selectedFile.toPath());

                if (lines.isEmpty()) return;


                String imageUrl = lines.get(0);
                Image backgroundImage = new Image(imageUrl);
                ImageView backgroundView = new ImageView(backgroundImage);
                
                // Ta bort skalning?
                // backgroundView.setFitWidth(centerPane.getWidth());
                // backgroundView.setFitHeight(centerPane.getHeight());
                // backgroundView.setPreserveRatio(true);

                // Centrera bilden i centerPane
                backgroundView.setPreserveRatio(true);
                backgroundView.setSmooth(true);
                centerPane.setAlignment(backgroundView, javafx.geometry.Pos.CENTER);

                Pane overlay = new Pane();
                overlay.setPickOnBounds(false);


                centerPane.getChildren().clear();
                graph = new ListGraph<>();

                centerPane.getChildren().addAll(backgroundView, overlay);


                if (lines.size() > 1) {
                    String[] nodeData = lines.get(1).split(";");
                    for (int i = 0; i < nodeData.length; i += 3) {
                        String name = nodeData[i];
                        double x = Double.parseDouble(nodeData[i + 1]);
                        double y = Double.parseDouble(nodeData[i + 2]);

                        graph.add(name);

                        Circle city = new Circle(8, Color.RED);
                        city.setLayoutX(x);
                        city.setLayoutY(y);

                        Label label = new Label(name);
                        label.setLayoutX(x + 10);
                        label.setLayoutY(y - 5);

                        overlay.getChildren().addAll(city, label);


                    }
                }


                for (int i = 2; i < lines.size(); i++) {
                    String[] edgeData = lines.get(i).split(";");
                    String from = edgeData[0];
                    String to = edgeData[1];
                    String edgeName = edgeData[2];
                    int weight = Integer.parseInt(edgeData[3]);


                    graph.connect(from, to, edgeName, weight);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error opening file: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }


    private void handleNewMap() {
        unsavedChanges = true;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Background Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("bilder", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(centerPane.getScene().getWindow());
        if (selectedFile != null) {
            Image backgroundImage = new Image(selectedFile.toURI().toString());
            centerPane.getChildren().clear();

            ImageView backgroundView = new ImageView(backgroundImage);
            
            backgroundView.setFitWidth(centerPane.getWidth());
            backgroundView.setFitHeight(centerPane.getHeight());
            backgroundView.setPreserveRatio(true);

            overlay = new Pane();

            overlay.setPickOnBounds(true);
            overlay.setMouseTransparent(false);
            overlay.prefWidthProperty().bind(centerPane.widthProperty());
            overlay.prefHeightProperty().bind(centerPane.heightProperty());
            centerPane.getChildren().addAll(backgroundView, overlay);
            enableAllButtons(newPlaceButton, showConnectionButton, findPathButton, changeConnectionButton, newConnectionButton);

        }

    }

    private HBox createHbox() {
        newPlaceButton = new Button("New Place");
        newPlaceButton.setDisable(true);
        newPlaceButton.setOnAction(e -> {
            if (overlay != null) {
                handleAddPlace(newPlaceButton, overlay);
            }
        });

        newConnectionButton = new Button("New Connection");
        newConnectionButton.setDisable(true);
        newConnectionButton.setOnAction(e -> handleAddConnection());

        changeConnectionButton = new Button("Change Connection");
        changeConnectionButton.setDisable(true);
        changeConnectionButton.setOnAction(e -> handleEditConnection());

        showConnectionButton = new Button("Show Connection");
        showConnectionButton.setDisable(true);
        showConnectionButton.setOnAction(e -> handleShowConnection());

        findPathButton = new Button("Find Path");
        findPathButton.setDisable(true);
        findPathButton.setOnAction(e -> handleFindPath());


        HBox menuBox = new HBox(10, newPlaceButton, showConnectionButton, newConnectionButton, changeConnectionButton, findPathButton);
        menuBox.setPadding(new Insets(10));
        return menuBox;
    }

    private void handleFindPath() {
        if (selectedPlaces.size() != 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must select exactly two places");
            alert.showAndWait();
            return;
        }

        Circle fromCity = selectedPlaces.get(0);
        Circle toCity = selectedPlaces.get(1);

        String fromName = ((Label) getLabelForCircle(fromCity)).getText();
        String toName = ((Label) getLabelForCircle(toCity)).getText();

        List<Edge<String>> path = graph.getPath(fromName, toName);

        if (path == null || path.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No path exists between these cities");
            alert.showAndWait();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Start: ").append(fromName).append("\n");
            int totalTime = 0;

            String current = fromName;
            for (Edge<String> edge : path) {
                sb.append(current).append(" -> ").append(edge.getDestination())
                        .append(" (").append(edge.getWeight()).append(" min)\n");
                totalTime += edge.getWeight();
                current = edge.getDestination();
            }
            sb.append("Total time: ").append(totalTime).append(" min");

            Alert alert = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            alert.setHeaderText("Found Path");
            alert.showAndWait();
        }

        selectedPlaces.clear();
    }


    private void handleShowConnection() {
        if (selectedPlaces.size() != 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must select exactly two places");
            alert.showAndWait();
            return;
        }

        Circle fromCity = selectedPlaces.get(0);
        Circle toCity = selectedPlaces.get(1);

        String fromName = ((Label) getLabelForCircle(fromCity)).getText();
        String toName = ((Label) getLabelForCircle(toCity)).getText();


        if (!graph.pathExists(fromName, toName)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No connection exists between these places");
            alert.showAndWait();
            return;
        }


        Edge<String> connection = graph.getEdgeBetween(fromName, toName);
        if (connection == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Connection could not be found");
            alert.showAndWait();
            return;
        }

        String weightStr = String.valueOf(connection.getWeight());

        showConnectionDialog(
                fromName,
                toName,
                connection.getName(),
                weightStr,
                false,
                false
        );
    }


    private void handleEditConnection() {
        if (selectedPlaces.size() != 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must select exactly two places");
            alert.showAndWait();
            return;
        }

        Circle fromCity = selectedPlaces.get(0);
        Circle toCity = selectedPlaces.get(1);

        String fromName = ((Label) getLabelForCircle(fromCity)).getText();
        String toName = ((Label) getLabelForCircle(toCity)).getText();

        Edge<String> edge = graph.getEdgeBetween(fromName, toName);
        if (edge == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No connection exists between the selected cities");
            alert.showAndWait();
            return;
        }


        String weightStr = String.valueOf(edge.getWeight());

        Optional<Pair<String, Integer>> result = showConnectionDialog(fromName, toName, edge.getName(), weightStr, false, true);

        if (result.isPresent()) {
            Pair<String, Integer> pair = result.get();
            String newName = pair.getKey();
            int newWeight = pair.getValue();

            if (newWeight < 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Time must be a non-negative number");
                alert.showAndWait();
                return;
            }
            graph.setConnectionWeight(fromName, toName, newWeight);
        }

        selectedPlaces.clear();
    }


    private void handleAddConnection() {
        if (selectedPlaces.size() != 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must select exactly two places");
            alert.showAndWait();
            return;
        }

        Circle fromCity = selectedPlaces.get(0);
        Circle toCity = selectedPlaces.get(1);

        String fromName = ((Label) getLabelForCircle(fromCity)).getText();
        String toName = ((Label) getLabelForCircle(toCity)).getText();

        Optional<Pair<String, Integer>> result = showConnectionDialog(fromName, toName, "", null, true, true);
        if (result.isPresent()) {
            String connName = result.get().getKey();
            int connTime = result.get().getValue();

            if (connName.trim().isEmpty() || connTime < 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input. Name cannot be empty and time must be a non-negative number");
                alert.showAndWait();
                return;
            }

            if (graph.getEdgeBetween(fromName, toName) != null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "This connection already exists!");
                alert.showAndWait();
                return;
            }

            //lägger till connection i grafen
            graph.connect(fromName, toName, connName, connTime);

            //ritar linje mellan cirklarna
            Line connectionLine = new Line(
                    fromCity.getLayoutX(), fromCity.getLayoutY(),
                    toCity.getLayoutX(), toCity.getLayoutY()
            );
            overlay.getChildren().add(connectionLine);

            fromCity.setFill(Color.BLUE);
            toCity.setFill(Color.BLUE);

            selectedPlaces.clear();
        }
    }


    private void handleAddPlace(Button newPlaceButton, Pane overlay) {

        overlay.setCursor(Cursor.CROSSHAIR);
        newPlaceButton.setDisable(true);

        overlay.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Place");
            dialog.setHeaderText("Enter the name of the city:");
            dialog.setContentText("City name:");

            dialog.showAndWait().ifPresent(name -> {
                if (!graph.getNodes().contains(name)) {
                    graph.add(name);

                    Circle city = new Circle(8, Color.BLUE);
                    makePlaceSelectable(city);
                    city.setLayoutX(x);
                    city.setLayoutY(y);

                    Label label = new Label(name);
                    label.setLayoutX(x + 10);
                    label.setLayoutY(y - 5);

                    overlay.getChildren().addAll(city, label);
                    circleLabelMap.put(city, label);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "This city already exists!");
                    alert.showAndWait();
                }
            });

            overlay.setCursor(Cursor.DEFAULT);
            newPlaceButton.setDisable(false);

            overlay.setOnMouseClicked(null);
        });
    }


    //Buttons

    private Optional<Pair<String, Integer>> showConnectionDialog(String from, String to,
                                                                 String name, String weight,
                                                                 boolean canEditName, boolean canEditTime) {
        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Connection");
        dialog.setHeaderText("Connection between " + from + " and " + to);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setText(name != null ? name : "");
        nameField.setEditable(canEditName);

        TextField timeField = new TextField();
        timeField.setText(weight != null ? String.valueOf(weight) : "");
        timeField.setEditable(canEditTime);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Time:"), 0, 1);
        grid.add(timeField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    int time = Integer.parseInt(timeField.getText().trim());
                    return new Pair<>(nameField.getText().trim(), time);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Node getLabelForCircle(Circle city) {
        return circleLabelMap.get(city);
    }

    private void makePlaceSelectable(Circle city) {
        city.setOnMouseClicked(e -> {
            if (selectedPlaces.contains(city)) {
                city.setFill(Color.BLUE);
                selectedPlaces.remove(city);
            } else {
                if (selectedPlaces.size() < 2) {
                    city.setFill(Color.RED);
                    selectedPlaces.add(city);
                }
            }
        });

    }

    private void enableAllButtons(Button... buttons) {
        for (Button btn : buttons) {
            btn.setDisable(false);
        }
    }

    private boolean confirmUnsavedChanges() {
        if (!unsavedChanges) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Continue without saving?");

        ButtonType continueButton = new ButtonType("Continue");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(continueButton, cancelButton);

        return alert.showAndWait().orElse(cancelButton) == continueButton;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
