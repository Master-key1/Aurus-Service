package com.auruspay.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.auruspay.decryptor.AurusDecryptor;
import com.auruspay.filewriter.ExcelWriter;
import com.auruspay.filewriter.TxtWriter;

public class MainRunner {

    public static void main(String[] args) {

        String txnId ="295260853503884501";

        try {

            SshService sshService = new SshService();
            LogParserService parser = new LogParserService();
            FileExportService fileService = new FileExportService();

            String txnLogs = sshService.fetchTxnLogs(txnId);
            String uuid = parser.extractUUID(txnLogs);

            if (uuid == null) {
                System.out.println("UUID not found");
                return;
            }

            String uuidLogs = sshService.fetchUUIDLogs(uuid);

            Map<String, Object> map = parser.parse(uuidLogs, uuid, txnId);

            fileService.export(txnId, map, uuidLogs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class SshService {

    private static final String LOG_PATH = "/opt/auruspay_switch/log/auruspay/";

    public String fetchTxnLogs(String txnId) throws Exception {

        return execute(buildCommand(txnId));
    }

    public String fetchUUIDLogs(String uuid) throws Exception {

        return execute(buildCommand(uuid));
    }

    private String buildCommand(String value) {

        return "zgrep --text '" + value + "' " + LOG_PATH + "auruspay.log";
    }

    private String execute(String command) throws Exception {

        // TODO: move credentials to config
        // same logic as your executeCommand()

        return "LOG_OUTPUT"; // replace with actual
    }
}
class LogParserService {

    public String extractUUID(String logs) {

        Matcher m = Pattern.compile("[a-f0-9\\-]{36}").matcher(logs);
        return m.find() ? m.group() : null;
    }

    public Map<String, Object> parse(String logs, String uuid, String txnId) {

        Map<String, Object> map = new HashMap<>();
        List<String> issues = new ArrayList<>();

        map.put("TXNID", txnId);
        map.put("UUID", uuid);

        for (String line : logs.split("\n")) {

            if (!line.contains(uuid)) continue;

            if (line.contains("AURUSPAY ENCRYPTED REQUEST")) {
                put(map, line, "AURUSPAY ENCRYPTED REQUEST :", "AurusReq");
            }

            if (line.contains("REQUEST :")) {
                put(map, line, "REQUEST :", "ProcReq");
            }

            if (line.contains("FINAL RESPONSE")) {
                put(map, line, "FINAL RESPONSE :", "ProcRes");
            }

            if (line.contains("AURUSPAY ENCRYPTED RESPONSE")) {
                put(map, line, "AURUSPAY ENCRYPTED RESPONSE :", "AurusRes");
            }

            if (line.matches(".*(ERROR|Exception|Timeout|Declined|Failed).*")) {
                issues.add(line);
            }
        }

        map.put("Issues", issues);

        return map;
    }

    private void put(Map<String, Object> map, String line, String key, String prefix) {

        try {
            String value = line.substring(line.indexOf(key) + key.length());
            String decrypted = AurusDecryptor.decryptor(value);

            map.put(prefix + "Encrypt", value);
            map.put(prefix + "Decrypt", decrypted);

        } catch (Exception e) {
            map.put(prefix + "Decrypt", "DECRYPTION_FAILED");
        }
    }
}

class FileExportService {

    public void export(String txnId, Map<String, Object> map, String logs) throws Exception {

        String path = "C:/logs/" + txnId;

        ExcelWriter excel = new ExcelWriter(path + ".xlsx");
        excel.addMapAsSingleRow(map);
        excel.save();

        TxtWriter txt = new TxtWriter(path + ".txt");
        txt.writeMapWithLogs(map, logs);
    }
}