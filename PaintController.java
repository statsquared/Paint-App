package com.example.paint1_1;
//import files for the Controller class
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;

//Controller class that holds most of the information that is visually displayed in the window
public class PaintController {
    //Linking the xml/SceneBuilder with JavaFX objects/properties
    @FXML private Canvas mainCanvas;
    @FXML private Canvas tempCanvas;
    @FXML private Slider widthSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Text lineWidthText;
    @FXML private CheckBox drawBox;
    @FXML private CheckBox eraserBox;
    @FXML private CheckBox dashBox;
    @FXML private Label colorLabel;
    @FXML private ToggleButton lineToggle;
    @FXML private ToggleButton squareToggle;
    @FXML private ToggleButton circleToggle;
    @FXML private ToggleButton rectangleToggle;
    @FXML private ToggleButton ovalToggle;
    @FXML private ToggleButton diamondToggle;
    @FXML private ToggleButton triangleToggle;
    @FXML private ToggleButton colorGrabToggle;
    @FXML private ToggleButton polygonToggle;
    @FXML private ToggleButton textToggle;
    @FXML private ToggleButton selectToggle;
    @FXML private ToggleButton pasteToggle;
    @FXML private MenuItem undo;
    @FXML private MenuItem redo;
    private double startX, startY, endX, endY;
    int lineWidth = 2;
    Image selectedArea;
    private int sides = 3;
    private File currentFile;
    public boolean isModified = false;
    private final Stack<WritableImage> undoStack = new Stack<>();
    private final Stack<WritableImage> redoStack = new Stack<>();
    private GraphicsContext tempGC;
    private GraphicsContext mainGC;
    boolean isSelecting = false;
    //drawListener and eraserListener are variables that contains functions that can be run. They were created to prevent code replication going forwards
    Runnable drawListener=()-> {disabler(); if(drawBox.isSelected()){enableDraw();}};
    Runnable eraserListener=()-> {if (eraserBox.isSelected()){disabler();enableErase();}else{drawListener.run();}};

