package com.auruspay.rest;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auruspay.driver.Main;
import com.auruspay.model.LogServiceDetails;

@RestController
@RequestMapping("/aurus")
public class AurusServiceController {
	
	@Autowired
	private Main main ;
	@Autowired
	private Environment env  ;
	
	
	   private static final Logger logger = Logger.getLogger(AurusServiceController.class.getName());

	    @GetMapping("/log/{txnID}/{uniqueID}/{auruspayDate}")
	    public ResponseEntity<String> getLogDetails(
	            @PathVariable String txnID,
	            @PathVariable String uniqueID,
	            @PathVariable String auruspayDate) {

	        logger.info("Received request for log search");
	        logger.info("txnID: " + txnID + ", uniqueID: " + uniqueID + ", auruspayDate: " + auruspayDate);

	        try {

	            logger.info("Invoking log search service");

	            StringBuilder logBuffer = main.invokeLogDetails(txnID, uniqueID, auruspayDate);

	            logger.info("Log search completed successfully.\n"+logBuffer);

	            return new ResponseEntity<>(logBuffer.toString(), HttpStatus.OK);

	        } catch (Exception e) {

	            logger.severe("Error while fetching logs: " + e.getMessage());

	            return new ResponseEntity<>("Error fetching logs", HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
	
	@GetMapping("/logger")
	//public ResponseEntity<LogServiceDetails> getLogDetails(@RequestBody LogServiceDetails details ){
	public ResponseEntity<String> getLogDetails(){
		
		/*
		LogServiceDetails details = null;
		String txnID = details.getTxnID();
		String uniqueID = details.getUniqueID();
		String auruspayDate = details.getAuruspayDate();
		*/
		String details = "logger : (port : "; 
		String port = env.getProperty("server.port");
		details+=port +")";
		return new ResponseEntity<String>(details ,HttpStatus.OK);
		
	}
	

    @GetMapping("/test")
    public String test() {
    	
    	String port = env.getProperty("server.port");
    	
        return "Aurus Service Running ......... with ( Port : "+port+" )";
    }
	

}
