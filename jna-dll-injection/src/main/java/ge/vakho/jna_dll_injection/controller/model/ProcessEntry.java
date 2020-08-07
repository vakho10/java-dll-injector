package ge.vakho.jna_dll_injection.controller.model;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProcessEntry {

    private LongProperty processId;
    private StringProperty name;
    private StringProperty command;

    public void setProcessId(long value) {
        processIdProperty().set(value);
    }

    public long getProcessId() {
        return processIdProperty().get();
    }

    public LongProperty processIdProperty() {
        if (processId == null) {
            processId = new SimpleLongProperty(this, "processId");
        }
        return processId;
    }

    public void setName(String value) {
        nameProperty().set(value);
    }

    public String getName() {
        return nameProperty().get();
    }

    public StringProperty nameProperty() {
        if (name == null) {
            name = new SimpleStringProperty(this, "name");
        }
        return name;
    }

    public void setCommand(String value) {
        commandProperty().set(value);
    }

    public String getCommand() {
        return commandProperty().get();
    }

    public StringProperty commandProperty() {
        if (command == null) {
            command = new SimpleStringProperty(this, "command");
        }
        return command;
    }

    public static ProcessEntry fromProcessHandle(ProcessHandle processHandle) {
        ProcessEntry processEntry = new ProcessEntry();
        processEntry.setProcessId(processHandle.pid());
        ProcessHandle.Info info = processHandle.info();
        String command = info.command().orElse("Noname");
        processEntry.setCommand(command);

        // Derive name from command
        int idx = command.lastIndexOf("/") != -1 ? command.lastIndexOf("/") : command.lastIndexOf("\\");
        processEntry.setName(idx != -1 ? command.substring(idx + 1) : "Noname");
        return processEntry;
    }
}
