package ge.vakho.jna_dll_injection.controller;

import ge.vakho.jna_dll_injection.Injector;
import ge.vakho.jna_dll_injection.controller.model.ProcessEntry;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {

    private static final Logger log = LogManager.getLogger();

    private final ObservableList<ProcessEntry> processEntriesList = FXCollections.observableArrayList();
    private List<Path> selectedDllPaths = new ArrayList<>();

    @FXML
    TextField textFieldFindFilter;

    @FXML
    Label labelRunningProcesses;

    @FXML
    TextField textFieldDllPath;

    @FXML
    TableView<ProcessEntry> tableViewProcesses;

    @FXML
    TableColumn<ProcessEntry, Long> tableColumnPid;

    @FXML
    TableColumn<ProcessEntry, String> tableColumnCommand;

    @FXML
    TableColumn<ProcessEntry, String> tableColumnName;

    @FXML
    Button buttonRefreshProcesses;

    @FXML
    Button buttonInjectDll;

    @FXML
    public void initialize() {
        log.info("Initialized MainController");

        // Set items for processes' TableView
        tableViewProcesses.setItems(processEntriesList);

        // Set columns factories
        tableColumnPid.setCellValueFactory(p -> p.getValue().processIdProperty().asObject());
        tableColumnName.setCellValueFactory(p -> p.getValue().nameProperty());
        tableColumnCommand.setCellValueFactory(p -> p.getValue().commandProperty());

        tableViewProcesses.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !selectedDllPaths.isEmpty()) {
                buttonInjectDll.setDisable(false);
            } else {
                buttonInjectDll.setDisable(true);
            }
        });

        // Add filter event listener for name or id
        textFieldFindFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            refreshProcesses();
        });

        refreshProcesses();
    }

    @FXML
    protected void handleRefreshProcessesButtonAction(ActionEvent event) {
        refreshProcesses();
    }

    @FXML
    protected void handleChooseDllButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(Paths.get(".").toFile());
        fileChooser.setTitle("Select DLL file to inject");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DLL Files", "*.dll")
        );

        // Get stage for file chooser...
        final Node source = (Node) event.getSource();
        final Window mainStage = source.getScene().getWindow();

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(mainStage);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {

            // Clear old paths
            selectedDllPaths.clear();

            // Add new paths
            selectedFiles.forEach(selectedFile -> {
                selectedDllPaths.add(selectedFile.toPath().toAbsolutePath());
                textFieldDllPath.setText(selectedDllPaths.toString());
            });
        }
    }

    @FXML
    protected void handleInjectDllButtonAction(ActionEvent event) {

        // Disable inject button
        buttonInjectDll.setDisable(true);

        try {
            // Get parameters
            ProcessEntry processEntry = tableViewProcesses.getSelectionModel().getSelectedItem();

            if (!selectedDllPaths.isEmpty()) {
                // Inject DLLs
                selectedDllPaths.forEach(selectedDllPath -> new Injector((int) processEntry.getProcessId(), selectedDllPath).run());
            }
        } finally {
            buttonInjectDll.setDisable(false);
        }
    }

    private void refreshProcesses() {

        // Disable refresh button
        buttonRefreshProcesses.setDisable(true);

        // Clear old results
        processEntriesList.clear();

        try (Stream<ProcessHandle> processesStream = ProcessHandle.allProcesses()) {
            List<ProcessEntry> processEntries = processesStream
                    .map(ProcessEntry::fromProcessHandle)
                    .filter(i -> {
                        final String searchString = textFieldFindFilter.getText().trim();
                        if (searchString == null || searchString.isBlank()) {
                            return true;
                        }
                        long longSearchValue = -1;
                        try {
                            longSearchValue = Long.parseLong(searchString);
                        } catch (NumberFormatException e) {
                        }
                        return i.getProcessId() == longSearchValue
                                || i.getName().toLowerCase().contains(searchString.toLowerCase());
                    })
                    .collect(Collectors.toList());

            // Update label with amount of processes running
            labelRunningProcesses.setText("Running processes (" + processEntries.size() + ")");

            processEntriesList.addAll(processEntries);
        } finally {
            buttonRefreshProcesses.setDisable(false);
        }
    }

}
