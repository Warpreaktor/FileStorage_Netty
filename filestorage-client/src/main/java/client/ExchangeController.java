package client;

import commands.*;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class ExchangeController implements Initializable {
    //Общие компоненты приложения
    @FXML
    AnchorPane mainPane;
    @FXML
    VBox mainBox;
    @FXML
    DialogPane createFolderDialog;
    @FXML
    TextField textField;

    private double mouseX;
    private double mouseY;
    //Окно файлового менеджера клиента
    @FXML
    TreeView<File> clientFileTree;
    @FXML
    TreeView<File> serverFileTree;
    @FXML
    ContextMenu leftContextMenu;
    @FXML
    ContextMenu rightContextMenu;
    TreeItem<File> clientFileTreeFocus;

    //Серверная панель
    @FXML
    TextField serverConsole;
    @FXML
    TextField ipTextField;
    @FXML
    TextField portTextField;
    @FXML
    Button connectBut;
    String serverRootPath; //корневая папка на сервере выше которой клиенту ничего не доступно.
    TreeItem<File> serverFileTreeFocus; //текущий файл на сервере в котором сфокусирован пользователь

    //Контекстное меню
    @FXML
    MenuItem createTxtFile;
    @FXML
    MenuItem copyFileToServer;
    @FXML
    MenuItem sendFileToServer;
    @FXML
    MenuItem createFolder;
    @FXML
    MenuItem deleteServerFile;

    Socket socket;
    int bufferSize = 1024;

    private String ipAddress = "localhost";
    int port = 8189;

    //Обмен командами
    ObjectEncoderOutputStream os;
    ObjectDecoderInputStream is;

    //Обмен байтами
    BufferedInputStream bis;
    BufferedOutputStream bos;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientFileTree.setRoot(buildFileSystemBrowser("/").getRoot());
        clientFileTreeFocus = clientFileTree.getRoot();

        //Позволяет выбирать несколько файлов
        clientFileTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serverFileTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //Настройки корневого каталога
        clientFileTree.getRoot().setExpanded(true);

        //Настройка ивентов с мышью
        clientFileTree.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            }
        });
        serverFileTree.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            }
        });

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
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(clientFile), bufferSize)) {
                    byte[] rd = bis.readNBytes(bufferSize);
                    os.writeObject(new FilePart(Paths.get(clientFile.getAbsolutePath()), true, false, rd));
                    while (true) {
                        rd = bis.readNBytes(bufferSize);
                        if (rd.length < 1){
                            break;
                        }
                        os.writeObject(new FilePart(Paths.get(clientFile.getAbsolutePath()), false, false, rd));
                        if (rd.length <= bufferSize) {
                            os.writeObject(new FilePart(Paths.get(clientFile.getAbsolutePath()), false, true, rd));
                            os.flush();
                            break;
                        }
                    }


                } catch (IOException except) {
                    except.printStackTrace();
                }
                updateServFileTreeDir(serverFileTreeFocus.getValue().getParent());
            }
        });

        clientFileTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    leftContextMenu.show(clientFileTree, Side.BOTTOM, mouseEvent.getY(), mouseEvent.getX());
                }
                if (mouseEvent.getButton() == MouseButton.PRIMARY &&
                        clientFileTree.getSelectionModel().getSelectedItem() != null) {
                    clientFileTreeFocus = clientFileTree.getSelectionModel().getSelectedItem();
                }
            }
        });

        serverFileTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (serverFileTree.getSelectionModel().getSelectedItem() == null){
                    return;
                }
                refreshServFocused();
            }
        });
    }

    //TODO сделать так, что обновлялась только та директория, в которой произошло изменение.
    public void updateServFileTreeDir(String dirName) {
        Platform.runLater(() -> {
            serverFileTree.setRoot(buildFileSystemBrowser(serverRootPath).getRoot());
            serverFileTree.getRoot().setExpanded(true);
        });
    }
    public void updateClientFileTreeDir(String dirName) {
        Platform.runLater(() -> {
            clientFileTree.setRoot(buildFileSystemBrowser("/").getRoot());
            clientFileTree.getRoot().setExpanded(true);
        });
    }

    synchronized public void createClientFolder() {
        //TODO Добавить здесь обработку ситуации, когда создаётся папка с названием которое уже существует в данной директории
        if (createFolderDialog.isVisible()){
            return;
        }
        clientFileTree.setVisible(false);
        createFolderDialog.setHeaderText("Folder name");
        createFolderDialog.setPrefWidth(150);
        createFolderDialog.setPrefHeight(100);
        createFolderDialog.setLayoutX(mouseX);
        createFolderDialog.setLayoutY(mouseY);
        createFolderDialog.setVisible(true);
        final Button okButton = (Button) createFolderDialog.lookupButton(ButtonType.OK);
        final Button cancelButton = (Button) createFolderDialog.lookupButton(ButtonType.CANCEL);
        okButton.addEventFilter(ActionEvent.ACTION, event ->
        {
            if (textField.getText().equals("")) {
               return;
            }
            if (clientFileTreeFocus.getValue().isDirectory()) {
                try {
                    System.out.println(clientFileTreeFocus.getValue().getAbsolutePath() + "\\" + textField.getText());
                    Files.createDirectory(Path.of(clientFileTreeFocus.getValue().getAbsolutePath() + "\\" + textField.getText()));
                    updateClientFileTreeDir(clientFileTreeFocus.getValue().getAbsolutePath());
                    createFolderDialog.setVisible(false);
                    clientFileTree.setVisible(true);
                    textField.clear();
                    event.consume();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    clientFileTree.setVisible(true);
                    textField.clear();
                }
            }else{
                try {
                    Files.createDirectory(Path.of(clientFileTreeFocus.getValue().getAbsolutePath() + "\\" + textField.getText()));
                    updateClientFileTreeDir(clientFileTreeFocus.getValue().getAbsolutePath());
                    clientFileTree.setVisible(true);
                    event.consume();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    clientFileTree.setVisible(true);
                    textField.clear();
                }
            }
        });
        cancelButton.addEventFilter(ActionEvent.ACTION, event ->
        {
            createFolderDialog.setVisible(false);
            clientFileTree.setVisible(true);
            textField.clear();
            event.consume();
        });
    }

    synchronized public void createServerFolder() {
        //TODO Добавить здесь обработку ситуации, когда создаётся папка с названием которое уже существует в данной директории
        if (createFolderDialog.isVisible()){
            return;
        }
        serverFileTree.setVisible(false);
        createFolderDialog.setHeaderText("Folder name");
        createFolderDialog.setPrefWidth(150);
        createFolderDialog.setPrefHeight(100);
        createFolderDialog.setLayoutX(mouseX + clientFileTree.getWidth());
        createFolderDialog.setLayoutY(mouseY);
        createFolderDialog.setVisible(true);
        final Button okButton = (Button) createFolderDialog.lookupButton(ButtonType.OK);
        final Button cancelButton = (Button) createFolderDialog.lookupButton(ButtonType.CANCEL);
        okButton.addEventFilter(ActionEvent.ACTION, event ->
        {
            if (textField.getText().equals("")) {
                return;
            }
            if (serverFileTreeFocus.getValue().isDirectory()) {
                try {
                    os.writeObject(new CreateDirectory(serverFileTreeFocus.getValue().getAbsolutePath() + "\\" + textField.getText()));
                    updateServFileTreeDir(serverFileTreeFocus.getValue().getAbsolutePath());
                    createFolderDialog.setVisible(false);
                    serverFileTree.setVisible(true);
                    textField.clear();
                    event.consume();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    serverFileTree.setVisible(true);
                    textField.clear();
                }
            }else{
                try {
                    Files.createDirectory(Path.of(serverFileTreeFocus.getValue().getAbsolutePath() + "\\" + textField.getText()));
                    updateClientFileTreeDir(serverFileTreeFocus.getValue().getAbsolutePath());
                    serverFileTree.setVisible(true);
                    event.consume();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    serverFileTree.setVisible(true);
                    textField.clear();
                }
            }
        });
        cancelButton.addEventFilter(ActionEvent.ACTION, event ->
        {
            createFolderDialog.setVisible(false);
            serverFileTree.setVisible(true);
            textField.clear();
            event.consume();
        });
    }

    public void deleteServerFile() {
        refreshServFocused();
        try {
            os.writeObject(new DeleteRequest(serverFileTreeFocus.getValue().getAbsolutePath()));
            updateServFileTreeDir(serverFileTreeFocus.getValue().getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshServFocused() {
        try {
            serverFileTreeFocus = serverFileTree.getSelectionModel().getSelectedItem();
            os.writeObject(new FocusResponse(serverFileTreeFocus.getValue().getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                                Platform.runLater(() -> {
                                    serverFileTree.setRoot(buildFileSystemBrowser(serverRootPath).getRoot());
                                    serverFileTreeFocus = serverFileTree.getRoot();
                                });
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
