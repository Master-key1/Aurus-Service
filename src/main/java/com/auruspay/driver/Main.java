package com.auruspay.driver;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.auruspay.service.LogFinderService;
import com.auruspay.util.TimeResult;
import com.auruspay.util.TimeUtil;

@Service
public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public StringBuilder invokeLogDetails(String txnId, String uniqueID, String auruspayDate) {

        StringBuilder logMessage = new StringBuilder();

        logMessage.append("Log not found for \n {")
                .append("\n\"txnId\" : \"").append(txnId).append("\" ,")
                .append("\n\"uniqueID\" : \"").append(uniqueID).append("\",")
                .append("\n\"auruspayDate\" : \"").append(auruspayDate).append("\"")
                .append("\n}");
        

        boolean withinOneHour = false;
        String formattedHour = null;

        try {

            logger.info("Starting log search");

            if (auruspayDate != null && !auruspayDate.isEmpty()) {

                TimeResult result = TimeUtil.processTime(auruspayDate);

                withinOneHour = result.isWithinOneHour();
                formattedHour = result.getFormattedHour();
            }

            LogFinderService service = new LogFinderService();

            String ip = service.resolveNodeIP(txnId);

            logger.info("withinOneHour : " + withinOneHour);
            logger.info("formattedHour : " + formattedHour);
            logger.info("resolveNodeIP : " + ip);

            if (ip == null) {

                logger.severe("Node not found");

                StringBuilder message = new StringBuilder();
                message.append("Node not found. Please enter the correct node");

                return message;
            }

            if (uniqueID == null || uniqueID.isEmpty()) {

                logger.info("Searching UUID using transaction ID");

                uniqueID = service.findUUID(
                        txnId,
                        ip,
                        withinOneHour,
                        formattedHour);
            }

            if (uniqueID != null) {

                logger.info("UUID found: " + uniqueID);

                logMessage = service.searchFullTransaction(
                        uniqueID,
                        ip,
                        withinOneHour,
                        formattedHour);

            } else {

                logger.warning("UUID not found");
            }

        } catch (Exception e) {

            logger.severe("Execution failed: " + e.getMessage());
        }

        return logMessage;
    }
}