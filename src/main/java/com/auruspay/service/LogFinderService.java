package com.auruspay.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class LogFinderService {

    private static final Logger logger = Logger.getLogger(LogFinderService.class.getName());

    private static final String USERNAME = "kevalin";

    private static final String LOG_PATH = "/opt/auruspay_switch/log/auruspay/auruspay.log";

    private final Map<String, String> nodeMap = new HashMap<>();

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F\\-]{36}");

    public LogFinderService() {

        logger.info("Initializing LogFinderService and loading node map");

        nodeMap.put("91", "192.168.50.152");
        nodeMap.put("92", "192.168.50.153");
        nodeMap.put("93", "192.168.50.172");
        nodeMap.put("94", "192.168.50.72");
        nodeMap.put("95", "192.168.50.155");
        nodeMap.put("96", "192.168.50.196");
        nodeMap.put("97", "192.168.50.69");

        logger.info("Node map loaded with size: " + nodeMap.size());
    }

    // Step-1
    public String resolveNodeIP(String txnId) {

        logger.info("Resolving node IP for transaction ID: " + txnId);

        String nodeKey = txnId.substring(1, 3);

        logger.info("Extracted node key: " + nodeKey);

        String ip = nodeMap.get(nodeKey);

        if (ip == null) {
            logger.warning("No node mapping found for key: " + nodeKey);
        } else {
            logger.info("Resolved node IP: " + ip);
        }

        return ip;
    }

    private String buildCommand(String searchValue, String ip, boolean withinOneHour, String formattedHour) {

        logger.info("Building command for searchValue: " + searchValue + " IP: " + ip);

        if (!withinOneHour) {

            logger.info("Searching archived logs for hour: " + formattedHour);

            String archivedLog = LOG_PATH + "-" + formattedHour + "*";

            return String.format(
                    "ssh %s@%s \"zgrep --text -C5 '%s' %s\"",
                    USERNAME, ip, searchValue, archivedLog);

        } else {

            logger.info("Searching current log file");

            return String.format(
                    "ssh %s@%s \"grep --text -C5 '%s' %s\"",
                    USERNAME, ip, searchValue, LOG_PATH);
        }
    }

    // Step-2
    public String findUUID(String txnId, String ip, boolean withinOneHour, String formattedHour) throws Exception {

        logger.info("Starting UUID search for transaction ID: " + txnId + " on node: " + ip);

        String cmd = buildCommand(txnId, ip, withinOneHour, formattedHour);

        logger.info("Executing command: " + cmd);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);

        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                logger.fine("Processing log line");

                if (line.contains(txnId)) {

                    Matcher matcher = UUID_PATTERN.matcher(line);

                    if (matcher.find()) {

                        String uuid = matcher.group();

                        logger.info("UUID found: " + uuid);

                        return uuid;
                    }
                }
            }
        }

        logger.warning("UUID not found for transaction ID: " + txnId);

        return null;
    }

    // Step-3
    public StringBuilder searchFullTransaction(String uuid,
                                               String ip,
                                               boolean withinOneHour,
                                               String formattedHour) throws Exception {

        logger.info("Starting full transaction search for UUID: " + uuid + " from IP: " + ip);
        int count = 0;
        StringBuilder data = new StringBuilder();
        //withinOneHour
      if(withinOneHour==true) {
    	   String cmd = buildCommand(uuid, ip, withinOneHour, formattedHour);  
              logger.info("Generated command: " + cmd);

     

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);

        pb.redirectErrorStream(true);

        logger.info("Executing command...");

        Process process = pb.start();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;

             count = 0;

            while ((line = reader.readLine()) != null) {

                data.append(line).append("\n");

                count++;
            }

            logger.info("Total log lines captured: " + count);

        } catch (Exception e) {

            logger.severe("Error while reading process output: " + e.getMessage());

            throw e;
        }

        int exitCode = process.waitFor();

        logger.info("Command execution completed with exit code: " + exitCode);
       }else  {

    	   String cmd = buildCommand(uuid, ip, withinOneHour, formattedHour);  
              logger.info("Generated command: " + cmd);

     

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);

        pb.redirectErrorStream(true);

        logger.info("Executing command...");

        Process process = pb.start();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;

             count = 0;

            while ((line = reader.readLine()) != null) {

                data.append(line).append("\n");

                count++;
            }

            logger.info("Total log lines captured: " + count);

        } catch (Exception e) {

            logger.severe("Error while reading process output: " + e.getMessage());

            throw e;
        }

        int exitCode = process.waitFor();

        logger.info("Command execution completed with exit code: " + exitCode);
       
       }

        if (data.length() == 0) {

            logger.warning("No logs found for UUID: " + uuid);
        }

        logger.info("Transaction log search completed for UUID: " + uuid);

        return data;
    }
}