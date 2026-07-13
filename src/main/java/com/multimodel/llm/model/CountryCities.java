package com.multimodel.llm.model;

import java.util.List;

/**
 * Structured output representing a country and a list of its cities, used as a target type
 * for converting model responses into typed beans.
 *
 * @param country the country name
 * @param cities the list of city names in that country
 */
public record CountryCities(String country, List<String> cities) {
}
