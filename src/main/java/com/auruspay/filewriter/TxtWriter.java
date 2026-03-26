package com.auruspay.filewriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TxtWriter {

    private String filePath;

    public TxtWriter(String filePath) {
        this.filePath = filePath;
    }

    // ================= MAP + LOG (APPEND MODE) =================
public void writeMapWithLogs(Map<String, Object> map, String logs) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {

            writer.write("\n=============================================================================\n");
            writer.write("\n\nTRANSACTION DATA\n");
            writer.write("===============================================================================\n");

            // ===== MAP DATA =====
            for (Map.Entry<String, Object> entry : map.entrySet()) {

                Object value = entry.getValue();

                if (value instanceof List) {

                    writer.write(entry.getKey() + " :\n");

                    for (Object item : (List<?>) value) {
                        writer.write("   - " + item + "\n");
                    }

                } else {

                    writer.write(entry.getKey() + " : " + value + "\n");
                }

                // ✅ Separator after EACH MAP ENTRY
                writer.write("----------------------------------------------------------------------------------------------------------\n");
            }

            // ===== RAW LOGS (NO SEPARATOR HERE) =====
            writer.write("\n======================================= RAW LOGS =================================================================\n");
            writer.write(logs);
            writer.write("\n==================================================================================================================\n");

            writer.flush();
        }

        System.out.println("✅ TXT file updated correctly: " + filePath);
    }
}