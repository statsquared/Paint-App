package com.example.paint1_1;
//imports for this class and main specifically
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import javafx.stage.WindowEvent;

//the start method helps create/prepare all the behind the scenes pieces
//that are incorporated in the window that is later shown
public class PaintApplication extends Application {
    private static boolean isModified = false;
    private static PaintController paintController;
    @Override public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PaintApplication.class.getResource("paint.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1530, 780);
        paintController = fxmlLoader.getController();
        paintController.setupKeyboardShortcuts();
        File file = new File("C:\\Users\\matth\\OneDrive\\Documents\\College Stuff\\Junior College\\Misc\\paintIcon.jpg");
        Image icon = new Image(file.toURI().toString());
        stage.getIcons().add(icon);
        stage.setTitle("Pain(t)");
        stage.setOnCloseRequest(event -> {if(!isModified){promptSaveBeforeExit(event);}else{stage.close();}});
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        isModified = false;}

        public static void promptSaveBeforeExit(WindowEvent event) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setGraphic(null);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes. Do you want to save before exiting?");
            alert.setContentText("Choose an option:");
            ButtonType saveButton = new ButtonType("Save");
            ButtonType discardButton = new ButtonType("Delete");
            ButtonType cancelButton = new ButtonType("Cancel");
            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            // Show the dialog and wait for the user's response
            alert.showAndWait().ifPresent(response -> {
                if(response == saveButton) {paintController.saveAsButtonClick(); isModified = false;
                }else if(response == discardButton) {isModified= false;
                }else{event.consume();}});}

    //Launches the window that has my display
    public static void main(String[] args) {
        launch();
    }
}