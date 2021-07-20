package ClientSide;

import ServerSide.Commands.AbstractCommand;
import ServerSide.Commands.FileMessage;
import ServerSide.Commands.ListResponse;
import ServerSide.Commands.PathUpResponse;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.SneakyThrows;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class ExchangeController implements Initializable {
    //Окно файлового менеджера
    @FXML
    TreeView<File> clientFileTree;
    @FXML
    TreeView<File> serverFileTree;
    @FXML
    ContextMenu leftContextMenu;
    @FXML
    ContextMenu rightContextMenu;

    //Серверная панель
    @FXML
    TextField serverConsole;
    @FXML
    TextField ipTextField;
    @FXML
    TextField portTextField;
    @FXML
    Button connectBut;
    String serverRootPath;
    Path serverOpenPath;

    //Контекстное меню
    @FXML
    MenuItem createTxtFile;
    @FXML
    MenuItem copyFileToServer;
    @FXML
    MenuItem sendFileToServer;
    @FXML
    MenuItem createFolder;

    Socket socket;

    private String ipAddress = "localhost";
    int port = 8189;

    //Обмен командами
    ObjectEncoderOutputStream os;
    ObjectDecoderInputStream is;

    //Обмен текстовыми командами
    DataInputStream dis;
    DataOutputStream dos;

    //Обмен байтами
    BufferedInputStream bis;
    BufferedOutputStream bos;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientFileTree.setRoot(buildFileSystemBrowser("/").getRoot());

        //Позволяет выбирать несколько файлов
        clientFileTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serverFileTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //Настройки корневого каталога
        clientFileTree.getRoot().setExpanded(true);
        //serverFileTree.getRoot().setExpanded(true);

        //Настройка ивентов с мышью
        createTxtFile.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                String path = clientFileTree.getSelectionModel().getSelectedItems().get(0).getValue().getParent();
                String fileExtension = "txt";
                int incr = 0;
                //TODO Перед созданием нового файла обойти папку и если такой уже есть, то создать новый с incr+1 в имени
                try {
                    File file = new File(path + "New text document" + "." + fileExtension);
                    file.createNewFile();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        copyFileToServer.setOnAction(new EventHandler<ActionEvent>() {
            @SneakyThrows
            public void handle(ActionEvent e) {
                //TODO Обработать здесь так же и вариант при котором пользователь выбирает
                // несколько файлов, должны отправляться все.
                File clientFile = clientFileTree.getSelectionModel().getSelectedItems().get(0).getValue().getAbsoluteFile();
                os.writeObject(new FileMessage(Paths.get(clientFile.getAbsolutePath())));
                os.flush();
                Platform.runLater(() -> serverFileTree.setRoot(buildFileSystemBrowser(serverRootPath).getRoot()));
                serverFileTree.getRoot().setExpanded(true);
            }
        });

        clientFileTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    leftContextMenu.show(clientFileTree, Side.BOTTOM, mouseEvent.getY(), mouseEvent.getX());
                }
            }
        });

        serverFileTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                serverOpenPath = Paths.get(serverFileTree.getFocusModel().getFocusedItem().getValue().toURI());
            }
        });
    }

    public void consoleCommand() {
        if (socket == null || socket.isClosed()) return;
        String cmd = serverConsole.getText();
        try {
            bos.write(cmd.getBytes(StandardCharsets.UTF_8));
            bos.flush();
            serverConsole.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void connectButton() {

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Настраиваем соединение с сервером.
        ipAddress = ipTextField.getText() == null ? "localhost" : ipTextField.getText();

        //TODO обработать текстовое поле, так чтобы туда нельзя было вписать текст
        port = Integer.parseInt(portTextField.getText() == null ? Integer.toString(port) : portTextField.getText());

        try {
            socket = new Socket(ipAddress, port);

            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        AbstractCommand command = (AbstractCommand) is.readObject();
                        switch (command.getType()) {
                            case LIST_MESSAGE:
                                ListResponse response = (ListResponse) command;
                                List<String> names = response.getNames();
                                System.out.println("Client got the List " + names);
                                //refreshServerView(names);
                                break;
                            case PATH_RESPONSE:
                                PathUpResponse pathResponse = (PathUpResponse) command;
                                serverRootPath = pathResponse.getPath();
                                System.out.println("Client got the Path " + pathResponse);
                                Platform.runLater(() -> serverFileTree.setRoot(buildFileSystemBrowser(serverRootPath).getRoot()));
                                break;
                            case FILE_MESSAGE:
                                FileMessage message = (FileMessage) command;
                                System.out.println("File " + message);
                                //Files.write(currentDir.resolve(message.getName()), message.getData());
                                //refreshClientView();
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeView buildFileSystemBrowser(String rootPath) {
        TreeItem<File> root = createNode(new File(rootPath));
        return new TreeView<File>(root);
    }

    private TreeItem<File> createNode(final File f) {

        return new TreeItem<File>(f) {
            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            private void setPicture(TreeItem<File> treeItem) {
                ImageView imageView;
                if (treeItem.getValue().isDirectory()) {
                   imageView = new ImageView(new Image("directory.png"));
                } else {
                   imageView = new ImageView(new Image("file.png"));
                }
                imageView.setFitWidth(15);
                imageView.setFitHeight(15);
                treeItem.setGraphic(imageView);
            }

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    File f = (File) getValue();
                    isLeaf = f.isFile();
                }
                return isLeaf;
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> treeItem) {
                File f = treeItem.getValue();
                if (f != null && f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                        for (File childFile : files) {
                            TreeItem<File> node = createNode(childFile);
                            children.add(node);
                            setPicture(node);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    setPicture(node);
                                }
                            });
                        }
                        return children;
                    }
                }
                return FXCollections.emptyObservableList();
            }
        };
    }
}
