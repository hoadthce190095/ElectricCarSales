package com.electriccarsales.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import com.electriccarsales.service.AIService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AIContractTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void javaBeansKeepExistingAssistantJsonFieldNamesAndDeserializeRequests() throws Exception {
        AIRequest input = mapper.readValue("""
            {"message":"Tư vấn xe", "budget":500000000, "minRange":300,
             "minSeats":5, "modelIds":[1], "intent":"RECOMMEND"}
            """, AIRequest.class);
        assertEquals("Tư vấn xe", input.getMessage());
        assertEquals(new BigDecimal("500000000"), input.getBudget());
        assertEquals(List.of(1), input.getModelIds());

        VehicleSpecifications specifications = new VehicleSpecifications();
        specifications.setRangeKM(new BigDecimal("350"));
        specifications.setSeatingCapacity(5);
        VehicleModel vehicle = new VehicleModel(1, "VF 5", "SUV", "VinFast", null, new BigDecimal("450000000"),
            new BigDecimal("450000000"), 1, List.of("Standard"), List.of("Trắng"), specifications);
        AIConstraints constraints = new AIConstraints(input.getBudget(), input.getMinRange(), input.getMinSeats(), null,
            null, null, List.of(), List.of());
        AIResponse response = new AIResponse(AIService.MODE, "RECOMMEND", false, "Gợi ý xe", constraints,
            List.of(new VehicleRecommendation(vehicle, List.of("Đáp ứng ngân sách"))), List.of(), "product-session",
            Instant.parse("2026-10-05T00:00:00Z"));

        JsonNode json = mapper.valueToTree(response);
        assertEquals(Set.of("mode", "intent", "refused", "answer", "constraints", "recommendations", "comparison",
            "conversationId", "generatedAt"), fieldNames(json));
        assertEquals(Set.of("budget", "minRange", "minSeats", "usagePurpose", "preferredBrand", "dailyDistanceKm",
            "notes", "inferredFields"), fieldNames(json.get("constraints")));
        assertFalse(json.get("refused").asBoolean());
        JsonNode modelJson = json.get("recommendations").get(0).get("vehicle");
        assertEquals(1, modelJson.get("modelId").asInt());
        assertEquals(350, modelJson.get("specifications").get("rangeKM").asInt());
        assertEquals(Set.of("mode", "models"),
            fieldNames(mapper.valueToTree(new CatalogueResponse(AIService.MODE, List.of(vehicle)))));
        assertEquals("VF 5",
            mapper.treeToValue(json, AIResponse.class).getRecommendations().getFirst().getVehicle().getModelName());
    }

    @Test
    void mutableBeanSettersCopyCollectionsWithoutBypassingRequestValidation() throws Exception {
        List<Integer> ids = new ArrayList<>(List.of(1));
        AIRequest input = new AIRequest("So sánh", null, null, null, ids, "COMPARE");
        ids.add(2);
        assertEquals(List.of(1), input.getModelIds());
        assertThrows(UnsupportedOperationException.class, () -> input.getModelIds().add(3));

        AIRequest invalid = mapper.readValue("""
            {"message":"So sánh", "modelIds":[1,null], "intent":"COMPARE"}
            """, AIRequest.class);
        AIService service = new AIService(() -> {
            throw new AssertionError("Invalid model ids must not reach the catalogue");
        });
        assertEquals(400, assertThrows(ApiException.class, () -> service.respond(invalid)).getStatus());

        VehicleModel model = new VehicleModel();
        List<String> colors = new ArrayList<>(List.of("Trắng"));
        model.setColors(colors);
        colors.clear();
        assertEquals(List.of("Trắng"), model.getColors());
        assertThrows(UnsupportedOperationException.class, () -> model.getColors().add("Đen"));
        assertNotNull(model.getSpecifications());
        model.setSpecifications(null);
        assertNull(model.getSpecifications().getRangeKM());
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
