package app.demo.services;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

@Slf4j
public class HashService {

    // src: https://medium.com/@AlexanderObregon/what-is-sha-256-hashing-in-java-0d46dfb83888
    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = md.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();


        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return null;
        } catch (Exception e) {
            log.error("Error while hashing text", e);
            return null;
        }
    }

    // src: https://github.com/sing1ee/simhash-java
    public static long simHash(String text) {
        int[] bitVector = new int[64];
        StringTokenizer tokenizer = new StringTokenizer(text);

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            long hash = murmurHash64(token);

            for (int i = 0; i < 64; i++) {
                if (((hash >>> i) & 1) == 1) {
                    bitVector[i] += 1;
                } else {
                    bitVector[i] -= 1;
                }
            }
        }

        long simHash = 0L;
        for (int i = 0; i < 64; i++) {
            if (bitVector[i] > 0) {
                simHash |= (1L << i);
            }
        }
        return simHash;
    }

    private static long murmurHash64(String key) {
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        long hash = 1125899906842597L; // a large prime
        for (byte b : data) {
            hash = 31 * hash + b;
        }
        return hash;
    }

}
