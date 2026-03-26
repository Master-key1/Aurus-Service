package com.auruspay.decryptor;import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class AurusDecryptor {

    private static final String HEX_KEY = "A309BB49B764D95BD17666F0709C2881";
	private static String decrypted;

    public static String decryptor(String encryptedInput) {
        // Example: If this string has a space at the end, it will now still work
      //  String encryptedInput = " PASTE_YOUR_DATA_HERE "; 

        try {
            // 1. CLEAN THE INPUT (Fixes the 'character 20' error)
            String cleanInput = encryptedInput.replaceAll("\\s", "");
            
             decrypted = decrypt(cleanInput, HEX_KEY);
         
        } catch (Exception e) {	
         
            e.printStackTrace();
        }
        return decrypted.replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&").replace("&#37;", "%").replace("&apos;", "'").replace("&quot;", "'");
    }

    public static String decrypt(String base64Content, String hexKey) throws Exception {
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < 32; i += 2) {
            keyBytes[i / 2] = (byte) ((Character.digit(hexKey.charAt(i), 16) << 4)
                             + Character.digit(hexKey.charAt(i + 1), 16));
        }

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // This line used to fail because of the spaces; now it receives 'cleanInput'
        byte[] decodedBuffer = Base64.getDecoder().decode(base64Content);
        byte[] decryptedBuffer = cipher.doFinal(decodedBuffer);

        return new String(decryptedBuffer, StandardCharsets.UTF_8);
    }
}