package ge.vakho.jna_dll_injection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Demonstrates simple DLL injection using JNA library.
 */
public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        Scene scene = new Scene(root, 500, 400);
        primaryStage.setTitle("DLL Injector");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
