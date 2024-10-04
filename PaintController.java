package com.example.paint1_1;
//import files for the Controller class
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.StrokeLineCap;
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
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;

//Controller class that holds most of the information that is visually displayed in the window
public class PaintController {
    //Linking the xml/SceneBuilder with JavaFX objects/properties
    @FXML private Canvas canvas;
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
    //Stores the initial mouse position for drawing
    private double startX, startY;
    int lineWidth = 2;
    private File currentFile;
    public boolean isModified = false;


    @FXML public void initialize() {
        //drawListener and eraserListener are variables that contains functions that can be run. They'll be called within
        //the initialize function and were created to prevent code replication going forwards
        Runnable drawListener=()-> {if (drawBox.isSelected()){disabler();enableDraw();}else{disabler();}};
        Runnable eraserListener=()-> {if (eraserBox.isSelected()){disabler();enableErase();}else{drawListener.run();}};
        widthSlider.valueProperty().addListener((_,_,_)-> lineWidthAdjusted());
        colorPicker.valueProperty().addListener((_,_,_)-> colorLabelUpdate());
        drawBox.selectedProperty().addListener((_,_,_)-> drawListener.run());
        dashBox.selectedProperty().addListener((_,_,_)-> eraserListener.run());
        eraserBox.selectedProperty().addListener((_,_,_)-> eraserListener.run());
        colorGrabToggle.selectedProperty().addListener((_,_,newValue) -> {if (newValue){disabler(); colorGrabButtonClick();}else{enableDraw();}});
        List<ToggleButton> toggleButtons = Arrays.asList(lineToggle, rectangleToggle, circleToggle, squareToggle, ovalToggle, diamondToggle, triangleToggle);
        for (ToggleButton toggleButton : toggleButtons) {toggleButton.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {clearExcept(toggleButton);enableDraw();} else{eraserListener.run();}});}}

    public void setupKeyboardShortcuts() {
        Scene scene = canvas.getScene();  // Get the scene of the canvas or any other node
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::saveButtonClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openFileButtonClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN), this::clearButtonClick);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), Platform::exit);}

    public void clearExcept(ToggleButton selectedToggle) {
        // List of all toggle buttons
        List<ToggleButton> toggleButtons = Arrays.asList(lineToggle, rectangleToggle, circleToggle, squareToggle, ovalToggle, diamondToggle, triangleToggle);
        // Iterate through the list and deselect all buttons except the selected one
        for (ToggleButton toggleButton : toggleButtons) {
            if (toggleButton != selectedToggle) {
                toggleButton.setSelected(false);
                isModified = false;}}}

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
        canvas.setOnMousePressed(event -> {
            double x = event.getX();
            double y = event.getY();
            PixelReader pixelReader = canvas.snapshot(null, null).getPixelReader();
            Color color = pixelReader.getColor((int) x, (int) y);
            colorPicker.setValue(color);
            colorGrabToggle.setSelected(false);
        });
    }

    //Method that clears the canvas and makes it blank
    @FXML protected void clearButtonClick(){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        isModified = false;}

    @FXML protected void resizeCanvasClick(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Resize Canvas");
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
                    double newWidth = Double.parseDouble(widthText);
                    double newHeight = Double.parseDouble(heightText);
                    resizeCanvas(newWidth, newHeight);
                }catch(NumberFormatException e) {
                    Alert alert2 = new Alert(Alert.AlertType.ERROR);
                    alert2.setTitle("Error");
                    alert2.setHeaderText(null);
                    alert2.setContentText("Invalid input. Please enter valid numbers for width and height.");
                    alert2.showAndWait();
                    resizeCanvasClick();}}});}

    private void resizeCanvas(double width, double height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

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
        Stage stage = (Stage) canvas.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        //double checks to make sure there is a file that can be saved, and then saves it
        if (file != null) {currentFile = file; saveImage(file); isModified = false;}}

    //Draws an image on the canvas by first clearing the canvas if necessary, then it draws the image
    public void drawImageOnCanvas(Image image) {
        //Gets the current content on the canvas, clears it, then draws the new image
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
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
        gc.drawImage(image, xOffset, 0, newWidth, newHeight);
        isModified = true;}

    //This is how the user can open a file, with many supported file types
    @FXML protected void openFileButtonClick(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp"));
        //Scene stage is created and the file chooser is opened
        Stage stage = (Stage) canvas.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            currentFile = selectedFile;
            Image image = new Image(selectedFile.toURI().toString());
            drawImageOnCanvas(image);
            isModified = true;}}

    //Captures the screenshot needed to save canvases as a file
    public Image getCanvasSnapshot() {
        return canvas.snapshot(null, null);}

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
        canvas.setOnMousePressed(null);
        canvas.setOnMouseDragged(null);
        canvas.setOnMouseReleased(null);}

    public void colorLabelUpdate(){
        Color c = colorPicker.getValue();
        colorLabel.setText("#"+Integer.toHexString(colorPicker.getValue().hashCode())+
            " "+(int)(c.getRed()*255)+"/"+(int)(c.getGreen()*255)+"/"+(int)(c.getBlue()*255));}

    //Erases on the canvas whatever the user clicks
    public void enableErase(){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        //Captures either a single click or a mouse drag
        canvas.setOnMousePressed(event -> {
            startX = event.getX();
            startY = event.getY();
            gc.setLineWidth(lineWidth);
            gc.clearRect(startX-(lineWidth/2.0), startY-(lineWidth/2.0), lineWidth, lineWidth);});
        canvas.setOnMouseDragged(event -> {
            double endX = event.getX();
            double endY = event.getY();
            //Sets color and width
            gc.setLineWidth(lineWidth);
            //Erases the line and resets the coordinates if another line needs to be erased
            gc.clearRect(startX-(lineWidth/2.0), startY-(lineWidth/2.0), lineWidth, lineWidth);
            startX = endX;
            startY = endY;});
        isModified = true;}

    public void enableDraw(){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        if (dashBox.isSelected()) {gc.setLineDashes(10);}else{gc.setLineDashes(0);}
        //Freehand drawing mode (if no shape toggle is selected)
        if (!lineToggle.isSelected() && !rectangleToggle.isSelected() && !circleToggle.isSelected()
        && !squareToggle.isSelected() && !ovalToggle.isSelected() && !diamondToggle.isSelected()
        && !triangleToggle.isSelected()) {
            canvas.setOnMousePressed(event -> {
                startX = event.getX();
                startY = event.getY();
                gc.setLineWidth(lineWidth);
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.setStroke(colorPicker.getValue());
                gc.strokeLine(startX, startY, startX, startY);});
            canvas.setOnMouseDragged(event -> {
                double endX = event.getX();
                double endY = event.getY();
                //Sets color and width
                gc.setStroke(colorPicker.getValue());
                gc.setLineWidth(lineWidth);
                //Draws the line and resets the coordinates if another line needs to be drawn
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.strokeLine(startX, startY, endX, endY);
                startX = endX;
                startY = endY;});
        }else{
            canvas.setOnMouseDragged(null);
            // Shape drawing mode (if a shape toggle is selected)
            canvas.setOnMousePressed(event -> {
                startX = event.getX();
                startY = event.getY();});
            canvas.setOnMouseReleased(event -> {
                double endX = event.getX();
                double endY = event.getY();
                PaintShapes.Shape shape = null;
                //Check for the selected shape and instantiate the appropriate class
                if(lineToggle.isSelected()) {shape = new PaintShapes.LineShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (rectangleToggle.isSelected()) {shape = new PaintShapes.RectangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (circleToggle.isSelected()) {shape = new PaintShapes.CircleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (ovalToggle.isSelected()) {shape = new PaintShapes.OvalShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (triangleToggle.isSelected()) {shape = new PaintShapes.TriangleShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (diamondToggle.isSelected()) {shape = new PaintShapes.DiamondShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);
                }else if (squareToggle.isSelected()) {shape = new PaintShapes.SquareShape(startX, startY, endX, endY, colorPicker.getValue(), lineWidth);}
                if(shape != null) {shape.draw(gc);}});}
        isModified = true;}

    private String getFileExtension(String fileName) {
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }}