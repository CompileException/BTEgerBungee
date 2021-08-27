package de.lucas.btegermany.bteteleport.bungee;

/*
 * Copyright (c) BTE Germany 2021.
 * Plugin by Lucas L. - CompileException.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Metrics {
    public static final int B_STATS_VERSION = 1;

    private static final String URL = "https://bStats.org/submitData/bungeecord";

    private final Plugin plugin;

    private boolean enabled;

    private String serverUUID;

    static {
        if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
            String defaultPackage = new String(new byte[] {
                    111, 114, 103, 46, 98, 115, 116, 97, 116, 115,
                    46, 98, 117, 110, 103, 101, 101, 99, 111, 114,
                    100 });
            String examplePackage = new String(new byte[] {
                    121, 111, 117, 114, 46, 112, 97, 99, 107, 97,
                    103, 101 });
            if (Metrics.class.getPackage().getName().equals(defaultPackage) || Metrics.class.getPackage().getName().equals(examplePackage))
                throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
        }
    }

    private boolean logFailedRequests = false;

    private static boolean logSentData;

    private static boolean logResponseStatusText;

    private static final List<Object> knownMetricsInstances = new ArrayList();

    private final List<CustomChart> charts = new ArrayList<>();

    public Metrics(Plugin plugin) {
        this.plugin = plugin;
        try {
            loadConfig();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load bStats config!", e);
            return;
        }
        if (!this.enabled)
            return;
        Class<?> usedMetricsClass = getFirstBStatsClass();
        if (usedMetricsClass == null)
            return;
        if (usedMetricsClass == getClass()) {
            linkMetrics(this);
            startSubmitting();
        } else {
            try {
                usedMetricsClass.getMethod("linkMetrics", new Class[] { Object.class }).invoke(null, new Object[] { this });
            } catch (NoSuchMethodException|IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
                if (this.logFailedRequests)
                    plugin.getLogger().log(Level.WARNING, "Failed to link to first metrics class " + usedMetricsClass.getName() + "!", e);
            }
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void addCustomChart(CustomChart chart) {
        if (chart == null)
            this.plugin.getLogger().log(Level.WARNING, "Chart cannot be null");
        this.charts.add(chart);
    }

    public static void linkMetrics(Object metrics) {
        knownMetricsInstances.add(metrics);
    }

    public JsonObject getPluginData() {
        JsonObject data = new JsonObject();
        String pluginName = this.plugin.getDescription().getName();
        String pluginVersion = this.plugin.getDescription().getVersion();
        data.addProperty("pluginName", pluginName);
        data.addProperty("pluginVersion", pluginVersion);
        JsonArray customCharts = new JsonArray();
        for (CustomChart customChart : this.charts) {
            JsonObject chart = customChart.getRequestJsonObject(this.plugin.getLogger(), this.logFailedRequests);
            if (chart == null)
                continue;
            customCharts.add((JsonElement)chart);
        }
        data.add("customCharts", (JsonElement)customCharts);
        return data;
    }

    private void startSubmitting() {
        this.plugin.getProxy().getScheduler().schedule(this.plugin, this::submitData, 2L, 30L, TimeUnit.MINUTES);
    }

    private JsonObject getServerData() {
        int playerAmount = this.plugin.getProxy().getOnlineCount();
        playerAmount = (playerAmount > 500) ? 500 : playerAmount;
        int onlineMode = this.plugin.getProxy().getConfig().isOnlineMode() ? 1 : 0;
        String bungeecordVersion = this.plugin.getProxy().getVersion();
        int managedServers = this.plugin.getProxy().getServers().size();
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        int coreCount = Runtime.getRuntime().availableProcessors();
        JsonObject data = new JsonObject();
        data.addProperty("serverUUID", this.serverUUID);
        data.addProperty("playerAmount", Integer.valueOf(playerAmount));
        data.addProperty("managedServers", Integer.valueOf(managedServers));
        data.addProperty("onlineMode", Integer.valueOf(onlineMode));
        data.addProperty("bungeecordVersion", bungeecordVersion);
        data.addProperty("javaVersion", javaVersion);
        data.addProperty("osName", osName);
        data.addProperty("osArch", osArch);
        data.addProperty("osVersion", osVersion);
        data.addProperty("coreCount", Integer.valueOf(coreCount));
        return data;
    }

    private void submitData() {
        JsonObject data = getServerData();
        JsonArray pluginData = new JsonArray();
        for (Object metrics : knownMetricsInstances) {
            try {
                Object plugin = metrics.getClass().getMethod("getPluginData", new Class[0]).invoke(metrics, new Object[0]);
                if (plugin instanceof JsonObject)
                    pluginData.add((JsonElement)plugin);
            } catch (NoSuchMethodException|IllegalAccessException|java.lang.reflect.InvocationTargetException noSuchMethodException) {}
        }
        data.add("plugins", (JsonElement)pluginData);
        try {
            sendData(this.plugin, data);
        } catch (Exception e) {
            if (this.logFailedRequests)
                this.plugin.getLogger().log(Level.WARNING, "Could not submit plugin stats!", e);
        }
    }

    private void loadConfig() throws IOException {
        Path configPath = this.plugin.getDataFolder().toPath().getParent().resolve("bStats");
        configPath.toFile().mkdirs();
        File configFile = new File(configPath.toFile(), "config.yml");
        if (!configFile.exists())
            writeFile(configFile, new String[] { "#bStats collects some data for plugin authors like how many servers are using their plugins.", "#To honor their work, you should not disable it.", "#This has nearly no effect on the server performance!", "#Check out https://bStats.org/ to learn more :)", "enabled: true", "serverUuid: \"" +

                    UUID.randomUUID().toString() + "\"", "logFailedRequests: false", "logSentData: false", "logResponseStatusText: false" });
        Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        this.enabled = configuration.getBoolean("enabled", true);
        this.serverUUID = configuration.getString("serverUuid");
        this.logFailedRequests = configuration.getBoolean("logFailedRequests", false);
        logSentData = configuration.getBoolean("logSentData", false);
        logResponseStatusText = configuration.getBoolean("logResponseStatusText", false);
    }

    private Class<?> getFirstBStatsClass() {
        Path configPath = this.plugin.getDataFolder().toPath().getParent().resolve("bStats");
        configPath.toFile().mkdirs();
        File tempFile = new File(configPath.toFile(), "temp.txt");
        try {
            String className = readFile(tempFile);
            if (className != null)
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException classNotFoundException) {}
            writeFile(tempFile, new String[] { getClass().getName() });
            return getClass();
        } catch (IOException e) {
            if (this.logFailedRequests)
                this.plugin.getLogger().log(Level.WARNING, "Failed to get first bStats class!", e);
            return null;
        }
    }

    private String readFile(File file) throws IOException {
        if (!file.exists())
            return null;
        try(FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            return bufferedReader.readLine();
        }
    }

    private void writeFile(File file, String... lines) throws IOException {
        if (!file.exists())
            file.createNewFile();
        try(FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }

    private static void sendData(Plugin plugin, JsonObject data) throws Exception {
        if (data == null)
            throw new IllegalArgumentException("Data cannot be null");
        if (logSentData)
            plugin.getLogger().info("Sending data to bStats: " + data.toString());
        HttpsURLConnection connection = (HttpsURLConnection)(new URL("https://bStats.org/submitData/bungeecord")).openConnection();
        byte[] compressedData = compress(data.toString());
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "MC-Server/1");
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(compressedData);
        outputStream.flush();
        outputStream.close();
        InputStream inputStream = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null)
            builder.append(line);
        bufferedReader.close();
        if (logResponseStatusText)
            plugin.getLogger().info("Sent data to bStats and received response: " + builder.toString());
    }

    private static byte[] compress(String str) throws IOException {
        if (str == null)
            return null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
        gzip.write(str.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        return outputStream.toByteArray();
    }

    public static abstract class CustomChart {
        private final String chartId;

        CustomChart(String chartId) {
            if (chartId == null || chartId.isEmpty())
                throw new IllegalArgumentException("ChartId cannot be null or empty!");
            this.chartId = chartId;
        }

        private JsonObject getRequestJsonObject(Logger logger, boolean logFailedRequests) {
            JsonObject chart = new JsonObject();
            chart.addProperty("chartId", this.chartId);
            try {
                JsonObject data = getChartData();
                if (data == null)
                    return null;
                chart.add("data", (JsonElement)data);
            } catch (Throwable t) {
                if (logFailedRequests)
                    logger.log(Level.WARNING, "Failed to get data for custom chart with id " + this.chartId, t);
                return null;
            }
            return chart;
        }

        protected abstract JsonObject getChartData() throws Exception;
    }

    public static class SimplePie extends CustomChart {
        private final Callable<String> callable;

        public SimplePie(String chartId, Callable<String> callable) {
            super(chartId);
            this.callable = callable;
        }

        protected JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            String value = this.callable.call();
            if (value == null || value.isEmpty())
                return null;
            data.addProperty("value", value);
            return data;
        }
    }

    public static class AdvancedPie extends CustomChart {
        private final Callable<Map<String, Integer>> callable;

        public AdvancedPie(String chartId, Callable<Map<String, Integer>> callable) {
            super(chartId);
            this.callable = callable;
        }

        protected JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            JsonObject values = new JsonObject();
            Map<String, Integer> map = this.callable.call();
            if (map == null || map.isEmpty())
                return null;
            boolean allSkipped = true;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (((Integer)entry.getValue()).intValue() == 0)
                    continue;
                allSkipped = false;
                values.addProperty(entry.getKey(), entry.getValue());
            }
            if (allSkipped)
                return null;
            data.add("values", (JsonElement)values);
            return data;
        }
    }

    public static class DrilldownPie extends CustomChart {
        private final Callable<Map<String, Map<String, Integer>>> callable;

        public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
            super(chartId);
            this.callable = callable;
        }

        public JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            JsonObject values = new JsonObject();
            Map<String, Map<String, Integer>> map = this.callable.call();
            if (map == null || map.isEmpty())
                return null;
            boolean reallyAllSkipped = true;
            for (Map.Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
                JsonObject value = new JsonObject();
                boolean allSkipped = true;
                for (Map.Entry<String, Integer> valueEntry : (Iterable<Map.Entry<String, Integer>>)((Map)map.get(entryValues.getKey())).entrySet()) {
                    value.addProperty(valueEntry.getKey(), valueEntry.getValue());
                    allSkipped = false;
                }
                if (!allSkipped) {
                    reallyAllSkipped = false;
                    values.add(entryValues.getKey(), (JsonElement)value);
                }
            }
            if (reallyAllSkipped)
                return null;
            data.add("values", (JsonElement)values);
            return data;
        }
    }

    public static class SingleLineChart extends CustomChart {
        private final Callable<Integer> callable;

        public SingleLineChart(String chartId, Callable<Integer> callable) {
            super(chartId);
            this.callable = callable;
        }

        protected JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            int value = ((Integer)this.callable.call()).intValue();
            if (value == 0)
                return null;
            data.addProperty("value", Integer.valueOf(value));
            return data;
        }
    }

    public static class MultiLineChart extends CustomChart {
        private final Callable<Map<String, Integer>> callable;

        public MultiLineChart(String chartId, Callable<Map<String, Integer>> callable) {
            super(chartId);
            this.callable = callable;
        }

        protected JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            JsonObject values = new JsonObject();
            Map<String, Integer> map = this.callable.call();
            if (map == null || map.isEmpty())
                return null;
            boolean allSkipped = true;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (((Integer)entry.getValue()).intValue() == 0)
                    continue;
                allSkipped = false;
                values.addProperty(entry.getKey(), entry.getValue());
            }
            if (allSkipped)
                return null;
            data.add("values", (JsonElement)values);
            return data;
        }
    }

    public static class SimpleBarChart extends CustomChart {
        private final Callable<Map<String, Integer>> callable;

        public SimpleBarChart(String chartId, Callable<Map<String, Integer>> callable) {
            super(chartId);
            this.callable = callable;
        }

        protected JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            JsonObject values = new JsonObject();
            Map<String, Integer> map = this.callable.call();
            if (map == null || map.isEmpty())
                return null;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                JsonArray categoryValues = new JsonArray();
                categoryValues.add((JsonElement)new JsonPrimitive(entry.getValue()));
                values.add(entry.getKey(), (JsonElement)categoryValues);
            }
            data.add("values", (JsonElement)values);
            return data;
        }
    }

    public static class AdvancedBarChart extends CustomChart {
        private final Callable<Map<String, int[]>> callable;

        public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
            super(chartId);
            this.callable = callable;
        }

        protected JsonObject getChartData() throws Exception {
            JsonObject data = new JsonObject();
            JsonObject values = new JsonObject();
            Map<String, int[]> map = this.callable.call();
            if (map == null || map.isEmpty())
                return null;
            boolean allSkipped = true;
            for (Map.Entry<String, int[]> entry : map.entrySet()) {
                if (((int[])entry.getValue()).length == 0)
                    continue;
                allSkipped = false;
                JsonArray categoryValues = new JsonArray();
                for (int categoryValue : (int[])entry.getValue())
                    categoryValues.add((JsonElement)new JsonPrimitive(Integer.valueOf(categoryValue)));
                values.add(entry.getKey(), (JsonElement)categoryValues);
            }
            if (allSkipped)
                return null;
            data.add("values", (JsonElement)values);
            return data;
        }
    }
}
