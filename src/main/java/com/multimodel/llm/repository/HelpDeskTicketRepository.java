package com.multimodel.llm.repository;

import com.multimodel.llm.entity.HelpDeskTicket;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface HelpDeskTicketRepository extends JpaRepository<@NonNull HelpDeskTicket, @NonNull Long> {

    List<HelpDeskTicket> findByUsername(String username);

}
