package de.furkan.filehashchecker;

import com.google.gson.stream.JsonReader;
import sun.misc.Signal;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class FileHashChecker {

    public static void main(String[] args) {
        new Core(args);
    }

}

class Core {

    private static Core instance;
    private final List<String> checkedFiles = new ArrayList<>(), invalidFiles = new ArrayList<>(), notFoundFiles = new ArrayList<>();
    private boolean verbose;
    private ConfigManager configManager;
    private List<File> allFiles = new ArrayList<>();

    public Core(String[] args) {
        System.out.println("\n [FileHashChecker created by Furkan | Made in Germany]");
        if (args.length == 0) {
            System.out.println("No config file provided!");
            return;
        }
        verbose = args.length != 1 && args[1].equalsIgnoreCase("-v");
        instance = this;
        configManager = new ConfigManager();

        configManager.loadConfiguration(new File(args[0]));
        executeWithConfig();
    }

    public static Core getInstance() {
        return instance;
    }

    public List<String> getInvalidFiles() {
        return invalidFiles;
    }

    public List<String> getCheckedFiles() {
        return checkedFiles;
    }

    public List<String> getNotFoundFiles() {
        return notFoundFiles;
    }

    public List<File> getAllFiles() {
        return allFiles;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    private void executeWithConfig() {
        if (configManager.getCurrentConfig() == null) {
            sendVerboseMessage(getClass(), "No config loaded!");
            return;
        }
        try {
            SortedMap<File, String> fileHashMap = new TreeMap<>();
            JsonReader jsonReader = new JsonReader(new FileReader(configManager.getCurrentConfig().checkFile()));
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                fileHashMap.put(new File(jsonReader.nextName()), jsonReader.nextString());
            }
            jsonReader.close();
            allFiles = new ArrayList<>(fileHashMap.keySet());
            HashThread hashThread = new HashThread(true, fileHashMap, null);
            Signal.handle(new Signal("INT"), signal -> hashThread.abortOperation());
        } catch (Exception e) {
            System.out.println("Error while reading checkFile!");
            e.printStackTrace();
        }


    }


    public void sendVerboseMessage(Class c, String message) {
        if (!verbose) {
            return;
        }
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss")) + "] " + c.getSimpleName() + ": " + message);
    }

}
