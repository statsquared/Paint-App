package com.example.paint1_1;

//import files for the fileChooser and pieces needed to import/use images
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

//Desired to use JavaFX.Utils, but didn't figure out to backdate Java until it was working pretty much

import java.io.*;

//Controller class that holds most of the information that is visually displayed in the window
public class PaintController {
    @FXML
    private Label welcomeText;

    @FXML
    private ImageView imageView;

    private File currentFile;

    //Save button that is connected with the FXML
    //Has a failsafe that defaults to SaveAs if the file doesn't exist
    @FXML
    protected void saveButtonClick() {
        if (currentFile != null) {
            System.out.println("Saving to file: " + currentFile.getAbsolutePath());
            savePixelData(currentFile);
        } else {
            System.out.println("No current file set. Prompting to save as...");
            saveAsButtonClick();
        }
    }

    //SaveAs Button that is also connected to FXML
    //This opens the file system, allows the user to rename the file, and saves a new file
    @FXML
    protected void saveAsButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
                //new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));

        //Starts the stage initialization so that the fileChooser appears visibly for the user
        Stage stage = (Stage) welcomeText.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        //double checks to make sure there is a file that can be saved, and then saves it
        if (file != null) {
            currentFile = file;
            savePixelData(file);
        }}

    //This is how the user can open a file, with many supported file types
    @FXML
    protected void openFileButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Image Files", "*.jpeg","*.png", "*.jpg", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        //Scene stage is created and the file chooser is opened
        Stage stage = (Stage) welcomeText.getScene().getWindow();  // Get the current stage
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            currentFile = selectedFile;
            String extension = getFileExtension(selectedFile.getName()).toLowerCase();

            if (extension.equals("txt")) {
                loadPixelData(selectedFile);
            } else {
                Image image = new Image(selectedFile.toURI().toString());
                imageView.setImage(image);
            }
        }
    }

    //This is an old version where I tried saving the file using the FX.Utils that wasn't working
    //This will be revisited/revised in future versions most likely
    /*private void saveFile(File file) {
        // This method needs to be implemented to handle the saving process
        if (imageView.getImage() != null) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                String extension = getFileExtension(file.getName());

                // Save the image as a different format if it is necessary
                if(extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("gif")){
                    Image image = imageView.getImage();
                    // The process to write the image to file goes here
                    // JavaFX does not provide a direct way to write Image objects to disk, so you would need to use AWT for this.}
                else {
                    welcomeText.setText("Unsupported file format.");
                }
            } catch (IOException e) {
                welcomeText.setText("Failed to save the file.");
            }
        }
    }*/

    //This method saves the data as a file with each pixel as data (takes up more space)
    private void savePixelData(File file) {
        if (imageView.getImage() != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                Image image = imageView.getImage();
                PixelReader pixelReader = image.getPixelReader();

                int width = (int) image.getWidth();
                int height = (int) image.getHeight();

                writer.write(width + " " + height + "\n");

                // Iterate through each pixel
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Color color = pixelReader.getColor(x, y);
                        int red = (int) (color.getRed() * 255);
                        int green = (int) (color.getGreen() * 255);
                        int blue = (int) (color.getBlue() * 255);
                        writer.write(String.format("%d %d %d %d %d\n", x, y, red, green, blue));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // Print stack trace for debugging
            }
        }
    }

    //This method will load the file and generate the image on the window
    private void loadPixelData(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                String[] dimensions = line.split(" ");
                int width = Integer.parseInt(dimensions[0]);
                int height = Integer.parseInt(dimensions[1]);

                WritableImage writableImage = new WritableImage(width, height);
                PixelWriter pixelWriter = writableImage.getPixelWriter();

                while ((line = reader.readLine()) != null) {
                    String[] pixelData = line.split(" ");
                    int x = Integer.parseInt(pixelData[0]);
                    int y = Integer.parseInt(pixelData[1]);
                    int red = Integer.parseInt(pixelData[2]);
                    int green = Integer.parseInt(pixelData[3]);
                    int blue = Integer.parseInt(pixelData[4]);

                    Color color = Color.rgb(red, green, blue);
                    pixelWriter.setColor(x, y, color);
                }

                imageView.setImage(writableImage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }
}