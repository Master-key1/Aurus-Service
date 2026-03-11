package com.auruspay.util;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

    public static TimeResult processTime(String inputDateTime) {

        LocalDateTime inputTime =
                LocalDateTime.parse(inputDateTime, INPUT_FORMAT);

        boolean withinOneHour =
                inputTime.isAfter(LocalDateTime.now().minusHours(1));

        String formatted =
                inputTime.format(OUTPUT_FORMAT);

        return new TimeResult(withinOneHour, formatted);
    }
}