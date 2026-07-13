package com.multimodel.llm.service;

import com.multimodel.llm.entity.HelpDeskTicket;
import com.multimodel.llm.model.TicketRequest;
import com.multimodel.llm.repository.HelpDeskTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.multimodel.llm.config.Constants.TICKET_STATUS_OPEN;

/**
 * Service layer for creating and querying {@link HelpDeskTicket}s, typically invoked as a
 * tool by the help-desk chat client.
 */
@Service
@RequiredArgsConstructor
public class HelpDeskTicketService {

    private final HelpDeskTicketRepository helpDeskTicketRepository;

    /**
     * Creates and persists a new help-desk ticket for the given user, defaulting its status
     * to {@code OPEN}, its creation time to now, and its ETA to 7 days from now.
     *
     * @param ticketInput the incoming ticket request containing the issue description
     * @param username the user raising the ticket
     * @return the persisted ticket, including its generated ID
     */
    public HelpDeskTicket createTicket(TicketRequest ticketInput, String username) {
        HelpDeskTicket ticket = HelpDeskTicket.builder()
                .issue(ticketInput.issue())
                .username(username)
                .status(TICKET_STATUS_OPEN)
                .createdAt(LocalDateTime.now())
                .eta(LocalDateTime.now().plusDays(7))
                .build();
        return helpDeskTicketRepository.save(ticket);
    }

    /**
     * Retrieves all help-desk tickets raised by the given user.
     *
     * @param username the username to search for
     * @return the list of tickets raised by that user, empty if none exist
     */
    public List<HelpDeskTicket> getTicketsByUsername(String username) {
        return helpDeskTicketRepository.findByUsername(username);
    }
}
