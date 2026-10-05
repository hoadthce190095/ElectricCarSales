package vn.edu.fpt.electriccarsales.dto;

import vn.edu.fpt.electriccarsales.entities.VehicleModel;

import java.time.Instant;
import java.util.List;

/**
 * Assistant response containing public catalogue data only.
 */
public class AIResponse {
    private String mode;
    private String intent;
    private boolean refused;
    private String answer;
    private AIConstraints constraints;
    private List<VehicleRecommendation> recommendations = List.of();
    private List<VehicleModel> comparison = List.of();
    private String conversationId;
    private Instant generatedAt;

    public AIResponse() {
    }

    public AIResponse(String mode, String intent, boolean refused, String answer, AIConstraints constraints,
        List<VehicleRecommendation> recommendations, List<VehicleModel> comparison, String conversationId,
        Instant generatedAt) {
        setMode(mode);
        setIntent(intent);
        setRefused(refused);
        setAnswer(answer);
        setConstraints(constraints);
        setRecommendations(recommendations);
        setComparison(comparison);
        setConversationId(conversationId);
        setGeneratedAt(generatedAt);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public boolean isRefused() {
        return refused;
    }

    public void setRefused(boolean refused) {
        this.refused = refused;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public AIConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(AIConstraints constraints) {
        this.constraints = constraints;
    }

    public List<VehicleRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<VehicleRecommendation> recommendations) {
        this.recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    public List<VehicleModel> getComparison() {
        return comparison;
    }

    public void setComparison(List<VehicleModel> comparison) {
        this.comparison = comparison == null ? List.of() : List.copyOf(comparison);
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
