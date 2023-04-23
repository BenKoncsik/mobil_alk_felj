package hu.koncsik.extensions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;


public class GenerateId {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RANDOM_BOUND = 100000;

    private static String generateUniqueId() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FORMATTER);
        Random random = new Random();
        int randomNumber = random.nextInt(RANDOM_BOUND);
        String uniqueId = String.format("%s%04d", timestamp, randomNumber);

        if (uniqueId.length() > 16) {
            uniqueId = uniqueId.substring(0, 16);
        }

        return uniqueId;
    }

    public static String newId(){
        return generateUniqueId();
    }

}
