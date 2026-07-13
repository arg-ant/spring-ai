package com.multimodel.llm.repository;

import com.multimodel.llm.entity.HelpDeskTicket;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * Spring Data JPA repository for {@link HelpDeskTicket} entities.
 */
public interface HelpDeskTicketRepository extends JpaRepository<@NonNull HelpDeskTicket, @NonNull Long> {

    /**
     * Finds all help-desk tickets raised by the given user.
     *
     * @param username the username to search for
     * @return the list of tickets raised by that user, empty if none exist
     */
    List<HelpDeskTicket> findByUsername(String username);

}
