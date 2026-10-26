package com.electriccarsales.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.electriccarsales.model.AIRequest;
import com.electriccarsales.model.AIResponse;
import com.electriccarsales.model.CatalogueResponse;
import com.electriccarsales.model.ConversationContext;
import com.electriccarsales.model.VehicleRecommendation;
import com.electriccarsales.model.VehicleModel;
import com.electriccarsales.service.AIService;
import com.electriccarsales.model.ApiException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Nhận câu hỏi tư vấn và lưu ngữ cảnh sản phẩm trong phiên hiện tại.
 */
@WebServlet("/api/ai")
public final class AIController extends JsonServlet {
    private static final String CONTEXT = AIController.class.getName() + ".context";
    private final AIService aiService;

    public AIController() {
        this(new AIService());
    }

    public AIController(AIService aiService) {
        this.aiService = Objects.requireNonNull(aiService);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeJson(response, new CatalogueResponse(AIService.MODE, aiService.models()));
        } catch (SQLException exception) {
            throw new ApiException(503, "Danh mục xe tạm thời không khả dụng. Vui lòng thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        if (action != null && !action.isBlank() && !"reset".equals(action)) {
            throw new ApiException(400, "Thao tác tư vấn không hợp lệ.");
        }
        HttpSession session = request.getSession();
        if ("reset".equals(action)) {
            // Đổi cuộc trò chuyện chỉ xóa tiêu chí xe, không xóa phiên đăng nhập.
            readJson(request, Map.class);
            synchronized (session) {
                session.removeAttribute(CONTEXT);
                ConversationContext freshContext = getConversationContext(session);
                writeJson(response, Map.of("cleared", true, "conversationId", freshContext.getId(), "message",
                    "Đã bắt đầu cuộc trò chuyện mới."));
            }
            return;
        }
        AIRequest aiRequest = readJson(request, AIRequest.class);
        ConversationContext context = getConversationContext(session);
        try {
            AIResponse aiResponse = aiService.respond(aiRequest, context.getId(), context.getConstraints(),
                context.getModelIds());

            // Không lưu yêu cầu bị từ chối vào ngữ cảnh tư vấn xe.
            if (!aiResponse.isRefused()) {
                saveConversationContext(session, context.getId(), aiResponse);
            }
            writeJson(response, aiResponse);
        } catch (SQLException exception) {
            throw new ApiException(503, "Trợ lý chưa đọc được danh mục xe. Vui lòng thử lại sau.");
        }
    }

    private ConversationContext getConversationContext(HttpSession session) {
        synchronized (session) {
            ConversationContext context = (ConversationContext) session.getAttribute(CONTEXT);
            if (context == null) {
                String conversationId = UUID.randomUUID().toString();
                context = new ConversationContext(conversationId, null, List.of());
                session.setAttribute(CONTEXT, context);
            }
            return context;
        }
    }

    private void saveConversationContext(HttpSession session, String conversationId, AIResponse aiResponse) {
        List<Integer> modelIds = new ArrayList<>();
        if (!aiResponse.getComparison().isEmpty()) {
            for (VehicleModel model : aiResponse.getComparison()) {
                modelIds.add(model.getModelId());
            }
        } else {
            for (VehicleRecommendation recommendation : aiResponse.getRecommendations()) {
                modelIds.add(recommendation.getVehicle().getModelId());
            }
        }

        ConversationContext newContext = new ConversationContext(conversationId, aiResponse.getConstraints(), modelIds);
        synchronized (session) {
            try {
                ConversationContext current = (ConversationContext) session.getAttribute(CONTEXT);
                // Một tab khác có thể đã bắt đầu cuộc trò chuyện mới trong lúc đọc danh mục.
                if (current != null && Objects.equals(current.getId(), conversationId)) {
                    session.setAttribute(CONTEXT, newContext);
                }
            } catch (IllegalStateException ignored) {
                // Đăng xuất hoặc đổi mật khẩu có thể kết thúc phiên khi câu trả lời đang xử lý.
            }
        }
    }
}
