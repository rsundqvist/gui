package gui.dialog;

import assets.Const;
import contract.datastructure.AbstractType;
import contract.datastructure.Array;
import contract.datastructure.DataStructure;
import contract.datastructure.IndependentElement;
import contract.datastructure.RawType;
import contract.datastructure.VisualType;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CreateStructureDialog {

    private final Stage root;
    private final ChoiceBox<RawType> rawType;
    private final Label name;
    // Volatile
    private RawType raw;
    private AbstractType abs;
    private VisualType vis;
    private DataStructure struct;
    private String identifier;

    public CreateStructureDialog (Stage parent) {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/dialog/CreateStructureDialog.fxml"));
        fxmlLoader.setController(this);
        root = new Stage();
        root.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon_cogwheel.png")));
        root.initModality(Modality.APPLICATION_MODAL);
        root.setTitle(Const.PROGRAM_NAME + ": Create Data Structure");
        root.initOwner(parent);
        GridPane p = null;
        try {
            p = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        root.setOnCloseRequest(event -> {
            event.consume();
            closeButton();
        });
        /*
         * Raw Type
         */
        rawType = (ChoiceBox) fxmlLoader.getNamespace().get("rawType");
        rawType.setItems(FXCollections.observableArrayList(RawType.values()));
        rawType.getSelectionModel().select(RawType.independentElement);
        rawType.setOnAction(event -> {
            chooseRawType();
        });
        /*
         * Build.
         */
        name = (Label) fxmlLoader.getNamespace().get("name");
        Scene dialogScene = new Scene(p, p.getPrefWidth() - 5, p.getPrefHeight());
        root.setScene(dialogScene);
        root.setResizable(false);
    }

    private void chooseRawType () {
        raw = rawType.getSelectionModel().getSelectedItem();
    }

    public void closeButton () {
        root.close();
    }

    public void okButton () {
        createStruct();
        root.close();
    }

    private void createStruct () {
        raw = rawType.getSelectionModel().getSelectedItem();
        switch (raw) {
            case array:
                struct = new Array(identifier, abs, vis, null);
                break;
            case tree:
                struct = null;
                System.err.println("Raw type tree not supported yet.");
                break;
            case independentElement:
                struct = new IndependentElement(identifier, abs, vis, null);
                break;
        }
    }

    /**
     * Show the DataStructure creation dialog.
     *
     * @param identifier The name of the new structure.
     * @return A new DataStructure. Returns {@code null} if the user cancelled.
     */
    public DataStructure show (String identifier) {
        this.identifier = identifier;
        struct = null;
        name.setText("Create Variable: \"" + identifier + "\"");
        root.showAndWait();
        return struct;
    }
}
