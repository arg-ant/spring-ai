package com.multimodel.llm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;


/**
 * Spring AI tools exposing time-related functions that a {@code ChatClient} can invoke to
 * answer questions about the current time.
 */
@Component
public class TimeTools {

    private static final Logger logger = LoggerFactory.getLogger(TimeTools.class);

    /**
     * Returns the current time in the server's default time zone.
     *
     * @return a message containing the current local time
     */
    @Tool(name = "getCurrentLocalTime", description = "Get the current time in the user's timezone")
    public String getCurrentLocalTime() {
        logger.info("Getting current local time");
        return "Current local time is " + LocalTime.now();
    }

    /**
     * Returns the current time in the given time zone.
     *
     * @param timeZone the time zone ID to compute the current time in (e.g. {@code "America/New_York"})
     * @return the current time in that time zone
     * @throws java.time.DateTimeException if the time zone ID is invalid
     */
    @Tool(name = "getCurrentTime", description = "Get the current time in the specified time zone")
    public String getCurrentTime(@ToolParam(description = "Value representing the time zone") String timeZone) {
        logger.info("Getting current time in the time zone {}", timeZone);
        return LocalTime.now(ZoneId.of(timeZone)).toString();
    }
}