    @FXML public void initialize() {
        mainGC = mainCanvas.getGraphicsContext2D();
        mainGC.setFill(Color.WHITE);
        mainGC.fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        widthSlider.valueProperty().addListener((_,_,_)-> lineWidthAdjusted());
        colorPicker.valueProperty().addListener((_,_,_)-> colorLabelUpdate());
        polygonToggle.selectedProperty().addListener((_,_,_)-> {if(polygonToggle.isSelected()){clearExcept(polygonToggle); polygonAlert(); eraserListener.run();
                }else{eraserListener.run();}});
        textToggle.selectedProperty().addListener((_,_,_)-> {if(textToggle.isSelected()){clearExcept(textToggle); textToggleAlert();
                }else{eraserListener.run();}});
        selectToggle.selectedProperty().addListener((_,_,_)-> {if(selectToggle.isSelected()){clearExcept(selectToggle); enableSelection();}});
        drawBox.selectedProperty().addListener((_,_,_)-> drawListener.run());
        dashBox.selectedProperty().addListener((_,_,_)-> eraserListener.run());
        eraserBox.selectedProperty().addListener((_,_,_)-> eraserListener.run());
        colorGrabToggle.selectedProperty().addListener((_,_,newValue) -> {if (newValue){clearExcept(colorGrabToggle); disabler(); colorGrabButtonClick();}else{eraserListener.run();}});
        List<ToggleButton> toggleButtons = Arrays.asList(lineToggle, rectangleToggle, circleToggle, squareToggle, ovalToggle, diamondToggle, triangleToggle);
        for (ToggleButton toggleButton : toggleButtons) {toggleButton.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {clearExcept(toggleButton); eraserListener.run();}else{eraserListener.run();}});}}

    public void clearExcept(ToggleButton selectedToggle) {
        // List of all toggle buttons
        List<ToggleButton> toggleButtons = Arrays.asList(lineToggle, rectangleToggle, circleToggle, squareToggle, ovalToggle,
                diamondToggle, triangleToggle, textToggle, colorGrabToggle, polygonToggle, selectToggle);
        // Iterate through the list and deselect all buttons except the selected one
        for (ToggleButton toggleButton : toggleButtons) {
            if (toggleButton != selectedToggle) {
                toggleButton.setSelected(false);
                isModified = false;}}}

    public void textToggleAlert(){
        TextInputDialog dialog = new TextInputDialog("Enter your text here");
        dialog.setTitle("Add Text");
        dialog.setGraphic(null);
        dialog.setHeaderText("Enter the text you want to add to the canvas:");
        dialog.setContentText("Text:");
        enableDraw();
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::enableTextDrawing);}

    public void enableTextDrawing(String userText) {
        Font font = new Font("Arial", 24);
        tempCanvas.setHeight(mainCanvas.getHeight());
        tempCanvas.setWidth(mainCanvas.getWidth());
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
            isModified = true;});}


    public void setupKeyboardShortcuts() {
        Scene scene = mainCanvas.getScene();  // Get the scene of the canvas or any other node
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::saveButtonClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openFileButtonClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), this::clearButtonClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), Platform::exit);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::undoClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::redoClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN), this::pasteSelectedArea);}

    private void saveState() {
        WritableImage snapshot = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        mainCanvas.snapshot(null, snapshot);
        if (undoStack.size() >= 100) {undoStack.removeFirst();}  // Remove the oldest state
        undoStack.push(snapshot);
        redoStack.clear();
        isModified = true;}  // Clear redo stack on new action

    private void updateUndoRedoButtons() {
        undo.setDisable(undoStack.isEmpty());
        redo.setDisable(redoStack.isEmpty());}

    private WritableImage getCurrentCanvasState() {
        WritableImage writableImage = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        mainCanvas.snapshot(null, writableImage);  // Capture the current state of the canvas
        return writableImage;  // Return the WritableImage
    }

    private void restoreCanvasState(WritableImage image) {
        GraphicsContext gc = mainCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());  // Clear the canvas
        gc.drawImage(image, 0, 0);  // Draw the saved image onto the canvas
    }

    @FXML protected void undoClick() {
        tempCanvas.setHeight(0);
        tempCanvas.setWidth(0);
        undoRedoExtraction(undoStack, redoStack);
        eraserBox.setSelected(false);
        eraserListener.run();}

    @FXML protected void redoClick() {
        tempCanvas.setHeight(0);
        tempCanvas.setWidth(0);
        undoRedoExtraction(redoStack, undoStack);
        eraserListener.run();}

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

    //Connects the lineWidthSlider and changes the width every time it is updated
    @FXML protected void lineWidthAdjusted(){
        lineWidth=(int) widthSlider.getValue();
        lineWidthText.setText(""+lineWidth);}

    //Save button that is connected with the FXML
    @FXML protected void saveButtonClick() {
        if(currentFile != null){
            System.out.println("Saving to file: " + currentFile.getAbsolutePath());
            saveImage(currentFile);
        }else{
            //Failsafe that defaults to SaveAs if the file doesn't exist
            System.out.println("No current file set. Prompting to save as...");
            saveAsButtonClick();}
        isModified = false;}

    @FXML protected void colorGrabButtonClick() {
        mainCanvas.setOnMousePressed(event -> {
            double x = event.getX();
            double y = event.getY();
            PixelReader pixelReader = mainCanvas.snapshot(null, null).getPixelReader();
            Color color = pixelReader.getColor((int) x, (int) y);
            colorPicker.setValue(color);
            colorGrabToggle.setSelected(false);});}

    //Method that clears the canvas and makes it blank
    @FXML protected void clearButtonClick(){
        //Create a confirmation alert
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
            isModified = false;}}

    public void polygonAlert(){
        TextInputDialog dialog = new TextInputDialog("5");
        dialog.setTitle("Polygon Sides");
        dialog.setGraphic(null);
        dialog.setHeaderText("Enter the number of sides for the polygon:");
        dialog.setContentText("Sides:");

        // Get the number of sides entered by the user
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(sidesInput -> {
            try{
                int tempSides = Integer.parseInt(sidesInput);
                if(tempSides < 3){
                    polygonToggle.setSelected(false);
                    return;}
                // Pass the number of sides to the drawing function
                sides = tempSides;
            }catch(NumberFormatException e){
                polygonToggle.setSelected(false);}});}

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
                }catch(NumberFormatException e) {
                    Alert alert2 = new Alert(Alert.AlertType.ERROR);
                    alert2.setTitle("Error");
                    alert2.setHeaderText(null);
                    alert2.setContentText("Invalid input. Please enter valid numbers for width and height.");
                    alert2.showAndWait();
                    resizeCanvasClick();}}});}

    //Brings up the About Alert that has been customized specifically for the user by ME :)
    @FXML protected void aboutClick() {
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

    //SaveAs Button that is also connected to FXML
    //This opens the file system, allows the user to rename and select the file type, and saves as a new file
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
        if (file != null) {currentFile = file; saveImage(file); isModified = false;}}

    //Draws an image on the canvas by first clearing the canvas if necessary, then it draws the image
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

    //This is how the user can open a file, with many supported file types
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
            isModified = true;}}

    //Captures the screenshot needed to save canvases as a file
    public Image getCanvasSnapshot() {
        SnapshotParameters params = new SnapshotParameters();
        WritableImage snapshot = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        mainCanvas.snapshot(params, snapshot);
        return snapshot;}

    //This method saves the data as a screenshot
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

    //Converts the stored snapshot image as a buffered image
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

    //Turns draw mode off by making the clicks and drags do nothing
    public void disabler(){
        mainCanvas.setOnMousePressed(null);
        mainCanvas.setOnMouseDragged(null);
        mainCanvas.setOnMouseReleased(null);
        tempCanvas.setWidth(0);
        tempCanvas.setHeight(0);}

    public void colorLabelUpdate(){
        Color c = colorPicker.getValue();
        colorLabel.setText("#"+Integer.toHexString(colorPicker.getValue().hashCode())+
            " "+(int)(c.getRed()*255)+"/"+(int)(c.getGreen()*255)+"/"+(int)(c.getBlue()*255));}

    public void enableSelection() {
        tempGC = tempCanvas.getGraphicsContext2D();
        mainGC = mainCanvas.getGraphicsContext2D();
        tempCanvas.setHeight(mainCanvas.getHeight());
        tempCanvas.setWidth(mainCanvas.getWidth());
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

    public void copySelectedArea() {
        if (endX-startX != 0 && endY-startY != 0) {
            tempGC = tempCanvas.getGraphicsContext2D();
            mainGC = mainCanvas.getGraphicsContext2D();
            SnapshotParameters params = new SnapshotParameters();
            params.setViewport(new Rectangle2D(startX, startY, endX-startX, endY-startY));
            selectedArea = tempCanvas.snapshot(params, null);
            moveSelectedArea();}}

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


    //Erases on the canvas whatever the user clicks
    public void enableErase(){
        GraphicsContext eraseGC = mainCanvas.getGraphicsContext2D();
        //Captures either a single click or a mouse drag
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
            isModified = true;});}

    private void updateDraw(){
        mainGC.setLineWidth(lineWidth);
        tempGC.setLineWidth(lineWidth);
        mainGC.setLineCap(StrokeLineCap.ROUND);
        tempGC.setLineCap(StrokeLineCap.ROUND);
        mainGC.setStroke(colorPicker.getValue());
        tempGC.setStroke(colorPicker.getValue());}

    public void enableDraw(){
        tempCanvas.setHeight(0);
        tempCanvas.setWidth(0);
        mainGC = mainCanvas.getGraphicsContext2D();
        tempGC = tempCanvas.getGraphicsContext2D();
        if (dashBox.isSelected()) {mainGC.setLineDashes(10); tempGC.setLineDashes(10);}else{mainGC.setLineDashes(0); tempGC.setLineDashes(0);}
        //Freehand drawing mode (if no shape toggle is selected)
        if (!lineToggle.isSelected() && !rectangleToggle.isSelected() && !circleToggle.isSelected()
        && !squareToggle.isSelected() && !ovalToggle.isSelected() && !diamondToggle.isSelected()
        && !triangleToggle.isSelected() && !textToggle.isSelected() && !polygonToggle.isSelected()) {
            canvasDrawPressed(mainCanvas);
            canvasDrawPressed(tempCanvas);
            canvasDrawDragged(mainCanvas);
            canvasDrawDragged(tempCanvas);
        }else{
            // Shape drawing mode (if a shape toggle is selected)
            tempCanvas.setHeight(mainCanvas.getHeight());
            tempCanvas.setWidth(mainCanvas.getWidth());
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
                PaintShapes.Shape shape = null;
                //Check for the selected shape and instantiate the appropriate class
                if(lineToggle.isSelected()) {shape = new PaintShapes.LineShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (rectangleToggle.isSelected()) {shape = new PaintShapes.RectangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (circleToggle.isSelected()) {shape = new PaintShapes.CircleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (ovalToggle.isSelected()) {shape = new PaintShapes.OvalShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (triangleToggle.isSelected()) {shape = new PaintShapes.TriangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (diamondToggle.isSelected()) {shape = new PaintShapes.DiamondShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (squareToggle.isSelected()) {shape = new PaintShapes.SquareShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (polygonToggle.isSelected()) {shape = new PaintShapes.PolygonShape(startX, startY, endX, endY, sides, colorPicker.getValue(), lineWidth);}
                if(shape != null) {shape.draw(tempGC);}
            });
            tempCanvas.setOnMouseReleased(event -> {
                endX = event.getX();
                endY = event.getY();
                PaintShapes.Shape shape = null;
                //Check for the selected shape and instantiate the appropriate class
                if(lineToggle.isSelected()) {shape = new PaintShapes.LineShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (rectangleToggle.isSelected()) {shape = new PaintShapes.RectangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (circleToggle.isSelected()) {shape = new PaintShapes.CircleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (ovalToggle.isSelected()) {shape = new PaintShapes.OvalShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (triangleToggle.isSelected()) {shape = new PaintShapes.TriangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (diamondToggle.isSelected()) {shape = new PaintShapes.DiamondShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (squareToggle.isSelected()) {shape = new PaintShapes.SquareShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (polygonToggle.isSelected()) {shape = new PaintShapes.PolygonShape(startX, startY, endX, endY, sides, colorPicker.getValue(), lineWidth);}
                if(shape != null) {saveState(); shape.draw(mainGC);}
                isModified = true;});}}

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
                redoStack.clear();  // Clear the redo stack after a new action
                updateUndoRedoButtons();});});}

    private void canvasDrawPressed(Canvas canvas1) {
        canvas1.setOnMousePressed(event -> {
            saveState();
            updateDraw();
            startX = event.getX();
            startY = event.getY();
            mainGC.strokeLine(startX, startY, startX, startY);
            tempGC.strokeLine(startX, startY, startX, startY);});}

    private String getFileExtension(String fileName) {
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0){
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }else{return "";}}}