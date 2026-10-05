package vn.edu.fpt.electriccarsales.dto;

import java.util.List;

/**
 * Session-local product context; it contains no account or financial data.
 */
public class ConversationContext {
    private String id;
    private AIConstraints constraints;
    private List<Integer> modelIds = List.of();

    public ConversationContext() {
    }

    public ConversationContext(String id, AIConstraints constraints, List<Integer> modelIds) {
        setId(id);
        setConstraints(constraints);
        setModelIds(modelIds);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AIConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(AIConstraints constraints) {
        this.constraints = constraints;
    }

    public List<Integer> getModelIds() {
        return modelIds;
    }

    public void setModelIds(List<Integer> modelIds) {
        this.modelIds = modelIds == null ? List.of() : List.copyOf(modelIds);
    }
}
