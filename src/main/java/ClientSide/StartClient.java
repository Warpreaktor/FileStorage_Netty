package ClientSide;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StartClient extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("NettyClient.fxml"));
        primaryStage.setTitle("W File Storage");
        primaryStage.setScene(new Scene(root, 700, 600));
        primaryStage.show();
        //fhggdfh
    }


    public static void main(String[] args) {
        launch(args);
    }
}
