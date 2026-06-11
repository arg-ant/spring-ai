package com.multimodel.llm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;


@Component
public class TimeTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeTools.class);

    @Tool(name = "getCurrentLocalTime", description = "Get the current time in the user's timezone")
    public String getCurrentLocalTime() {
        LOGGER.info("Getting current local time");
        return "Current local time is " + LocalTime.now();
    }

    @Tool(name = "getCurrentTime", description = "Get the current time in the specified time zone")
    public String getCurrentTime(@ToolParam
             (description = "Value representing the time zone") String timeZone) {
        LOGGER.info("Getting current time in the time zone {}", timeZone);
        return LocalTime.now(ZoneId.of(timeZone)).toString();
    }
}
