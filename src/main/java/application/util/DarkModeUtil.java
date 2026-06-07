package application.util;

/**
 * Enables the native Windows 10/11 dark mode title bar for the application window.
 * Uses a PowerShell call to DwmSetWindowAttribute via a small inline script,
 * requiring zero additional native libraries.
 */
public class DarkModeUtil {

    public static void setDarkModeForWindow(String windowTitle) {
        // Only runs on Windows; silently skipped on other OS.
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        try {
            // Give the window a moment to render before looking it up
            Thread.sleep(300);
            String script =
                "Add-Type -TypeDefinition @'\n" +
                "using System;\n" +
                "using System.Runtime.InteropServices;\n" +
                "public class DwmHelper {\n" +
                "  [DllImport(\"dwmapi.dll\")]\n" +
                "  public static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int attrValue, int attrSize);\n" +
                "  [DllImport(\"user32.dll\", SetLastError = true)]\n" +
                "  public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);\n" +
                "}\n" +
                "'@;\n" +
                "$hwnd = [DwmHelper]::FindWindow($null, '" + windowTitle.replace("'", "''") + "');\n" +
                "if ($hwnd -ne [IntPtr]::Zero) {\n" +
                "  $dark = 1;\n" +
                "  [DwmHelper]::DwmSetWindowAttribute($hwnd, 20, [ref]$dark, 4) | Out-Null;\n" +
                "}";

            new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
                .redirectErrorStream(true)
                .start();
        } catch (Exception e) {
            System.err.println("[DarkModeUtil] Could not apply dark title bar: " + e.getMessage());
        }
    }
}
