package gui.dialog;

import assets.Const;
import contract.datastructure.DataStructure;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collection;

public class IdentifierCollisionDialog {

    public static final short KEEP_OLD = 0;
    public static final short KEEP_OLD_ALWAYS = 1;
    public static final short CLEAR_OLD = 3;
    public static final short CLEAR_OLD_ALWAYS = 4;

    private short answer;
    private final TextField oldStructs, newStructs;
    private final CheckBox memory;
    private final Stage root;

    /**
     * Create a new IdentifierCollisionDialog.
     * @param parent The parent for the dialog.
     */
    public IdentifierCollisionDialog (Stage parent) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/dialog/IdentifierCollisionDialog.fxml"));
        fxmlLoader.setController(this);
        root = new Stage();
        root.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon_cogwheel.png")));
        root.initModality(Modality.APPLICATION_MODAL);
        root.setTitle(Const.PROGRAM_NAME + ": Identifier Collision");
        root.initOwner(parent);
        GridPane p = null;
        try {
            p = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root.setOnCloseRequest(event -> {
            event.consume();
            answer = KEEP_OLD;
            root.close();
        });
        fxmlLoader.getNamespace();
        oldStructs = (TextField) fxmlLoader.getNamespace().get("oldStructs");
        newStructs = (TextField) fxmlLoader.getNamespace().get("newStructs");
        memory = (CheckBox) fxmlLoader.getNamespace().get("memory");
        Scene dialogScene = new Scene(p, p.getPrefWidth() - 5, p.getPrefHeight());
        root.setScene(dialogScene);
        root.setResizable(false);
    }

    /**
     * Show the collision dialog.
     * @param oldStructs The old, known structures.
     * @param newStructs The new structures which are being imported.
     * @return A {@code short} indicating how the collision should be handled.
     */
    public short show (Collection<DataStructure> oldStructs, Collection<DataStructure> newStructs) {
        this.oldStructs.setText(oldStructs.toString());
        this.newStructs.setText(newStructs.toString());
        root.showAndWait();
        return answer;
    }

    /**
     * FXML Listener method.
     */
    public void reject_old () {
        if (memory.isSelected()) {
            answer = CLEAR_OLD_ALWAYS;
        } else {
            answer = CLEAR_OLD;
        }
        root.close();
    }

    /**
     * FXML Listener method.
     */
    public void keep_old () {
        if (memory.isSelected()) {
            answer = KEEP_OLD_ALWAYS;
        } else {
            answer = KEEP_OLD;
        }
        root.close();
    }
}
