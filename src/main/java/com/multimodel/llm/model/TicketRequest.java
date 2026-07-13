package com.multimodel.llm.model;

/**
 * Request payload for raising a help-desk ticket.
 *
 * @param issue description of the issue being reported
 */
public record TicketRequest(String issue) {
}
