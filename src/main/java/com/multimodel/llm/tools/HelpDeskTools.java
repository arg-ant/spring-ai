package com.multimodel.llm.tools;

import com.multimodel.llm.entity.HelpDeskTicket;
import com.multimodel.llm.model.TicketRequest;
import com.multimodel.llm.service.HelpDeskTicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI tools exposing help-desk ticket operations (creation and status lookup) that the
 * help-desk {@code ChatClient} can invoke on behalf of the requesting user. The requesting
 * username is expected to be supplied via the tool context (see
 * {@code HelpDeskController#helpDesk}).
 */
@Component
@RequiredArgsConstructor
public class HelpDeskTools {

    private static final Logger logger = LoggerFactory.getLogger(HelpDeskTools.class);

    private final HelpDeskTicketService service;

    /**
     * Creates a new help-desk ticket for the current user. Invoked by the model only when the
     * user explicitly requests to open a ticket; its result is returned directly to the user,
     * bypassing further model processing.
     *
     * @param ticketRequest the ticket details supplied by the model, with a mandatory issue field
     * @param toolContext the tool invocation context, expected to contain the requesting
     *                    {@code username}
     * @return a confirmation message including the new ticket's ID and username
     */
    @Tool(name = "createTicket",
            description = """
                    If user explicitly requests to create a helpdesk ticket,
                    Call this tool.
                    The issue field is mandatory.
                    """,
            returnDirect = true // return the result directly to the user, bypassing the model
    )
    String createTicket(@ToolParam(description = "Details to create a Support ticket")
                        TicketRequest ticketRequest,
                        ToolContext toolContext) {

        String username = (String) toolContext.getContext().get("username");
        logger.info("Creating support ticket for user: {} with details: {}", username, ticketRequest);
        HelpDeskTicket savedTicket = service.createTicket(ticketRequest, username);
        logger.info("Ticket created successfully. Ticket ID: {}, Username: {}", savedTicket.getId(), savedTicket.getUsername());

        return "Ticket #" + savedTicket.getId() + " created successfully for user " + savedTicket.getUsername();
    }

    /**
     * Fetches all help-desk tickets belonging to the current user.
     *
     * @param toolContext the tool invocation context, expected to contain the requesting
     *                    {@code username}
     * @return the list of tickets raised by that user, empty if none exist
     */
    @Tool(description = "Fetch the status of the tickets based on a given username")
    List<HelpDeskTicket> getTicketStatus(ToolContext toolContext) {
        String username = (String) toolContext.getContext().get("username");
        logger.info("Fetching tickets for user: {}", username);
        List<HelpDeskTicket> tickets = service.getTicketsByUsername(username);
        logger.info("Found {} tickets for user: {}", tickets.size(), username);
        // throw new RuntimeException("Unable to fetch ticket status");

        return tickets;
    }
}
