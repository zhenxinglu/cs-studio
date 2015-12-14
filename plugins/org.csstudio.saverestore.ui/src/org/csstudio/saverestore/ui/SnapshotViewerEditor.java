package org.csstudio.saverestore.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.csstudio.csdata.ProcessVariable;
import org.csstudio.csdata.TimestampedPV;
import org.csstudio.saverestore.DataProvider;
import org.csstudio.saverestore.DataProviderException;
import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.Utilities;
import org.csstudio.saverestore.Utilities.VTypeComparison;
import org.csstudio.saverestore.data.Snapshot;
import org.csstudio.saverestore.data.VNoData;
import org.csstudio.saverestore.data.VSnapshot;
import org.csstudio.saverestore.ui.util.SnapshotDataFormat;
import org.csstudio.saverestore.ui.util.VTypePair;
import org.csstudio.ui.fx.util.FXComboInputDialog;
import org.csstudio.ui.fx.util.FXEditorPart;
import org.csstudio.ui.fx.util.FXMessageDialog;
import org.csstudio.ui.fx.util.FXSaveAsDialog;
import org.csstudio.ui.fx.util.FXTextAreaInputDialog;
import org.csstudio.ui.fx.util.StaticTextArea;
import org.csstudio.ui.fx.util.StaticTextField;
import org.diirt.util.time.Timestamp;
import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.VType;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;

