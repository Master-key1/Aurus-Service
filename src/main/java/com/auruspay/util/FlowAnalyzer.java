package com.auruspay.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class FlowAnalyzer {

    // 🔥 Expected Flow
    private static final List<String> expectedFlow = Arrays.asList(
            "class :ProcessAurusTransaction Method :validateAurusRestRequest",
            "class :ContextMapper Method :createContext",
            "class :AurusISOMux Method :sendMsg"
    );

   
    // ✅ READ FROM CLASSPATH FILE (SAFE)
    private static void readClasspathFile() {

        Resource resource = new ClassPathResource("flow_tracking.txt");

        if (!resource.exists()) {
            System.out.println("⚠️ Classpath file not found");
            return;
        }

        System.out.println("📂 Reading Classpath File...\n");

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (Exception e) {
            System.out.println("❌ Error reading classpath file");
            e.printStackTrace();
        }
    }

    // 🚀 MAIN METHOD
    public static void analyzeAndTrack(String fullLogs, String filePath) {

        Set<String> trackingSet = new LinkedHashSet<>();
        Map<String, Integer> frequencyMap = new LinkedHashMap<>();
        List<String> currentFlow = new ArrayList<>();

        File file = new File(filePath);

        try {
            // ===============================
            // ✅ STEP 1: LOAD EXISTING FILE (DESKTOP)
            // ===============================
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("class :")) {
                            trackingSet.add(line.trim());
                        }
                    }
                }
                System.out.println("📂 Loaded existing entries: " + trackingSet.size());
            }

            // ===============================
            // ✅ STEP 2: PROCESS LOGS
            // ===============================
            String[] lines = fullLogs.split("\\r?\\n");

            boolean isNewData = false;

            for (String line : lines) {

                String extracted = extract(line);

                if (extracted != null) {

                    currentFlow.add(extracted);

                    // ✅ UNIQUE TRACKING
                    if (trackingSet.add(extracted)) {
                        System.out.println("🆕 New Step: " + extracted);
                        isNewData = true;
                    }

                    // ✅ FREQUENCY
                    frequencyMap.put(extracted,
                            frequencyMap.getOrDefault(extracted, 0) + 1);
                }
            }

            // ===============================
            // 🔍 STEP 3: MISSING STEP
            // ===============================
            detectMissingSteps(currentFlow);

            // ===============================
            // 🤖 STEP 4: ANOMALY
            // ===============================
            detectAnomaly(currentFlow);

            // ===============================
            // 📁 STEP 5: WRITE FILE
            // ===============================
            if (isNewData || !file.exists()) {
                writeToFile(filePath, trackingSet, frequencyMap);
            } else {
                System.out.println("ℹ️ No new updates");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔥 EXTRACT
    private static String extract(String line) {

        try {
            int start = line.indexOf("[");
            int end = line.indexOf("]");

            if (start == -1 || end == -1 || end <= start) return null;

            String content = line.substring(start + 1, end);

            if (!content.contains(".") || !content.contains("(")) return null;

            String className = content.substring(0, content.indexOf("."));
            String methodName = content.substring(
                    content.indexOf(".") + 1,
                    content.indexOf("(")
            );

            return "class :" + className + " Method :" + methodName;

        } catch (Exception e) {
            return null;
        }
    }

    // 🔍 MISSING STEP
    private static void detectMissingSteps(List<String> currentFlow) {

        System.out.println("\n🔍 Checking Missing Steps...");

        for (String expected : expectedFlow) {
            if (!currentFlow.contains(expected)) {
                System.out.println("⚠️ Missing Step: " + expected);
            }
        }
    }

    // 🤖 ANOMALY
    private static void detectAnomaly(List<String> flow) {

        System.out.println("\n🤖 Checking Anomalies...");

        for (int i = 0; i < flow.size() - 1; i++) {

            String current = flow.get(i);
            String next = flow.get(i + 1);

            if (current.contains("validateAurusRestRequest")
                    && !next.contains("createContext")) {

                System.out.println("🚨 Anomaly: createContext skipped");
            }

            if (current.contains("createContext")
                    && !next.contains("sendMsg")) {

                System.out.println("🚨 Anomaly: sendMsg skipped");
            }
        }
    }

    // 📁 WRITE FILE
    private static void writeToFile(String filePath,
                                    Set<String> flowSet,
                                    Map<String, Integer> freqMap) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            writer.write("========== FLOW ANALYSIS ==========\n\n");

            writer.write("📌 Unique Flow:\n");
            for (String s : flowSet) {
                writer.write(s + "\n");
            }

            writer.write("\n📊 Frequency:\n");
            for (Map.Entry<String, Integer> e : freqMap.entrySet()) {
                writer.write(e.getKey() + " -> " + e.getValue() + "\n");
            }

            writer.write("\n🕒 Generated At: " + LocalDateTime.now());

            System.out.println("✅ File Updated: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}