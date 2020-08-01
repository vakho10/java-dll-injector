package ge.vakho.jna_dll_injection;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demonstrates simple DLL injection using JNA library.
 */
public class App {

    public static void main(String[] args) {
        String processPathString = "C:\\WINDOWS\\system32\\notepad.exe";
        Path dllPath = Paths.get("..\\hello-world-dll\\hello-world-dll\\bin\\x64\\Release\\hello-world-dll.dll");
        new Injector(processPathString, dllPath).run();
    }
}
