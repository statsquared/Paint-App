package com.example.paint1_1;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.util.Duration;
/**
 * Controller class that contains most of the paint application content
 */
public class PaintController {
    @FXML private Canvas mainCanvas, tempCanvas;
    @FXML private Slider widthSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Text lineWidthText;
    @FXML private Label colorLabel, timeLabel;
    @FXML private ToggleButton lineToggle, squareToggle, circleToggle, rectangleToggle, ovalToggle, diamondToggle, dashBox, eraserBox, drawBox;
    @FXML private ToggleButton triangleToggle, colorGrabToggle, polygonToggle, textToggle, selectToggle, pasteToggle, starToggle;
    @FXML private MenuItem undo, redo;
    @FXML private StackPane activeStackPane;
    @FXML private AnchorPane activeAnchorPane;
    private double startX, startY, endX, endY;
    private Image selectedArea;
    private String firstExtension;
    private int sides = 3, lineWidth = 2, countdownTime=5;
    private File currentFile;
    /**
     * Boolean that helps determine if the save popup is necessary upon the user attempting to close the canvas
     */
    public boolean isModified = false, isSelecting = false, isAutosaveRunning = false;
    private final Stack<WritableImage> undoStack = new Stack<>(), redoStack = new Stack<>();
    private GraphicsContext tempGC, mainGC;
    private ScheduledExecutorService autosaveService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Runnable drawListener=()-> {disabler(); if(drawBox.isSelected()){enableDraw();}};
    private final Runnable eraserListener=()-> {if (eraserBox.isSelected()){disabler(); clearExcept(null);
            dashBox.setSelected(false); enableErase();}else{drawListener.run();}};
    private final Timeline countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), _ ->
            {countdownTime--;timeLabel.setText(""+countdownTime);if (countdownTime <= 0) {countdownTime = 60;}}));

    /**
     * Function that runs upon startup that sets a lot of listeners in action and brings up my initial canvas
     */
    @FXML public void initialize() {
        try {BufferedWriter _ = new BufferedWriter(new FileWriter(
                "C:/Users/matth/IntelliJ/Projects/Paint1.1/src/main/resources/com/example/paint1_1/Action Log", false));
        }catch (IOException e) {throw new RuntimeException(e);}
        actionLogUpdate("Initialize Ran!");
        activeAnchorPane.getChildren().add(activeStackPane);
        activeStackPane.getChildren().addAll(mainCanvas, tempCanvas);
        mainGC = mainCanvas.getGraphicsContext2D();
        setIcons();
        startAutosave(); stopAutosave();
        mainGC.setFill(Color.WHITE);
        mainGC.fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        widthSlider.valueProperty().addListener((_,_,_)-> lineWidthAdjusted());
        colorPicker.valueProperty().addListener((_,_,_)-> colorLabelUpdate());
        polygonToggle.selectedProperty().addListener((_,_,_)-> {if(polygonToggle.isSelected()){actionLogUpdate("polygonToggle was selected.");
            clearExcept(polygonToggle); polygonAlert(); eraserListener.run();}else{eraserListener.run(); actionLogUpdate("polygonToggle was deselected.");}});
        starToggle.selectedProperty().addListener((_,_,_)-> {if(starToggle.isSelected()){clearExcept(starToggle); starAlert(); eraserListener.run();
            actionLogUpdate("starToggle was selected.");}else{eraserListener.run(); actionLogUpdate("starToggle was deselected.");}});
        textToggle.selectedProperty().addListener((_,_,_)-> {if(textToggle.isSelected()){clearExcept(textToggle); textToggleAlert();
            actionLogUpdate("textToggle was selected.");}else{eraserListener.run(); actionLogUpdate("textToggle was deselected.");}});
        selectToggle.selectedProperty().addListener((_,_,_)-> {if(selectToggle.isSelected()){clearExcept(selectToggle); enableSelection();
            actionLogUpdate("selectToggle was selected.");}else{actionLogUpdate("selectToggle was deselected.");}});
        pasteToggle.selectedProperty().addListener((_,_,_)-> {if(pasteToggle.isSelected()){clearExcept(pasteToggle); pasteSelectedArea();
            actionLogUpdate("pasteToggle was selected.");}else{actionLogUpdate("pasteToggle was deselected.");}});
        drawBox.selectedProperty().addListener((_,_,newValue)-> {if(newValue){eraserListener.run(); actionLogUpdate("DrawBox was selected.");
        }else{actionLogUpdate("DrawBox was deselected.");}});
        dashBox.selectedProperty().addListener((_,_,newValue)-> {if(newValue){eraserListener.run(); actionLogUpdate("DashBox was selected.");
        }else{actionLogUpdate("DashBox was deselected.");}});
        eraserBox.selectedProperty().addListener((_,_,newValue)-> {if(newValue){eraserListener.run(); actionLogUpdate("Eraser was selected.");
        }else{actionLogUpdate("Eraser was deselected.");}});
        colorGrabToggle.selectedProperty().addListener((_,_,newValue) -> {if (newValue){colorGrabButtonClick();
            actionLogUpdate("colorGrabToggle was selected.");}else{eraserListener.run(); actionLogUpdate("colorGrabToggle was deselected.");}});
        List<ToggleButton> toggleButtons = Arrays.asList(lineToggle, rectangleToggle, circleToggle, squareToggle, ovalToggle, diamondToggle, triangleToggle);
        for (ToggleButton toggleButton : toggleButtons) {toggleButton.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {clearExcept(toggleButton); eraserListener.run(); actionLogUpdate(toggleButton.getId()+" was selected.");
            }else{eraserListener.run(); actionLogUpdate(toggleButton.getId()+" was deselected.");}});}}

    /**
     * Turns all toggleButtons off except the toggleButton that was passed to the function
     * @param selectedToggle is the passed toggle that is left turned on
     */
    public void clearExcept(ToggleButton selectedToggle) {
        // List of all toggle buttons
        List<ToggleButton> toggleButtons = Arrays.asList(lineToggle, rectangleToggle, circleToggle, squareToggle, ovalToggle,
                diamondToggle, triangleToggle, textToggle, colorGrabToggle, polygonToggle, selectToggle, pasteToggle, starToggle);
        // Iterate through the list and deselect all buttons except the selected one
        for (ToggleButton toggleButton : toggleButtons) {
            if (toggleButton != selectedToggle) {
                toggleButton.setSelected(false);
                isModified = false;}}}

    /**
     * Creates a pop-up window that allows the user to input what text they can type into and put on the screen
     */
    public void textToggleAlert(){
        TextInputDialog dialog = new TextInputDialog("Enter your text here");
        dialog.setTitle("Add Text");
        dialog.setGraphic(null);
        dialog.setHeaderText("Enter the text you want to add to the canvas:");
        dialog.setContentText("Text:");
        enableDraw();
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::enableTextDrawing);}

    /**
     * Function that draws the users text on the screen
     * @param userText is a string of the user's text that is printed on the screen
     */
    public void enableTextDrawing(String userText) {
        Font font = new Font("Arial", 24);
        tempCanvas.toFront();
        tempGC = tempCanvas.getGraphicsContext2D();
        mainGC = mainCanvas.getGraphicsContext2D();
        tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        tempGC.drawImage(getCanvasSnapshot(), 0, 0);
        tempCanvas.setOnMousePressed(event -> {
            startX = event.getX();
            startY = event.getY();});
        tempCanvas.setOnMouseDragged(event -> {
            startX = event.getX();
            startY = event.getY();
            // Create a TextShape object and draw it on the temp canvas
            PaintShapes.TextShape textShape = new PaintShapes.TextShape(startX, startY, userText, colorPicker.getValue(), font);
            tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            tempGC.drawImage(getCanvasSnapshot(), 0, 0);
            textShape.draw(tempGC);});
        tempCanvas.setOnMouseReleased(event -> {
            startX = event.getX();
            startY = event.getY();
            PaintShapes.TextShape textShape = new PaintShapes.TextShape(startX, startY, userText, colorPicker.getValue(), font);
            saveState();
            textShape.draw(tempGC);
            textShape.draw(mainGC);
            isModified = true;
            if(dashBox.isSelected()){actionLogUpdate("Dashed text drawn.");}else{actionLogUpdate("Text drawn.");}});}

    /**
     * Function that sets up all keybinds for this program and has them in wait to be called
     */
    public void setupKeybinds() {
        Scene activeScene = mainCanvas.getScene();  // Get the scene of the canvas or any other node
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), () -> eraserBox.setSelected(true));
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), () -> drawBox.setSelected(true));
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), Platform::exit);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::saveButtonClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openFileButtonClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN), this::clearButtonClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN), this::pasteSelectedArea);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), this::rotateCanvasClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN), this::vertClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN), this::vertClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN), this::horzClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN), this::horzClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN), this::autosaveToggle);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1, KeyCombination.CONTROL_DOWN), this::aboutClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN), this::timeViewToggelClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::undoClick);
        activeScene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::redoClick);}

    /**
     * Uses a pre-existing text document and adds a new line of text describing a particular action that has occurred
     * @param addThisText is the text line that will be added to the text document
     */
    public void actionLogUpdate(String addThisText){
        String file = "C:/Users/matth/IntelliJ/Projects/Paint1.1/src/main/resources/com/example/paint1_1/Action Log";
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedTime = currentTime.format(formatter);
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))){
            writer.write(formattedTime+" [Action Log] "+addThisText);
            writer.newLine();
        }catch(Exception e){e.printStackTrace();}}

    /**
     * Undoes the previous action
     */
    @FXML protected void undoClick() {
        tempCanvas.toFront();
        undoRedoExtraction(undoStack, redoStack);
        eraserBox.setSelected(false);
        eraserListener.run();
        actionLogUpdate("An action was undone.");}

    /**
     * Redoes the previous action
      */
    @FXML protected void redoClick() {
        mainCanvas.toFront();
        undoRedoExtraction(redoStack, undoStack);
        eraserListener.run();
        actionLogUpdate("An action was redone.");}

    /**
     * Flips the canvas vertically
     */
    @FXML protected void vertClick(){
        mainCanvas.getGraphicsContext2D();
        Image snapshot = getCanvasSnapshot();
        mainGC.clearRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        mainGC.save();
        mainGC.scale(1,-1);
        mainGC.translate(0, -mainCanvas.getHeight());
        mainGC.drawImage(snapshot, 0, 0);
        mainGC.restore();
        mainCanvas.toFront();
        actionLogUpdate("The canvas was flipped vertically.");
    }

    /**
     * Flips the canvas horizontally
     */
    @FXML protected void horzClick(){
        mainCanvas.getGraphicsContext2D();
        Image snapshot = getCanvasSnapshot();
        mainGC.clearRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        mainGC.save();
        mainGC.scale(-1,1);
        mainGC.translate(-mainCanvas.getWidth(), 0);
        mainGC.drawImage(snapshot, 0, 0);
        mainGC.restore();
        mainCanvas.toFront();
        actionLogUpdate("The canvas was flipped horizontally.");
    }

    /**
     * Uploads image icons for each of the toggle buttons
     * @param toggleButton is the selected toggle button with which to give an icon
     * @param imagePath is the path to the icon's image
     */
    public void iconUpload(ToggleButton toggleButton, String imagePath){
        try{
            File file = new File(imagePath);
            Image icon = new Image(file.toURI().toString());
            ImageView toggleIcon = new ImageView(icon);
            toggleButton.setGraphic(toggleIcon);
            toggleButton.setContentDisplay(ContentDisplay.CENTER);
            //double initialWidth = Math.max(toggleButton.getWidth() - 70, 0);
            toggleIcon.setFitWidth(20);
            toggleIcon.setPreserveRatio(true);

        }catch(NullPointerException e){
            System.out.println("Image not found. Please check file path again for: "+toggleButton);
            e.printStackTrace();}}

    /**
     * Updates the width variable from the slider's current number
     */
    @FXML protected void lineWidthAdjusted(){
        //Connects the lineWidthSlider and changes the width every time it is updated
        lineWidth=(int) widthSlider.getValue();
        lineWidthText.setText(""+lineWidth);}

    /**
     * Function that turns off the autosave feature
     */
    public void stopAutosave(){
        autosaveService.shutdown();
        countdownTimeline.pause();
        isAutosaveRunning = false;}

    /**
     * Toggle that turns the autosave feature on and off
     */
    @FXML protected void autosaveToggle(){
        if(isAutosaveRunning){stopAutosave(); actionLogUpdate("Autosave was turned off."); countdownTime=0; timeLabel.setText("Stopped");
                }else{startAutosave(); actionLogUpdate("Autosave was turned on.");}}

    /**
     * Toggles whether the autosave timer can be seen or not
     */
    @FXML protected void timeViewToggelClick(){
        timeLabel.setVisible(!timeLabel.isVisible());
        actionLogUpdate("The autosave timer was turned on: "+timeLabel.isVisible());}

    /**
     * Saves the current canvas to a file
     */
    @FXML protected void saveButtonClick() {
        if(currentFile != null){
            System.out.println("Saving to file: " + currentFile.getAbsolutePath());
            saveImage(currentFile);
            countdownTime =60;
            actionLogUpdate("File was saved to: " + currentFile.getAbsolutePath());
        }else{
            //Failsafe that defaults to SaveAs if the file doesn't exist
            System.out.println("No current file set. Prompting to save as...");
            Platform.runLater(this::saveAsButtonClick);}
        isModified = false;}

    /**
     * Chooses the pixel color from the screen and sets the draw tool to be that color
     */
    @FXML protected void colorGrabButtonClick() {
        clearExcept(colorGrabToggle);
        disabler();
        mainCanvas.toFront();
        mainCanvas.setOnMousePressed(event -> {
            double x = event.getX();
            double y = event.getY();
            PixelReader pixelReader = mainCanvas.snapshot(null, null).getPixelReader();
            Color color = pixelReader.getColor((int) x, (int) y);
            colorPicker.setValue(color);
            colorGrabToggle.setSelected(false);
            actionLogUpdate("New color was picked.");});}

    /**
     * Clears the current canvases
     */
    @FXML protected void clearButtonClick(){
        //Method that clears the canvas and makes it blank
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setGraphic(null);
        alert.setTitle("Confirm Clear");
        alert.setHeaderText("Are you sure you want to clear the canvas?");
        alert.setContentText("All changes will be lost.");
        //Show the alert and wait for the user's response
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        //Check the user's response
        if (result == ButtonType.OK) {
            //User confirmed, proceed with clearing the canvas
            mainGC = mainCanvas.getGraphicsContext2D();
            saveState();
            mainGC.setFill(Color.WHITE);
            tempGC.setFill(Color.WHITE);
            mainGC.fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
            tempGC.fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
            isModified = false;
            actionLogUpdate("Canvas was cleared.");}}

    /**
     * Function that prompts the user to enter a number of sides with which they'll draw the polgyon with
     */
    public void polygonAlert(){
        TextInputDialog dialog = new TextInputDialog("5");
        dialog.setTitle("Polygon Sides");
        dialog.setGraphic(null);
        dialog.setHeaderText("Enter the number of sides for the polygon:");
        dialog.setContentText("Sides:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(sidesInput -> {
            try{
                int tempSides = Integer.parseInt(sidesInput);
                if(tempSides < 3){starToggle.setSelected(false);return;}
                sides = tempSides;
            }catch(NumberFormatException e){
                polygonToggle.setSelected(false);}});}

    /**
     * Function that prompts the user to enter a number of sides with which they'll draw the polgyon with
     */
    public void starAlert(){
        TextInputDialog dialog = new TextInputDialog("5");
        dialog.setTitle("Input Points");
        dialog.setGraphic(null);
        dialog.setHeaderText("Enter the number of points for the star:");
        dialog.setContentText("Points:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(sidesInput -> {
            try{
                int points = Integer.parseInt(sidesInput);
                if(points < 5){starToggle.setSelected(false);return;}
                sides = points;
            }catch(NumberFormatException e){
                starToggle.setSelected(false);}});}

    /**
     * Option to be able to resize the canvas and change where can be drawn on
     */
    @FXML protected void resizeCanvasClick(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Resize Canvas");
        alert.setGraphic(null);
        alert.setHeaderText("Enter new dimensions for the canvas:");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        TextField widthField = new TextField();
        TextField heightField = new TextField();
        widthField.setPromptText("Width");
        heightField.setPromptText("Height");
        grid.addRow(0, new javafx.scene.control.Label("Width:"), widthField);
        grid.addRow(1, new javafx.scene.control.Label("Height:"), heightField);
        alert.getDialogPane().setContent(grid);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String widthText = widthField.getText();
                String heightText = heightField.getText();
                try {
                    mainCanvas.setWidth(Double.parseDouble(widthText));
                    mainCanvas.setHeight(Double.parseDouble(heightText));
                    tempCanvas.setWidth(Double.parseDouble(widthText));
                    tempCanvas.setHeight(Double.parseDouble(heightText));
                    actionLogUpdate("Canvas was resized");
                }catch(NumberFormatException e) {
                    Alert alert2 = new Alert(Alert.AlertType.ERROR);
                    alert2.setTitle("Error");
                    alert2.setHeaderText(null);
                    alert2.setContentText("Invalid input. Please enter valid numbers for width and height.");
                    alert2.showAndWait();
                    resizeCanvasClick();}}});}

    /**
     * Rotates the canvas the amount of degrees entered by the user
     */
    public void rotateCanvasClick() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rotate Canvas");
        dialog.setHeaderText("Enter Degrees to Rotate");
        dialog.setContentText("Please enter a number of degrees:");
        dialog.showAndWait().ifPresent(degreesInput -> {
            try {
                int degrees = Integer.parseInt(degreesInput);
                WritableImage snapshot = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
                mainCanvas.snapshot(null, snapshot);
                mainGC.clearRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
                mainGC.save();
                mainGC.translate(mainCanvas.getWidth() / 2, mainCanvas.getHeight() / 2);
                mainGC.rotate(degrees);
                mainGC.translate(-mainCanvas.getWidth() / 2, -mainCanvas.getHeight() / 2);
                mainGC.setFill(Color.WHITE);
                mainGC.fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
                mainGC.drawImage(snapshot, 0, 0);
                mainGC.restore(); // Restore the state after rotation
                actionLogUpdate("Canvas was rotated "+degrees+".");
            }catch(NumberFormatException e) {
                // Show an error alert if the input is not a valid number
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText("Input Error");
                alert.setContentText("Please enter a valid number.");
                alert.showAndWait();}});}

    /**
     * Creates an about Popup that has some fun information in it
     */
    @FXML protected void aboutClick() {
        //Brings up the About Alert that has been customized specifically for the user by ME :)
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setGraphic(null);
        alert.setTitle("About");
        alert.setHeaderText("Pain(t) v1.1.0");
        alert.getDialogPane().setPrefSize(500, 500);
        alert.setContentText("""
                A newer rendition of Microsoft Paint. It is not necessarily better though...
                
                Developed by: Matt
                
                
                Fun Java Pun:
                Why did the Java programmer quit his job?
                Answer: Because he didn't get arrays.""");
        ButtonType closeButton = new ButtonType("Close");  // Custom close button instead of 'ok'
        alert.getButtonTypes().setAll(closeButton); //sets all buttons to function as a close button
        alert.showAndWait();}

    /**
     * Prompts the user to save the current canvas as a new file
     */
    @FXML public void saveAsButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save New File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG File", "*.png"),
                new FileChooser.ExtensionFilter("JPG File", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP File", "*.bmp"));
        //Starts the stage initialization so that the fileChooser appears visibly for the user
        Stage stage = (Stage) mainCanvas.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        //double checks to make sure there is a file that can be saved, and then saves it
        if (file != null) {currentFile = file; saveImage(file);isModified = false; actionLogUpdate("File saved as another file.");}}

    /**
     * Prompts the user to save the current canvas under a new file type
     */
    @FXML public void saveAsNewButtonClick() {
        firstExtension = getFileExtension(currentFile.getName()).toLowerCase();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As Different File Type");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG File", "*.png"),
                new FileChooser.ExtensionFilter("JPG File", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP File", "*.bmp"));
        Stage stage = (Stage) mainCanvas.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        String newExtension = getFileExtension(file.getName()).toLowerCase();
        currentFile = file;
        saveImage(file);
        actionLogUpdate("File saved as new.");
        isModified = false;
        if(Objects.equals(firstExtension, newExtension)){System.out.println("Image type remains the same");
        }else{System.out.println("Image type has changed from " + firstExtension + " to " + newExtension + " and may result in data loss.");}}

    /**
     * Takes an image and draws it appropriately onto the canvas based on the different sizing and aspect ratios
     * @param image is an image most likely in the form of a screenshot that will be drawn onto the canvas
     */
    public void drawImageOnCanvas(Image image) {
        //Gets the current content on the canvas, clears it, then draws the new image
        GraphicsContext gc = mainCanvas.getGraphicsContext2D();
        double canvasWidth = mainCanvas.getWidth();
        double canvasHeight = mainCanvas.getHeight();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double aspectRatio = imageWidth / imageHeight;
        double newWidth;
        double newHeight;
        double xOffset = 0;
        if(canvasWidth > imageWidth && canvasHeight > imageHeight){
            //image is smaller than the canvas and therefore shouldn't be stretched larger
            newHeight = imageHeight;
            newWidth = imageWidth;
            xOffset = (canvasWidth - newWidth) / 2;
        }else if(canvasWidth / canvasHeight > aspectRatio){
            //Canvas is wider relative to its height than the image
            newHeight = canvasHeight;
            newWidth = newHeight * aspectRatio;
            xOffset = (canvasWidth - newWidth) / 2;
        }else{
            //Canvas is taller relative to its width than the image
            newWidth = canvasWidth;
            newHeight = newWidth / aspectRatio;}
        saveState();
        gc.drawImage(image, xOffset, 0, newWidth, newHeight);
        isModified = true;}

    /**
     * Function that can be used to open a file selected by the user to be printed on the screen
     */
    @FXML protected void openFileButtonClick(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp"));
        //Scene stage is created and the file chooser is opened
        Stage stage = (Stage) mainCanvas.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            currentFile = selectedFile;
            Image image = new Image(selectedFile.toURI().toString());
            drawImageOnCanvas(image);
            firstExtension = getFileExtension(selectedFile.getName()).toLowerCase();
            System.out.println(firstExtension);
            isModified = true;
            actionLogUpdate("Image opened from file: "+currentFile.getName());}}

    /**
     * Takes a snapshot of the current canvas and returns it
     * @return a snapshot of the current canvas
     */
    public Image getCanvasSnapshot() {
        SnapshotParameters params = new SnapshotParameters();
        WritableImage snapshot = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        mainCanvas.snapshot(params, snapshot);
        return snapshot;}

    /**
     * Turns off all mouse functions so that a specific function can perform a specific task without messup
     */
    public void disabler(){
        mainCanvas.setOnMousePressed(null);
        mainCanvas.setOnMouseDragged(null);
        mainCanvas.setOnMouseReleased(null);
        tempCanvas.setOnMousePressed(null);
        tempCanvas.setOnMouseDragged(null);
        tempCanvas.setOnMouseReleased(null);
        mainCanvas.toFront();}

    /**
     * Updates the color label with the correct rgb/hexadecimal values
     */
    public void colorLabelUpdate(){
        Color c = colorPicker.getValue();
        colorLabel.setText("#"+Integer.toHexString(colorPicker.getValue().hashCode())+
            " "+(int)(c.getRed()*255)+"/"+(int)(c.getGreen()*255)+"/"+(int)(c.getBlue()*255));}

    /**
     * Allows the user to mouse over certain segments of the canvas to either save or move them
     */
    public void enableSelection() {
        tempGC = tempCanvas.getGraphicsContext2D();
        mainGC = mainCanvas.getGraphicsContext2D();
        tempCanvas.toFront();
        tempGC.drawImage(getCanvasSnapshot(), 0, 0);
        tempCanvas.setOnMousePressed(event -> {
            startX = event.getX();
            startY = event.getY();
            isSelecting = true;});
        tempCanvas.setOnMouseDragged(event -> {
            if(isSelecting){
                tempGC = tempCanvas.getGraphicsContext2D();
                endX = event.getX();
                endY = event.getY();
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0);
                tempGC.setStroke(Color.BLUE);
                tempGC.setLineDashes(10);
                tempGC.setLineWidth(1);
                tempGC.strokeRect(startX, startY, endX-startX, endY-startY);}});
        tempCanvas.setOnMouseReleased(_ -> {
            tempGC = tempCanvas.getGraphicsContext2D();
            tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            tempGC.drawImage(getCanvasSnapshot(), 0, 0);
            isSelecting = false;
            selectToggle.setSelected(false);
            copySelectedArea();
            saveState();});}

    /**
     * Allows the user to copy the area inside a selection to then use later
     */
    public void copySelectedArea() {
        if (endX-startX != 0 && endY-startY != 0) {
            tempGC = tempCanvas.getGraphicsContext2D();
            mainGC = mainCanvas.getGraphicsContext2D();
            SnapshotParameters params = new SnapshotParameters();
            params.setViewport(new Rectangle2D(startX, startY, endX-startX, endY-startY));
            selectedArea = tempCanvas.snapshot(params, null);
            moveSelectedArea();}}

    /**
     * Allows the user to paste the copied image onto the canvas
     */
    public void pasteSelectedArea() {
        mainGC = mainCanvas.getGraphicsContext2D();
        tempGC = tempCanvas.getGraphicsContext2D();
        if (selectedArea != null) {
            tempCanvas.setOnMousePressed(event -> {
                startX = event.getX();
                startY = event.getY();
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0);});
            tempCanvas.setOnMouseDragged(event -> {
                tempGC = tempCanvas.getGraphicsContext2D();
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0); // Keep the background intact
                tempGC.drawImage(selectedArea, event.getX(), event.getY());});
            tempCanvas.setOnMouseReleased(event -> {
                tempGC = tempCanvas.getGraphicsContext2D();
                mainGC.drawImage(selectedArea, event.getX(), event.getY());
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0);
                disabler();
                eraserListener.run();
                saveState();});}}

    /**
     * Allows the user to move the selected area to a different part of the canvas
     */
    public void moveSelectedArea(){
        if(selectedArea != null){
            mainGC.clearRect(startX, startY, endX-startX, endY-startY);
            tempCanvas.setOnMousePressed(event -> {
                startX = event.getX();
                startY = event.getY();
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0);});
            tempCanvas.setOnMouseDragged(event -> {
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0); // Keep the background intact
                tempGC.drawImage(selectedArea, event.getX(), event.getY());});
            tempCanvas.setOnMouseReleased(event -> {
                mainGC.drawImage(selectedArea, event.getX(), event.getY());
                saveState();});}}

    /**
     * Turns on the erase feature and makes sure that drawing is off
     */
    public void enableErase(){
        GraphicsContext eraseGC = mainCanvas.getGraphicsContext2D();
        mainCanvas.setOnMousePressed(event -> {
            saveState();
            startX = event.getX();
            startY = event.getY();
            eraseGC.setLineWidth(lineWidth);
            eraseGC.setStroke(Color.WHITE);
            eraseGC.strokeLine(startX, startY, startX, startY);
            isModified = true;});
        mainCanvas.setOnMouseDragged(event -> {
            saveState();
            endX = event.getX();
            endY = event.getY();
            //Sets color and width
            eraseGC.setLineWidth(lineWidth);
            eraseGC.setStroke(Color.WHITE);
            //Erases the line and resets the coordinates if another line needs to be erased
            eraseGC.strokeLine(startX, startY, endX, endY);
            startX = endX;
            startY = endY;
            isModified = true;
            mainCanvas.setOnMouseReleased(_ -> actionLogUpdate("Canvas was erased by freehand "));});}

    /**
     * Takes variables that will impact every type of drawing and updates those for both active canvases
     */
    private void updateDraw(){
        mainGC.setLineWidth(lineWidth);
        tempGC.setLineWidth(lineWidth);
        mainGC.setLineCap(StrokeLineCap.ROUND);
        tempGC.setLineCap(StrokeLineCap.ROUND);
        mainGC.setStroke(colorPicker.getValue());
        tempGC.setStroke(colorPicker.getValue());}

    /**
     * Turns on the drawing feature and performs all the different types of drawing
     */
    public void enableDraw(){
        mainCanvas.toFront();
        mainGC = mainCanvas.getGraphicsContext2D();
        tempGC = tempCanvas.getGraphicsContext2D();
        if (dashBox.isSelected()) {mainGC.setLineDashes(10); tempGC.setLineDashes(10);}else{mainGC.setLineDashes(0); tempGC.setLineDashes(0);}
        if (!lineToggle.isSelected() && !rectangleToggle.isSelected() && !circleToggle.isSelected()
        && !squareToggle.isSelected() && !ovalToggle.isSelected() && !diamondToggle.isSelected()
        && !triangleToggle.isSelected() && !textToggle.isSelected() && !polygonToggle.isSelected()
        && !selectToggle.isSelected() && !starToggle.isSelected()){
            canvasDrawPressed(mainCanvas);
            canvasDrawPressed(tempCanvas);
            canvasDrawDragged(mainCanvas);
            canvasDrawDragged(tempCanvas);
        }else{
            tempCanvas.toFront();
            tempGC.drawImage(getCanvasSnapshot(), 0, 0);
            tempCanvas.setOnMousePressed(event -> {
                startX = event.getX();
                startY = event.getY();
                updateDraw();
                tempGC.drawImage(getCanvasSnapshot(), 0, 0);});
            tempCanvas.setOnMouseDragged(event -> {
                tempGC.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                tempGC.drawImage(getCanvasSnapshot(), 0, 0);
                endX = event.getX();
                endY = event.getY();
                PaintShapes.Shape shape;
                shape = shapeWork(startX, startY, endX, endY, sides, lineWidth);
                if(shape != null) {shape.draw(tempGC);}
            });
            tempCanvas.setOnMouseReleased(event -> {
                endX = event.getX();
                endY = event.getY();
                PaintShapes.Shape shape;
                shape = shapeWork(startX, startY, endX, endY, sides, lineWidth);
                if(shape!=null){saveState(); shape.draw(mainGC); if(dashBox.isSelected()){actionLogUpdate(
                        "Dashed "+shape.getClass().getSimpleName()+" drawn.");}else{actionLogUpdate(shape.getClass().getSimpleName()+" drawn.");}}
                isModified = true;});}}

    private PaintShapes.Shape shapeWork(double startX, double startY, double endX, double endY, int sides, int lineWidth){
        PaintShapes.Shape shape = null;
        if(lineToggle.isSelected()) {shape = new PaintShapes.LineShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (rectangleToggle.isSelected()) {shape = new PaintShapes.RectangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (circleToggle.isSelected()) {shape = new PaintShapes.CircleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (ovalToggle.isSelected()) {shape = new PaintShapes.OvalShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (triangleToggle.isSelected()) {shape = new PaintShapes.TriangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (diamondToggle.isSelected()) {shape = new PaintShapes.DiamondShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (squareToggle.isSelected()) {shape = new PaintShapes.SquareShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
        }else if (polygonToggle.isSelected()) {shape = new PaintShapes.PolygonShape(startX, startY, endX, endY, sides, colorPicker.getValue(), lineWidth);
        }else if (starToggle.isSelected()) {shape = new PaintShapes.StarShape(startX, startY, endX, endY, sides, colorPicker.getValue(), lineWidth);}
        return shape;}

    private void canvasDrawDragged(Canvas canvas2) {
        canvas2.setOnMouseDragged(event -> {
            endX = event.getX();
            endY = event.getY();
            updateDraw();
            mainGC.strokeLine(startX, startY, endX, endY);
            tempGC.strokeLine(startX, startY, endX, endY);
            startX = endX;
            startY = endY;
            isModified = true;
            canvas2.setOnMouseReleased(_ -> {
                redoStack.clear();
                if(dashBox.isSelected()){actionLogUpdate("Dashed freehand line drawn.");}else{actionLogUpdate("Freeehand line drawn.");}
                updateUndoRedoButtons();});});}

    private void canvasDrawPressed(Canvas canvas1) {
        canvas1.setOnMousePressed(event -> {
            saveState();
            updateDraw();
            startX = event.getX();
            startY = event.getY();
            mainGC.strokeLine(startX, startY, startX, startY);
            tempGC.strokeLine(startX, startY, startX, startY);});
        canvas1.setOnMouseReleased(_ -> actionLogUpdate("Dot drawn."));}

    private String getFileExtension(String fileName) {
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0){
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }else{return "";}}

    private void saveState() {
        WritableImage snapshot = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        mainCanvas.snapshot(null, snapshot);
        if (undoStack.size() >= 100) {undoStack.removeFirst();}  // Remove the oldest state
        undoStack.push(snapshot);
        redoStack.clear();
        isModified = true;}

    private void saveImage(File file){
        Image snapshot = getCanvasSnapshot();
        if (snapshot != null) {
            try{
                //Gets the file extension to determine the correct format
                String extension = getFileExtension(file.getName()).toLowerCase();
                BufferedImage bufferedImage = convertToBufferedImage(snapshot, extension);
                //Ensures we are saving in a supported format
                if(extension.equals("png") || extension.equals("jpg") || extension.equals("bmp")) {
                    ImageIO.write(bufferedImage, extension, file);
                    System.out.println("Image saved successfully.");
                }else{System.out.println("Unsupported file format.");}
            }catch (IOException e) {e.printStackTrace();}
            isModified = false;}}

    private BufferedImage convertToBufferedImage(Image img, String format){
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();
        BufferedImage bufferedImage;
        if (format.equals("jpg") || format.equals("bmp")) {bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }else{bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);}
        PixelReader pixelReader = img.getPixelReader();
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Color fxColor = pixelReader.getColor(x, y);
                java.awt.Color awtColor;
                if(format.equals("jpg") || format.equals("bmp")){
                    awtColor = new java.awt.Color((float) fxColor.getRed(), (float) fxColor.getGreen(), (float) fxColor.getBlue());
                    bufferedImage.setRGB(x, y, awtColor.getRGB());
                }else{
                    awtColor = new java.awt.Color((float) fxColor.getRed(), (float) fxColor.getGreen(), (float) fxColor.getBlue(), (float) fxColor.getOpacity());
                    bufferedImage.setRGB(x, y, awtColor.getRGB());}}}
        return bufferedImage;}

    private void updateUndoRedoButtons() {
        undo.setDisable(undoStack.isEmpty());
        redo.setDisable(redoStack.isEmpty());}

    private WritableImage getCurrentCanvasState() {
        WritableImage writableImage = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        mainCanvas.snapshot(null, writableImage);  // Capture the current state of the canvas
        return writableImage;  // Return the WritableImage
    }

    private void undoRedoExtraction(Stack<WritableImage> fromStack, Stack<WritableImage> toStack) {
        if (!fromStack.isEmpty()) {
            // Push the current state onto the toStack
            toStack.push(getCurrentCanvasState());
            // Limit the toStack size to 5 entries
            if (toStack.size() > 100) {
                toStack.removeFirst();}  // Remove the oldest state
            // Pop the last state from the fromStack and restore it
            WritableImage nextState = fromStack.pop();
            restoreCanvasState(nextState);
            isModified = true;}  // Mark as modified
        updateUndoRedoButtons();}

    private void restoreCanvasState(WritableImage image) {
        GraphicsContext gc = mainCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());  // Clear the canvas
        gc.drawImage(image, 0, 0);  // Draw the saved image onto the canvas
    }

    private void startAutosave() {
        if (isAutosaveRunning) {return;}
        autosaveService = Executors.newScheduledThreadPool(1);
        autosaveService.scheduleAtFixedRate(() -> Platform.runLater(() -> {saveButtonClick(); countdownTime = 60; autosaveNotification();}), 5, 60, TimeUnit.SECONDS);
        startCountdown();
        isAutosaveRunning = true;}

    private void autosaveNotification(){
        Popup popup = new Popup();
        Label label = new Label("Autosaved");
        label.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-padding: 5px;");
        StackPane pane = new StackPane(label);
        pane.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
        pane.setAlignment(Pos.CENTER);
        popup.getContent().add(pane);
        Scene scene = activeStackPane.getScene();
        if(scene != null) {popup.show(scene.getWindow());}
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), _ -> popup.hide()));
        timeline.play();}

    private void startCountdown() {
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();}

    private void setIcons(){
            String base = "C:/Users/matth/CS250 (ThisPC)/Button Icons/";
            iconUpload(circleToggle, base + "circle.png");
            iconUpload(colorGrabToggle, base + "color.png");
            iconUpload(diamondToggle, base + "diamond.png");
            iconUpload(lineToggle, base + "line.png");
            iconUpload(ovalToggle, base + "oval.png");
            iconUpload(pasteToggle, base + "paste.png");
            iconUpload(polygonToggle, base + "polygon.png");
            iconUpload(rectangleToggle, base + "rectangle.png");
            iconUpload(selectToggle, base + "select.png");
            iconUpload(squareToggle, base + "square.png");
            iconUpload(starToggle, base + "star.png");
            iconUpload(textToggle, base + "text.png");
            iconUpload(triangleToggle, base + "triangle.png");
    }}