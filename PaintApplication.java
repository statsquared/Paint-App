package com.example.paint1_1;
//imports for this class and main specifically
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

//the start method helps create/prepare all the behind the scenes pieces
//that are incorporated in the window that is later shown
public class PaintApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PaintApplication.class.getResource("paint.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1530, 780);
        File file = new File("C:\\Users\\matth\\OneDrive\\Documents\\College Stuff\\Junior College\\Misc\\paintIcon.jpg");
        Image icon = new Image(file.toURI().toString());
        stage.getIcons().add(icon);
        stage.setTitle("Pain(t)");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
    //Launches the window that has my display
    public static void main(String[] args) {
        launch();
    }
}