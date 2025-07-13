package me.nytesky.slotbinding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ConfigManager {
    private static final File CONFIG_FILE = new File("config/slotbinding.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config config = new Config(); // default fallback

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Config loaded = GSON.fromJson(reader, Config.class);

                // handle empty or partially empty config
                if (loaded == null) {
                    System.out.println("[SlotBinding] Config file is empty or malformed. Using defaults.");
                    config = new Config();
                } else {
                    config = loaded;
                    if (config.slotBinds == null) config.slotBinds = new HashMap<>();
                }

            } catch (Exception e) {
                System.err.println("[SlotBinding] Failed to load config. Using defaults.");
                e.printStackTrace();
                config = new Config();  // fallback on error
            }
        } else {
            config = new Config(); // file doesn’t exist so use defaults
        }

        save();  // write any missing/default fields back to file
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[SlotBinding] Failed to save config.");
            e.printStackTrace();
        }
    }
}


