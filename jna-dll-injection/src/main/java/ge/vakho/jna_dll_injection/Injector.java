package ge.vakho.jna_dll_injection;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.sun.jna.platform.win32.WinBase.WAIT_OBJECT_0;
import static com.sun.jna.platform.win32.WinNT.*;

public class Injector implements Runnable {

    private static final Logger log = LogManager.getLogger();

    private int processId = -1;
    private Path processPath;
    private Path dllPath;

    public Injector(int processId, String dllPathString) {
        this(processId, Paths.get(dllPathString));
    }

    public Injector(int processId, Path dllPath) {
        this.processId = processId;
        this.dllPath = dllPath;
    }

    public Injector(String processPathString, String dllPathString) {
        this(Paths.get(processPathString), Paths.get(dllPathString));
    }

    public Injector(String processPathString, Path dllPath) {
        this(Paths.get(processPathString), dllPath);
    }

    public Injector(Path processPath, Path dllPath) {
        this.processPath = processPath;
        this.dllPath = dllPath;
    }

    /**
     * This method does all the work.
     * It takes DLL and injects into specified process and executes it.
     */
    @Override
    public void run() {

        // Construct DLL path with string terminator at the end ('\0')
        final String DllPath = dllPath.toAbsolutePath().toString() + '\0';

        WinNT.HANDLE hProcess = null;

        // (1) Start new process or inject into the existing one?
        if (processId > -1) {
            hProcess = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, processId);
        } else {
            WinBase.STARTUPINFO startupInfo = new WinBase.STARTUPINFO();
            WinBase.PROCESS_INFORMATION.ByReference processInformation = new WinBase.PROCESS_INFORMATION.ByReference();

            // run some-app.exe in a new process
            boolean status = Kernel32.INSTANCE.CreateProcess(
                    processPath.toAbsolutePath().toString(),
                    null,
                    null,
                    null,
                    false,
                    new WinDef.DWORD(WinBase.CREATE_DEFAULT_ERROR_MODE),
                    Pointer.NULL,
                    null,
                    startupInfo,
                    processInformation);

            if (!status || processInformation.dwProcessId.longValue() <= 0) {
                throw new RuntimeException("Couldn't create the process!");
            }

            hProcess = processInformation.hProcess;
        }

        // (2) Allocate memory for the DLL's path in the target process
        Pointer pDllPath = Kernel32.INSTANCE.VirtualAllocEx(hProcess, null,
                new BaseTSD.SIZE_T(DllPath.length()),
                MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);

        // (3) Write the path to the address of the memory we just allocated in the target process
        // (Write from the source buffer to allocated pDllPath)
        ByteBuffer bufSrc = ByteBuffer.allocateDirect(DllPath.length());
        bufSrc.put(DllPath.getBytes());

        Pointer ptrSrc = Native.getDirectBufferPointer(bufSrc);

        IntByReference bytesWritten = new IntByReference(); // This may be 'null' if we aren't interested
        Kernel32.INSTANCE.WriteProcessMemory(hProcess, pDllPath, ptrSrc, DllPath.length(), bytesWritten);
        if (bytesWritten.getValue() != DllPath.length()) {
            throw new RuntimeException("Wrong amount of bytes written! Is: " + bytesWritten.getValue() + "! Should've been: " + DllPath.length());
        }

        // (4) Create a Remote Thread in the target process which
        // calls LoadLibraryA with our dllpath as an argument -> program loads our dll
        NativeLibrary kernel32Library = NativeLibrary.getInstance("kernel32");
        Function LoadLibraryAFunction = kernel32Library.getFunction("LoadLibraryA");

        DWORDByReference threadId = new DWORDByReference(); // This may also be 'null' if we aren't interested
        HANDLE hThread = Kernel32.INSTANCE.CreateRemoteThread(hProcess, null, 0,
                LoadLibraryAFunction, pDllPath, 0, threadId);

        // (5) Wait for the execution of our loader thread to finish
        int waitResult = Kernel32.INSTANCE.WaitForSingleObject(hThread, 10 * 1000); // Wait 10 seconds (or INFINITE?)
        if (WAIT_OBJECT_0 != waitResult) {
            throw new RuntimeException("Something went wrong during waiting for execution of our loader thread to finish!");
        }

        log.info("Dll path allocated at: {}", pDllPath);

        // (6) Free the memory allocated for our dll path
        // TODO should I delete this?
        if (!Kernel32.INSTANCE.VirtualFreeEx(hProcess, pDllPath, new SIZE_T(0), WinNT.MEM_RELEASE)) {
            throw new RuntimeException("Couldn't delete the memory we've allocated for pDllPath string value!");
        }
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public Path getProcessPath() {
        return processPath;
    }

    public void setProcessPath(Path processPath) {
        this.processPath = processPath;
    }

    public Path getDllPath() {
        return dllPath;
    }

    public void setDllPath(Path dllPath) {
        this.dllPath = dllPath;
    }
}