import javafx.animation.Animation.Status;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 *
 * <code>SnapshotViewerEditor</code> is an {@link IEditorPart} implementation for displaying, creating, comparing, and
 * restoring snapshots. It provides a table that displays the stored values (together with the alarm data and timestamp)
 * and live data. In comparison view only the values are displayed. At the top of the editor buttons for taking, saving
 * and restoring the snapshot are located.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class SnapshotViewerEditor extends FXEditorPart implements ISelectionProvider {

    /** The editor ID */
    public static final String ID = "org.csstudio.saverestore.ui.editor.snapshotviewer";
    private static boolean resizePolicyNotInitialized = true;
    private static final String STYLE = "style.css";

    /**
     *
     * <code>VTypeCellEditor</code> is an editor type for {@link VType} or {@link VTypePair}, which allows editing the
     * value as a string.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     * @param <T> {@link VType} or {@link VTypePair}
     */
    private static class VTypeCellEditor<T> extends TextFieldTableCell<TableEntry, T> {
        private static final Image WARNING_IMAGE = new Image(
            SnapshotViewerEditor.class.getResourceAsStream("/icons/hprio_tsk.png"));

        VTypeCellEditor() {
            setConverter(new StringConverter<T>() {
                @Override
                public String toString(T item) {
                    if (item == null) {
                        return "";
                    } else if (item instanceof VType) {
                        return Utilities.valueToString((VType) item);
                    } else if (item instanceof VTypePair) {
                        return Utilities.valueToString(((VTypePair) item).value);
                    } else {
                        return item.toString();
                    }
                }

                @SuppressWarnings("unchecked")
                @Override
                public T fromString(String string) {
                    T item = getItem();
                    if (string == null) {
                        return null;
                    } else if (item instanceof VType) {
                        return (T) Utilities.valueFromString(string, (VType) item);
                    } else if (item instanceof VTypePair) {
                        VTypePair t = (VTypePair) item;
                        if (t.value instanceof VNoData) {
                            return (T) new VTypePair(t.base, Utilities.valueFromString(string, t.base));
                        } else {
                            return (T) new VTypePair(t.base, Utilities.valueFromString(string, t.value));
                        }
                    } else {
                        return null;
                    }
                }
            });
            setTooltip(new Tooltip());
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            updateItem(getItem(), isEmpty());
        }

        /*
         * (non-Javadoc)
         *
         * @see javafx.scene.control.cell.TextFieldTableCell#updateItem(java.lang.Object, boolean)
         */
        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("diff-cell");
            if (item == null || empty) {
                setText("");
                getTooltip().setText(null);
                setGraphic(null);
            } else {
                if (item instanceof VNoData) {
                    setText(item.toString());
                    setGraphic(null);
                    getTooltip().setText("No Value Available");
                } else if (item instanceof VType) {
                    setText(Utilities.valueToString((VType) item));
                    setGraphic(null);
                    getTooltip().setText(item.toString());
                } else if (item instanceof VTypePair) {
                    VTypeComparison vtc = Utilities.valueToCompareString(((VTypePair) item).value,
                        ((VTypePair) item).base);
                    setText(vtc.string);
                    if (vtc.valuesEqual != 0) {
                        getStyleClass().add("diff-cell");
                        setGraphic(new ImageView(WARNING_IMAGE));
                    }
                    getTooltip().setText(item.toString());
                }

            }
        }
    }

    /**
     * <code>TooltipTableColumn</code> is the common table column implementation, which can also provide the tooltip.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     * @param <T> the type of the values displayed by this column
     */
    private class TooltipTableColumn<T> extends TableColumn<TableEntry, T> {
        TooltipTableColumn(String text, String tooltip, int minWidth) {
            setup(text, tooltip, minWidth, -1, true);
        }

        TooltipTableColumn(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
            setup(text, tooltip, minWidth, prefWidth, resizable);
        }

        private void setup(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
            Label label = new Label(text);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setTooltip(new Tooltip(tooltip));
            setGraphic(label);
            if (minWidth != -1) {
                setMinWidth(minWidth);
            }
            if (prefWidth != -1) {
                setPrefWidth(prefWidth);
            }
            setResizable(resizable);
            setOnEditStart(e -> controller.suspend());
            setOnEditCancel(e -> controller.resume());
            setOnEditCommit(e -> controller.resume());
        }
    }

    // the style for the animated save button
    private static final String ANIMATED_STYLE = "-fx-background-color: #FF8080; -fx-text-fill: white; "
        + "-fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.6),5,0.0,0,1);";

    private BorderPane contentPane;
    private TableView<TableEntry> table;
    private SnapshotViewerController controller;
    private TextArea commentField;
    private TextField dateField;
    private TextField creatorField;
    private Button takeSnapshotButton;
    private Button restoreSnapshotButton;
    private Button saveSnapshotButton;
    private CheckBox selectAllCheckBox;
    private FadeTransition animation;
    private Menu contextMenu;

    private int clickedColumn = -1;

    /**
     * Constructs a new editor.
     */
    public SnapshotViewerEditor() {
        controller = new SnapshotViewerController(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        MenuManager menu = new MenuManager();
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        contextMenu = menu.createContextMenu(parent);
        parent.setMenu(contextMenu);
        getSite().registerContextMenu(menu, this);
        // Platform.runLater(()->{
        // Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
        // System.out.println("Handler caught exception: "+throwable.getMessage());
        // throwable.printStackTrace();
        // });
        // });
    }

    private void init() {
        if (resizePolicyNotInitialized) {
            try {
                Field f = TableView.CONSTRAINED_RESIZE_POLICY.getClass().getDeclaredField("isFirstRun");
                f.setAccessible(true);
                f.set(TableView.CONSTRAINED_RESIZE_POLICY, Boolean.FALSE);
            } catch (Exception e) {
                // ignore
            }
            resizePolicyNotInitialized = false;
        }

        animation = new FadeTransition(Duration.seconds(0.15), saveSnapshotButton);
        animation.setAutoReverse(true);
        animation.setFromValue(1.0);
        animation.setToValue(0.4);
        animation.setCycleCount(6);
        controller.snapshotSaveableProperty().addListener((a, o, n) -> {
            if (!n) {
                animation.pause();
                animation.jumpTo(Duration.seconds(0));
                animation.stop();
                saveSnapshotButton.setStyle(null);
            } else if (animation.getStatus() != Status.RUNNING) {
                saveSnapshotButton.setStyle(ANIMATED_STYLE);
                animation.play();
            }
            firePropertyChange(PROP_DIRTY);
        });
        if (controller.snapshotSaveableProperty().get()) {
            saveSnapshotButton.setStyle(ANIMATED_STYLE);
            animation.play();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        if (getEditorInput() instanceof IFileEditorInput) {
            save(monitor, false);
        } else {
            SaveRestoreService.getInstance().execute("Save Snapshot", () -> save(monitor, false));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        if (getEditorInput() instanceof IFileEditorInput) {
            // if saving a file that was initially opened from workspace, save it back to workspacae, but let eclipse
            // to choose the thread and proper locking mechanism
            save(new NullProgressMonitor(), true);
        } else {
            // if saving to git, there is no save as, so this can never be called at all, but just in case,
            // do a normal save
            doSave(new NullProgressMonitor());
        }
    }

    private void save(final IProgressMonitor monitor, boolean saveAs) {
        // loop over all snapshots and save them one by one
        final List<VSnapshot> snapshots = controller.getSnapshots(true);
        do {
            if (!save(snapshots, true, saveAs).map(snapshots::remove).isPresent()) {
                break;
            }
        } while (!snapshots.isEmpty());
        getSite().getShell().getDisplay().asyncExec(() -> {
            monitor.done();
            firePropertyChange(PROP_DIRTY);
        });
    }

    private Optional<VSnapshot> save(List<VSnapshot> snapshots, boolean promptIfOnlyOne, final boolean saveAs) {
        if (snapshots.isEmpty()) {
            return null;
        }
        // ask the user to choose the snapshot to save if there is more than one snapshot or promptIfOnlyOne is true
        // and then save the selected snapshot
        if (getEditorInput() instanceof IFileEditorInput) {
            if (promptIfOnlyOne || snapshots.size() > 1) {
                return FXComboInputDialog
                    .pick(getSite().getShell(), "Select Snapshot", "Select the snapshot that you wish to save",
                        snapshots.get(0), snapshots)
                    .map(e -> saveSnapshotToFile(e, (IFileEditorInput) getEditorInput(), saveAs));
            } else {
                return Optional
                    .ofNullable(saveSnapshotToFile(snapshots.remove(0), (IFileEditorInput) getEditorInput(), saveAs));
            }
        } else {
            if (promptIfOnlyOne || snapshots.size() > 1) {
                return FXComboInputDialog.pick(getSite().getShell(), "Select Snapshot",
                    "Select the snapshot that you wish to save", snapshots.get(0), snapshots).map(this::saveSnapshot);
            } else {
                return Optional.ofNullable(saveSnapshot(snapshots.remove(0)));
            }
        }
    }

    private VSnapshot saveSnapshot(VSnapshot snapshot) {
        if (snapshot.getSnapshot().isPresent()) {
            Optional<String> comment = FXTextAreaInputDialog.get(getSite().getShell(), "Snapshot Comment",
                "Provide a short comment for the snapshot " + snapshot, "",
                e -> (e == null || e.trim().length() < 10) ? "Comment should be at least 10 characters long." : null);
            return comment.map(e -> controller.saveSnapshot(e, snapshot)).orElse(null);
        } else {
            // should never happen at all
            throw new IllegalArgumentException("Snapshot " + snapshot + " is invalid.");
        }
    }

    /**
     * Save the snapshot to a file in workspace. File can be either selected using the save as dialog or the file
     * specified by the input is used.
     *
     * @param snapshot the snapshot to save
     * @param input the editor input to use as a destination file or as an initial location
     * @param saveAs true if the snapshot should be saved o a new file or false if the file specified by input is
     *            overwritten
     * @return the snapshot if successful or null otherwise
     */
    private VSnapshot saveSnapshotToFile(VSnapshot snapshot, IFileEditorInput input, boolean saveAs) {
        if (saveAs) {
            SaveAsDialog saveAsDialog = new FXSaveAsDialog(getSite().getShell());
            saveAsDialog.setOriginalFile(((IFileEditorInput) input).getFile());
            if (saveAsDialog.open() == Window.OK) {
                IPath targetPath = saveAsDialog.getResult();
                IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(targetPath);
                return controller.saveToFile(targetFile, snapshot);
            }
        } else {
            return controller.saveToFile(input.getFile(), snapshot);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        VSnapshot snapshot = input.getAdapter(VSnapshot.class);
        if (snapshot != null) {
            setSnapshot(snapshot);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        controller.dispose();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return controller.snapshotSaveableProperty().get();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return getEditorInput() instanceof IFileEditorInput;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.ui.util.FXEditorPart#createFxScene()
     */
    @Override
    protected Scene createFxScene() {
        contentPane = new BorderPane();
        contentPane.setTop(new BorderPane(createMainControlPane(), createToolbarPane(), null, null, null));
        init();
        return new Scene(contentPane);
    }

    private Node createToolbarPane() {
        HBox leftToolbar = new HBox(5);
        Button addPVButton = new Button("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/includeMode_filter.png"))));
        addPVButton.setTooltip(new Tooltip("Add a PV from the central archiving system"));
        addPVButton.setOnAction(e -> controller.addPVFromArchive(x -> table.getItems().add(x)));
        Button importButton = new Button("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/import_wiz.png"))));
        importButton.setTooltip(new Tooltip("Import PV values from external source"));
        importButton.setDisable(ExtensionPointLoader.getInstance().getValueImporters().isEmpty());
        importButton.setOnAction(e -> {
            List<ValueImporterWrapper> importers = ExtensionPointLoader.getInstance().getValueImporters();
            if (importers.isEmpty()) {
                return;
            }
            new FXComboInputDialog<>(getSite().getShell(), "Select Value Importer",
                "Select the value importer from which you wish to import the values", importers.get(0), importers)
                    .openAndWait().ifPresent(imp -> SaveRestoreService.getInstance().execute("Import Values",
                        () -> controller.importValues(imp, x -> addSnapshot(x))));
        });
        ToggleButton addReadbacksButton = new ToggleButton("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/exp_deployplug.png"))));
        addReadbacksButton.setTooltip(new Tooltip("Show column with values from the readback PVs"));
        addReadbacksButton.setDisable(!ExtensionPointLoader.getInstance().getReadbackProvider().isPresent());
        addReadbacksButton.selectedProperty().addListener((a, o, n) -> controller.showReadbacks(n, b -> {
            final int num = controller.getNumberOfSnapshots();
            final boolean show = controller.isShowReadbacks();
            Platform.runLater(() -> createTable(b, num, show));
        }));
        Button openSnapshotFromFileButton = new Button("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/fldr_obj.png"))));
        openSnapshotFromFileButton.setTooltip(new Tooltip("Open snapshot from file"));
        openSnapshotFromFileButton.setOnAction(e -> {
            FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[] { "*" + SnapshotViewerController.FEXT_SNP });
            dialog.setFilterNames(new String[] { "Single Snapshot (*.snp)" });
            String ans = dialog.open();
            if (ans != null) {
                SaveRestoreService.getInstance().execute("Open file", () -> {
                    controller.openFromFile(new File(ans)).ifPresent(s -> addSnapshot(s));
                });
            }
        });
        Button saveSnapshotToFileButton = new Button("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/saveas_edit.png"))));
        saveSnapshotToFileButton.setTooltip(new Tooltip("Save a snapshot to file"));
        saveSnapshotToFileButton.setOnAction(e -> {
            selectFile(true).ifPresent(f -> {
                final List<VSnapshot> snapshots = controller.getAllSnapshots();
                if (snapshots.size() == 1) {
                    SaveRestoreService.getInstance().execute("Export to snp file",
                        () -> controller.exportSingleSnapshotToFile(snapshots.get(0), f));
                } else {
                    new FXComboInputDialog<>(getSite().getShell(), "Select Snapshot",
                        "Select the snapshot that you wish to save", snapshots.get(0), snapshots).openAndWait()
                            .ifPresent(s -> SaveRestoreService.getInstance().execute("Export to snp file",
                                () -> controller.exportSingleSnapshotToFile(s, f)));
                }
            });
        });
        Button exportButton = new Button("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/export_wiz.png"))));
        exportButton.setTooltip(new Tooltip("Export editor contents to file"));
        exportButton.setOnAction(e -> {
            selectFile(false).ifPresent(
                f -> SaveRestoreService.getInstance().execute("Export to csv file", () -> controller.exportToFile(f)));
        });
        javafx.scene.control.Separator separator1 = new javafx.scene.control.Separator(Orientation.VERTICAL);
        separator1.getStylesheets().add(this.getClass().getResource(STYLE).toExternalForm());
        javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator(Orientation.VERTICAL);
        separator2.getStylesheets().add(this.getClass().getResource(STYLE).toExternalForm());
        javafx.scene.control.Separator separator3 = new javafx.scene.control.Separator(Orientation.VERTICAL);
        separator3.getStylesheets().add(this.getClass().getResource(STYLE).toExternalForm());
        leftToolbar.getChildren().addAll(addPVButton, separator1, addReadbacksButton, separator2, importButton,
            separator3, openSnapshotFromFileButton, saveSnapshotToFileButton, exportButton);

        HBox rightToolbar = new HBox(5);
        ToggleButton hideEqualItemsButton = new ToggleButton("",
            new ImageView(new Image(SnapshotViewerEditor.class.getResourceAsStream("/icons/filter_ps.png"))));
        hideEqualItemsButton.setTooltip(new Tooltip("Hide/Show items where snapshot value equals current value"));
        hideEqualItemsButton.selectedProperty().addListener((a, o, n) -> {
            SaveRestoreService.getInstance().execute("Filter items", () -> {
                final List<TableEntry> entries = controller.setHideEqualItems(n);
                Platform.runLater(() -> table.getItems().setAll(entries));
            });
        });
        rightToolbar.getChildren().addAll(hideEqualItemsButton);
        HBox toolbar = new HBox(5);
        HBox.setHgrow(leftToolbar, Priority.ALWAYS);
        HBox.setHgrow(rightToolbar, Priority.NEVER);
        String background = "-fx-background-color: #FBFBFB;";
        leftToolbar.setStyle(background);
        rightToolbar.setStyle(background);
        toolbar.setStyle(background);
        toolbar.getChildren().addAll(leftToolbar, rightToolbar);
        return toolbar;
    }

    private Optional<File> selectFile(boolean snp) {
        File f = null;
        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        if (snp) {
            dialog.setFilterExtensions(new String[] { "*" + SnapshotViewerController.FEXT_SNP });
            dialog.setFilterNames(new String[] { "Single Snapshot (*.snp)" });
        } else {
            dialog.setFilterExtensions(new String[] { "*" + SnapshotViewerController.FEXT_CSV });
            dialog.setFilterNames(new String[] { "All snapshots (*.csv)" });
        }
        do {
            String file = dialog.open();
            if (file == null) {
                return Optional.empty();
            } else {
                f = new File(file);
                if (f.exists()) {
                    int ans = FXMessageDialog.openYesNoCancel(getSite().getShell(), "Overwrite File",
                        "The file '" + file + "' already exists. Do you want to overwrite it?");
                    if (ans == 0) {
                        return Optional.of(f); // overwrite
                    } else if (ans == 2) {
                        return Optional.empty(); // cancel
                    }
                } else {
                    return Optional.of(f);
                }
            }
        } while (true);
    }

    private Node createMainControlPane() {
        GridPane left = new GridPane();
        left.setPadding(new Insets(5, 5, 5, 5));
        left.setVgap(5);
        left.setHgap(5);
        left.setAlignment(Pos.TOP_LEFT);
        commentField = new StaticTextArea();
        commentField.setPrefWidth(300);
        commentField.setPrefRowCount(2);
        GridPane.setVgrow(commentField, Priority.ALWAYS);
        GridPane.setFillHeight(commentField, true);
        creatorField = new StaticTextField();
        creatorField.setPrefWidth(150);
        dateField = new StaticTextField();
        dateField.setPrefWidth(150);
        left.add(new Label("Comment:"), 0, 0);
        left.add(commentField, 1, 0, 1, 2);
        left.add(new Label("Creator:"), 2, 0);
        left.add(new Label("Timestamp:"), 2, 1);
        left.add(creatorField, 3, 0);
        left.add(dateField, 3, 1);
        GridPane grid = new GridPane();
        Node right = createButtonPane();
        GridPane.setHgrow(left, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.NEVER);
        GridPane.setFillHeight(right, true);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        return grid;
    }

    private Node createButtonPane() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        takeSnapshotButton = new Button();
        VBox box = new VBox();
        box.getChildren().addAll(new Label("Take"), new Label("Snapshot"));
        box.setAlignment(Pos.CENTER);
        takeSnapshotButton.setGraphic(box);
        takeSnapshotButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        takeSnapshotButton.setTooltip(new Tooltip("Take a new snapshot (locally store the live values)"));
        takeSnapshotButton.setOnAction(
            e -> SaveRestoreService.getInstance().execute("Take Snapshot", () -> controller.takeSnapshot()));

        saveSnapshotButton = new Button();
        box = new VBox();
        box.getChildren().addAll(new Label("Save"), new Label("Snapshot"));
        box.setAlignment(Pos.CENTER);
        saveSnapshotButton.setGraphic(box);
        saveSnapshotButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        saveSnapshotButton.setTooltip(new Tooltip("Save the snapshot values as a new revision"));
        saveSnapshotButton.setOnAction(e -> {
            // when user presses the big save button, only save one selected snapshot
            final List<VSnapshot> snapshots = controller.getSnapshots(true);
            if (getEditorInput() instanceof IFileEditorInput) {
                save(snapshots, false, false);
            } else {
                SaveRestoreService.getInstance().execute("Save Snapshot", () -> save(snapshots, false, false));
            }
        });
        saveSnapshotButton.disableProperty().bind(controller.snapshotSaveableProperty().not());

        restoreSnapshotButton = new Button("Restore");
        restoreSnapshotButton.setTooltip(new Tooltip("Set the stored values to PVs"));
        restoreSnapshotButton.setOnAction(e -> SaveRestoreService.getInstance().execute("Restore Snapshot", () -> {
            List<VSnapshot> snapshots = controller.getSnapshots(false);
            if (snapshots.isEmpty()) {
                return;
            } else if (snapshots.size() == 1) {
                controller.restoreSnapshot(snapshots.get(0));
            } else {
                FXComboInputDialog.pick(getSite().getShell(), "Select Snapshot",
                    "Select the snapshot that you wish to restore", snapshots.get(0), snapshots)
                    .ifPresent(controller::restoreSnapshot);
            }
        }));
        restoreSnapshotButton.disableProperty().bind(controller.snapshotRestorableProperty().not());

        restoreSnapshotButton.setMaxHeight(Integer.MAX_VALUE);
        saveSnapshotButton.setMaxHeight(Integer.MAX_VALUE);
        takeSnapshotButton.setMaxHeight(Integer.MAX_VALUE);
        GridPane.setHgrow(restoreSnapshotButton, Priority.ALWAYS);
        GridPane.setHgrow(takeSnapshotButton, Priority.NEVER);
        GridPane.setHgrow(saveSnapshotButton, Priority.NEVER);
        GridPane.setVgrow(restoreSnapshotButton, Priority.ALWAYS);
        GridPane.setVgrow(takeSnapshotButton, Priority.ALWAYS);
        GridPane.setVgrow(saveSnapshotButton, Priority.ALWAYS);
        GridPane.setFillHeight(restoreSnapshotButton, true);
        GridPane.setFillHeight(takeSnapshotButton, true);
        GridPane.setFillHeight(saveSnapshotButton, true);
        GridPane.setFillWidth(restoreSnapshotButton, true);
        GridPane.setFillWidth(takeSnapshotButton, true);
        GridPane.setFillWidth(saveSnapshotButton, true);
        grid.add(restoreSnapshotButton, 0, 0);
        grid.add(takeSnapshotButton, 1, 0);
        grid.add(saveSnapshotButton, 2, 0);
        return grid;
    }

    private TableView<TableEntry> createTableForSingleSnapshot(boolean showReadback) {
        final TableView<TableEntry> table = new TableView<>();
        TableColumn<TableEntry, Boolean> selectedColumn = new TooltipTableColumn<>("",
            "Include this PV when restoring values", 30, 30, false);
        selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectedColumn));
        selectedColumn.setEditable(true);
        selectedColumn.setSortable(false);
        selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.setOnAction(
            e -> table.getItems().forEach(te -> te.selectedProperty().setValue(selectAllCheckBox.isSelected())));
        selectedColumn.setGraphic(selectAllCheckBox);

        TableColumn<TableEntry, Integer> idColumn = new TooltipTableColumn<>("#",
            "The order number of the PV in the beamline set", 30, 30, false);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<TableEntry, String> pvNameColumn = new TooltipTableColumn<>("PV", "The name of the PV", 170);
        pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));

        TableColumn<TableEntry, Timestamp> timestampColumn = new TooltipTableColumn<>("Timestamp",
            "Timestamp of the value when the snapshot was taken", 120, 135, true);
        timestampColumn.setCellValueFactory(new PropertyValueFactory<TableEntry, Timestamp>("timestamp"));
        timestampColumn.setCellFactory(c -> new TableCell<TableEntry, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(Utilities.timestampToString(item));
                }
            }
        });
        timestampColumn.setPrefWidth(135);

        TableColumn<TableEntry, String> statusColumn = new TooltipTableColumn<>("Status",
            "Alarm status of the PV when the snapshot was taken", 100, 100, true);
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<TableEntry, AlarmSeverity> severityColumn = new TooltipTableColumn<>("Severity",
            "Alarm severity of the PV when the snapshot was taken", 80, 80, false);
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));

        TableColumn<TableEntry, VTypePair> storedValueColumn = new TooltipTableColumn<>(
            "Stored Value (" + Utilities.DELTA_CHAR + " Setpoint)", "PV value when the snapshot was taken", 100);
        storedValueColumn.setCellValueFactory(e -> e.getValue().valueProperty());
        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedValueColumn.setEditable(true);
        storedValueColumn.setOnEditCommit(e -> {
            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, e.getNewValue().value));
            controller.resume();
        });

        TableColumn<TableEntry, VType> liveValueColumn = new TooltipTableColumn<>("Setpoint Value", "Current PV value",
            100);
        liveValueColumn.setCellValueFactory(new PropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setEditable(false);

        if (showReadback) {
            TableColumn<TableEntry, VType> readbackColumn = new TooltipTableColumn<>(
                "Readback (" + Utilities.DELTA_CHAR + " Setpoint)", "Current Readback value", 100);
            readbackColumn.setCellValueFactory(new PropertyValueFactory<>("readback"));
            readbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
            readbackColumn.setEditable(false);
            table.getColumns().addAll(Arrays.asList(selectedColumn, idColumn, pvNameColumn, timestampColumn,
                statusColumn, severityColumn, storedValueColumn, liveValueColumn, readbackColumn));
        } else {
            table.getColumns().addAll(Arrays.asList(selectedColumn, idColumn, pvNameColumn, timestampColumn,
                statusColumn, severityColumn, storedValueColumn, liveValueColumn));
        }
        return table;
    }

    private TableView<TableEntry> createTableForMultipleSnapshots(int n, boolean showReadback) {
        final TableView<TableEntry> table = new TableView<>();
        TableColumn<TableEntry, Boolean> selectedColumn = new TooltipTableColumn<>("",
            "Include this PV when restoring values", 30, 30, false);
        selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectedColumn));
        selectedColumn.setEditable(true);
        selectedColumn.setSortable(false);
        selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.setOnAction(
            e -> table.getItems().forEach(te -> te.selectedProperty().setValue(selectAllCheckBox.isSelected())));
        selectedColumn.setGraphic(selectAllCheckBox);

        TableColumn<TableEntry, Integer> idColumn = new TooltipTableColumn<>("#",
            "The order number of the PV in the beamline set", 30, 30, false);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<TableEntry, String> pvNameColumn = new TooltipTableColumn<>("PV", "The name of the PV", 170);
        pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));

        TableColumn<TableEntry, VType> liveValueColumn = new TooltipTableColumn<>("Setpoint Value", "Current PV value",
            -1);
        liveValueColumn.setCellValueFactory(new PropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setEditable(false);

        TableColumn<TableEntry, ?> storedValueColumn = new TooltipTableColumn<>("Stored Values",
            "PV value when the snapshot was taken", -1);
        TableColumn<TableEntry, VTypePair> baseCol = new TooltipTableColumn<>(
            "Base (" + Utilities.DELTA_CHAR + " Setpoint)", "PV value when the snapshot was taken", 100);
        baseCol.setCellValueFactory(e -> e.getValue().valueProperty());
        baseCol.setCellFactory(e -> new VTypeCellEditor<>());
        baseCol.setEditable(true);
        baseCol.setOnEditCommit(e -> {
            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, e.getNewValue().value));
            controller.resume();
        });
        storedValueColumn.getColumns().add(baseCol);
        for (int i = 1; i < n; i++) {
            TableColumn<TableEntry, VTypePair> col = new TooltipTableColumn<>("", "", 100);
            Label label = new Label(controller.getSnapshot(i).toString() + " (" + Utilities.DELTA_CHAR + " Base)");
            label.setTooltip(new Tooltip("PV value when the snapshot was taken"));
            col.setGraphic(label);
            final int snapshotIndex = i;
            MenuItem removeItem = new MenuItem("Remove");
            removeItem.setOnAction(ev -> SaveRestoreService.getInstance().execute("Remove Snapshot", () -> {
                final List<TableEntry> entries = controller.removeSnapshot(snapshotIndex);
                final int numberOfSnapshots = controller.getNumberOfSnapshots();
                final boolean show = controller.isShowReadbacks();
                Platform.runLater(() -> createTable(entries, numberOfSnapshots, show));
            }));
            MenuItem setAsBaseItem = new MenuItem("Set As Base");
            setAsBaseItem.setOnAction(ev -> SaveRestoreService.getInstance().execute("Remove Snapshot", () -> {
                final List<TableEntry> entries = controller.setAsBase(snapshotIndex);
                final int numberOfSnapshots = controller.getNumberOfSnapshots();
                final boolean show = controller.isShowReadbacks();
                Platform.runLater(() -> createTable(entries, numberOfSnapshots, show));
            }));
            final ContextMenu menu = new ContextMenu(removeItem, setAsBaseItem);
            label.setContextMenu(menu);
            col.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshotIndex));
            col.setCellFactory(e -> new VTypeCellEditor<>());
            col.setEditable(true);
            col.setOnEditCommit(e -> {
                ObjectProperty<VTypePair> value = e.getRowValue().compareValueProperty(snapshotIndex);
                value.setValue(new VTypePair(value.get().base, e.getNewValue().value));
                controller.resume();
            });
            label.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    menu.show(label, e.getScreenX(), e.getScreenY());
                }
            });
            storedValueColumn.getColumns().add(col);
        }
        if (showReadback) {
            TableColumn<TableEntry, VType> readbackColumn = new TooltipTableColumn<>(
                "Readback (" + Utilities.DELTA_CHAR + " Setpoint)", "Current Readback value", 100);
            readbackColumn.setCellValueFactory(new PropertyValueFactory<>("readback"));
            readbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
            readbackColumn.setEditable(false);
            table.getColumns().addAll(Arrays.asList(selectedColumn, idColumn, pvNameColumn, storedValueColumn,
                liveValueColumn, readbackColumn));
        } else {
            table.getColumns()
                .addAll(Arrays.asList(selectedColumn, idColumn, pvNameColumn, storedValueColumn, liveValueColumn));
        }
        return table;
    }

    private void configureTableForDnD(TableView<TableEntry> table) {
        table.setOnDragOver(e -> {
            if (e.getGestureSource() != table && (e.getDragboard().hasContent(SnapshotDataFormat.INSTANCE)
                || e.getDragboard().hasContent(DataFormat.FILES))) {
                e.acceptTransferModes(TransferMode.ANY);
            }
            e.consume();
        });
        table.setOnDragDropped(e -> {
            if (e.getDragboard().hasContent(SnapshotDataFormat.INSTANCE)) {
                Snapshot s = (Snapshot) e.getDragboard().getContent(SnapshotDataFormat.INSTANCE);
                if (s != null) {
                    final DataProvider provider = SaveRestoreService.getInstance()
                        .getDataProvider(s.getBeamlineSet().getDataProviderId()).provider;
                    SaveRestoreService.getInstance().execute("Load snapshot data", () -> {
                        try {
                            addSnapshot(provider.getSnapshotContent(s));
                        } catch (DataProviderException ex) {
                            Selector.reportException(ex, getSite().getShell());
                        }
                    });
                }
            } else if (e.getDragboard().hasContent(DataFormat.FILES)) {
                @SuppressWarnings("unchecked")
                final List<File> files = (List<File>) e.getDragboard().getContent(DataFormat.FILES);
                if (files.size() > 0) {
                    SaveRestoreService.getInstance().execute("Open file",
                        () -> files.forEach(c -> controller.openFromFile(c).ifPresent(s -> addSnapshot(s))));
                }
            }
        });
    }

    private void createTable(List<TableEntry> entries, int numSnapshots, boolean showReadback) {
        if (numSnapshots == 1) {
            table = createTableForSingleSnapshot(showReadback);
        } else {
            table = createTableForMultipleSnapshots(numSnapshots, showReadback);
        }
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setMaxWidth(Double.MAX_VALUE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configureTableForDnD(table);
        table.setOnMouseReleased(e -> {
            contextMenu.setVisible(e.getButton() == MouseButton.SECONDARY);
        });
        table.setOnMouseClicked(e -> clickedColumn = table.getSelectionModel().getSelectedCells().get(0).getColumn());
        table.getStylesheets().add(this.getClass().getResource(STYLE).toExternalForm());
        table.getItems().setAll(entries);
        entries.forEach(e -> {
            e.selectedProperty()
                .addListener((a, o, n) -> {
                    selectAllCheckBox.setSelected(n ? selectAllCheckBox.isSelected() : false);
                });
            e.liveStoredEqualProperty().addListener((a,o,n)->{
                if (controller.isHideEqualItems()) {
                    if (n) {
                        table.getItems().remove(e);
                    } else {
                        table.getItems().add(e);
                    }
                }
            });
            if (controller.isHideEqualItems() && e.liveStoredEqualProperty().get()) {
                table.getItems().remove(e);
            }
        });
        contentPane.setCenter(table);
    }

    /**
     * Set the base snapshot to be displayed in this editor. When the base snapshot is set, the editor is emptied first.
     * The meta data of the base snapshot are displayed at the top of the editor (comment, date, creator).
     *
     * @param data the snapshot data to set
     */
    public void setSnapshot(final VSnapshot data) {
        SaveRestoreService.getInstance().execute("Open Snapshot", () -> {
            final List<TableEntry> entries = controller.setSnapshot(data);
            final int num = controller.getNumberOfSnapshots();
            final boolean show = controller.isShowReadbacks();
            Platform.runLater(() -> {
                data.getSnapshot().ifPresent(t -> {
                    commentField.setText(t.getComment());
                    creatorField.setText(t.getOwner());
                    dateField.setText(Utilities.timestampToBigEndianString(t.getDate(), true));
                });
                createTable(entries, num, show);
            });
        });
    }

    /**
     * Adds a snapshot to this editor. This snapshot is compared to the base snapshot.
     *
     * @param data the snapshot data
     */
    public void addSnapshot(VSnapshot data) {
        SaveRestoreService.getInstance().execute("Add Snapshot", () -> {
            final List<TableEntry> entries = controller.addSnapshot(data);
            final int num = controller.getNumberOfSnapshots();
            final boolean show = controller.isShowReadbacks();
            Platform.runLater(() -> createTable(entries, num, show));
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.ui.util.FXEditorPart#setFxFocus()
     */
    @Override
    public void setFxFocus() {
        if (table != null) {
            table.requestFocus();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    @Override
    public ISelection getSelection() {
        if (table == null) {
            return null;
        } else {
            Timestamp timestamp = null;
            if (controller.getNumberOfSnapshots() == 1 || clickedColumn < 0) {
                timestamp = controller.getSnapshot(0).getTimestamp();
            } else {
                timestamp = controller.getSnapshot(clickedColumn - 3).getTimestamp();
            }
            if (timestamp == null) {
                List<ProcessVariable> list = new ArrayList<>();
                for (TableEntry e : table.selectionModelProperty().get().getSelectedItems()) {
                    list.add(new ProcessVariable(e.pvNameProperty().get()));
                }
                return new StructuredSelection(list);
            } else {
                long time = timestamp.toDate().getTime();
                List<TimestampedPV> list = new ArrayList<>();
                for (TableEntry e : table.selectionModelProperty().get().getSelectedItems()) {
                    list.add(new TimestampedPV(e.pvNameProperty().get(), time));
                }
                return new StructuredSelection(list);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.
     * ISelectionChangedListener)
     */
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.
     * ISelectionChangedListener)
     */
    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void setSelection(ISelection selection) {
    }
}
