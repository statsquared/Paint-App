package com.example.paint1_1;
//import files for the Controller class
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.scene.control.Alert.AlertType;

//Controller class that holds most of the information that is visually displayed in the window
public class PaintController {
    //Linking the xml/SceneBuilder with JavaFX objects/properties
    @FXML private Canvas canvas;
    @FXML private Slider widthSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Text lineWidthText;
    @FXML private CheckBox drawBox;
    //Stores the initial mouse position for drawing
    private double startX, startY;
    int lineWidth = 2;
    private File currentFile;

    //Connects the lineWidthSlider and changes the width every time it is updated
    @FXML protected void lineWidthAdjusted(){
        lineWidth=(int) widthSlider.getValue();
        lineWidthText.setText(""+lineWidth);
    }
    //Save button that is connected with the FXML
    @FXML protected void saveButtonClick() {
        if (currentFile != null) {
            System.out.println("Saving to file: " + currentFile.getAbsolutePath());
            saveImage(currentFile);
        } else {
            //Failsafe that defaults to SaveAs if the file doesn't exist
            System.out.println("No current file set. Prompting to save as...");
            saveAsButtonClick();
        }
    }
    //Method that clears the canvas and makes it blank
    @FXML protected void clearButtonClick(){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    //Brings up the About Alert that has been customized specifically for the user by ME :)
    @FXML protected void aboutClick() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setGraphic(null);
        alert.setTitle("About");
        alert.setHeaderText("Pain(t) v1.1.0");
        alert.getDialogPane().setPrefSize(500, 500);
        alert.setContentText("A newer rendition of Microsoft Paint. It is not necessarily better though...\n\nDeveloped by: Matt");
        ButtonType closeButton = new ButtonType("Close");  // Custom close button instead of 'ok'
        alert.getButtonTypes().setAll(closeButton); //sets all buttons to function as a close button
        alert.showAndWait();
    }

    //SaveAs Button that is also connected to FXML
    //This opens the file system, allows the user to rename and select the file type, and saves as a new file
    @FXML protected void saveAsButtonClick() {
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
        if (file != null) {
            currentFile = file;
            saveImage(file);
        }}

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
        if (canvasWidth > imageWidth && canvasHeight > imageHeight) {
            //image is smaller than the canvas and therefore shouldn't be stretched larger
            newHeight = imageHeight;
            newWidth = imageWidth;
            xOffset = (canvasWidth - newWidth) / 2; //Centers horizontally
        }
        else if (canvasWidth / canvasHeight > aspectRatio) {
            //Canvas is wider relative to its height than the image
            newHeight = canvasHeight;
            newWidth = newHeight * aspectRatio;
            xOffset = (canvasWidth - newWidth) / 2; //Centers horizontally
        }
        else{
            //Canvas is taller relative to its width than the image
            newWidth = canvasWidth;
            newHeight = newWidth / aspectRatio;
        }
        gc.drawImage(image, xOffset, 0, newWidth, newHeight);
    }

    //This is how the user can open a file, with many supported file types
    @FXML protected void openFileButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp"));

        //Scene stage is created and the file chooser is opened
        Stage stage = (Stage) canvas.getScene().getWindow();  // Get the current stage
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            currentFile = selectedFile;
            Image image = new Image(selectedFile.toURI().toString());
            drawImageOnCanvas(image);
        }
    }

    //Captures the screenshot needed to save canvases as a file
    public Image getCanvasSnapshot() {
        return canvas.snapshot(null, null);
    }

    //This method saves the data as a screenshot
    private void saveImage(File file) {
        Image snapshot = getCanvasSnapshot();
        if (snapshot != null) {
            try {
                //Gets the file extension to determine the correct format
                String extension = getFileExtension(file.getName()).toLowerCase();
                BufferedImage bufferedImage = convertToBufferedImage(snapshot, extension);

                //Ensures we are saving in a supported format
                if (extension.equals("png") || extension.equals("jpg") || extension.equals("bmp")) {
                    ImageIO.write(bufferedImage, extension, file);
                    System.out.println("Image saved successfully.");
                } else {
                    System.out.println("Unsupported file format.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Converts the stored snapshot image as a buffered image
    private BufferedImage convertToBufferedImage(Image img, String format) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();
        BufferedImage bufferedImage;
        if (format.equals("jpg") || format.equals("bmp")) {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        } else {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        PixelReader pixelReader = img.getPixelReader();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color fxColor = pixelReader.getColor(x, y);
                java.awt.Color awtColor;
                if (format.equals("jpg") || format.equals("bmp")) {
                    awtColor = new java.awt.Color((float) fxColor.getRed(),
                            (float) fxColor.getGreen(),
                            (float) fxColor.getBlue());
                    bufferedImage.setRGB(x, y, awtColor.getRGB());
                } else {
                    awtColor = new java.awt.Color((float) fxColor.getRed(),
                            (float) fxColor.getGreen(),
                            (float) fxColor.getBlue(),
                            (float) fxColor.getOpacity());
                    bufferedImage.setRGB(x, y, awtColor.getRGB());
                }
            }
        }
        return bufferedImage;
    }

    //Turns draw mode off by making the clicks and drags do nothing
    public void disableDraw() {
        canvas.setOnMousePressed(null);
        canvas.setOnMouseDragged(null);
    }

    //Scanner that looks for the draw checkbox, and if it is clicked, allows the user to draw, otherwise the user is unable to.
    @FXML public void initialize() {
        //Listens for the checkbox to change states
        drawBox.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {
                enableDraw();  //Enable drawing if the checkbox is selected
            } else {
                disableDraw();  //Disables drawing if the checkbox is not selected
            }
        });
    }

    //Draws on the canvas whatever the user clicks and turns draw mode on
    public void enableDraw() {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            //Captures either a single click or a mouse drag
            canvas.setOnMousePressed(event -> {
                startX = event.getX();
                startY = event.getY();
            });
            canvas.setOnMouseDragged(event -> {
                double endX = event.getX();
                double endY = event.getY();
                //Sets color and width
                gc.setStroke(colorPicker.getValue());
                gc.setLineWidth(lineWidth);
                //Draws the line and resets the coordinates if another line needs to be drawn
                gc.strokeLine(startX, startY, endX, endY);
                startX = endX;
                startY = endY;
            });
    }

    private String getFileExtension(String fileName) {
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }
}