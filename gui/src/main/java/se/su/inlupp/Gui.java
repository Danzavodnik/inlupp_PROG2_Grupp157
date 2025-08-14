package se.su.inlupp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Gui extends Application {

    private Button newPlaceButton;
    private Button showConnectionButton;

    private Button newConnectionButton;

    private Button findPathButton;

    private Button changeConnectionButton;
    private StackPane centerPane;

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
                // URL
                ImageView backgroundView = (ImageView) centerPane.getChildren().get(0);
                writer.println(backgroundView.getImage().getUrl());

                // 2. Skriv alla noder i en lång rad
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

                // Write connections
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

                // 1. Läs bakgrundsbild
                String imageUrl = lines.get(0);
                Image backgroundImage = new Image(imageUrl);
                ImageView backgroundView = new ImageView(backgroundImage);
                backgroundView.setFitWidth(centerPane.getWidth());
                backgroundView.setFitHeight(centerPane.getHeight());
                backgroundView.setPreserveRatio(true);

                Pane overlay = new Pane();
                overlay.setPickOnBounds(false);

                // Rensa tidigare graf
                centerPane.getChildren().clear();
                graph = new ListGraph<>();

                centerPane.getChildren().addAll(backgroundView, overlay);

                // 2. Läs noder
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

                        // Gör så att man kan klicka på noden för att skapa förbindelser om ni vill
                    }
                }

                // 3. Läs förbindelser
                for (int i = 2; i < lines.size(); i++) {
                    String[] edgeData = lines.get(i).split(";");
                    String from = edgeData[0];
                    String to = edgeData[1];
                    String edgeName = edgeData[2];
                    int weight = Integer.parseInt(edgeData[3]);

                    // Lägg till förbindelse i grafen
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
        showConnectionButton.setOnAction(e -> handleViewConnections());

        findPathButton = new Button("Find Path");
        findPathButton.setDisable(true);
        findPathButton.setOnAction(e -> handleFindPath());


        HBox menuBox = new HBox(10, newPlaceButton, showConnectionButton, newConnectionButton, changeConnectionButton, findPathButton);
        menuBox.setPadding(new Insets(10));
        return menuBox;
    }

    private void handleFindPath() {
    }

    private void handleViewConnections() {
    }

    private void handleEditConnection() {
    }

    private void handleAddConnection() {
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

                    Circle city = new Circle(8, Color.RED);
                    city.setLayoutX(x);
                    city.setLayoutY(y);

                    Label label = new Label(name);
                    label.setLayoutX(x + 10);
                    label.setLayoutY(y - 5);

                    overlay.getChildren().addAll(city, label);
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

    private void handleAddPlaceAt() {

    }

    //Buttons
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
        launch(args); // 👈 detta startar JavaFX-applikationen korrekt
    }
}
