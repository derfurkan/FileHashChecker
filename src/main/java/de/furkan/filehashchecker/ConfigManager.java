package de.furkan.filehashchecker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class ConfigManager {

    private Config currentConfig;

    public Config getCurrentConfig() {
        return currentConfig;
    }

    public void loadConfiguration(File file) {
        Core.getInstance().sendVerboseMessage(getClass(), "Loading configuration from '" + file.getPath() + "'");
        if (!file.exists()) {
            Core.getInstance().sendVerboseMessage(getClass(), "Configuration file does not exist!");
            return;
        }
        if (file.length() == 0) {
            Core.getInstance().sendVerboseMessage(getClass(), "Configuration file is empty!");
            return;
        }
        JsonObject jsonObject;
        try {
            jsonObject = JsonParser.parseString(FileUtils.readFileToString(file, "UTF-8")).getAsJsonObject();
        } catch (IOException e) {
            Core.getInstance().sendVerboseMessage(getClass(), "Error while reading configuration file! (No valid Json file?)");
            return;
        }
        if (!jsonObject.has("checkFile") || !jsonObject.has("algorithm") || !jsonObject.has("multiThreading")) {
            Core.getInstance().sendVerboseMessage(getClass(), "Configuration file is missing required fields!");
            return;
        }
        Core.getInstance().sendVerboseMessage(getClass(), " - Check file: " + jsonObject.get("checkFile").getAsString());
        Core.getInstance().sendVerboseMessage(getClass(), " - Algorithm: " + jsonObject.get("algorithm").getAsString());
        Core.getInstance().sendVerboseMessage(getClass(), " - Multithreading: " + (jsonObject.get("multiThreading").getAsBoolean() ? "Enabled" : "Disabled"));
        Core.getInstance().sendVerboseMessage(getClass(), " - Threads: " + jsonObject.get("threads").getAsInt() + "\n");
        File checkFile;
        try {
            checkFile = new File(new File(Core.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParentFile() + "\\" + jsonObject.get("checkFile").getAsString());
        } catch (URISyntaxException e) {
            Core.getInstance().sendVerboseMessage(getClass(), "Error while getting check file!");
            return;
        }
        boolean isInvalid = false;
        if (!checkFile.exists()) {
            Core.getInstance().sendVerboseMessage(getClass(), "Check file (" + checkFile.getAbsolutePath() + ") does not exist!");
            isInvalid = true;
        }
        if (!checkFile.isFile() && checkFile.exists()) {
            Core.getInstance().sendVerboseMessage(getClass(), "Check file (" + checkFile.getAbsolutePath() + ") is a folder!");
            isInvalid = true;
        }
        if (checkFile.length() == 0 && checkFile.exists()) {
            Core.getInstance().sendVerboseMessage(getClass(), "Check file (" + checkFile.getAbsolutePath() + ") is empty!");
            isInvalid = true;
        }
        try {
            Algorithm.valueOf(jsonObject.get("algorithm").getAsString());
        } catch (Exception e) {
            Core.getInstance().sendVerboseMessage(getClass(), "Algorithm (" + jsonObject.get("algorithm").getAsString() + ") is not valid!");
            isInvalid = true;
        }


        if (isInvalid) {
            System.exit(0);
        } else {
            Config config;
            try {
                config = new Config(new File(Core.class.getProtectionDomain().getCodeSource().getLocation()
                        .toURI()).getParentFile(), checkFile, Algorithm.valueOf(jsonObject.get("algorithm").getAsString()), jsonObject.get("multiThreading").getAsBoolean(), jsonObject.get("threads").getAsInt());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            currentConfig = config;
        }

    }

}
