package com.electriccarsales.service;

import com.electriccarsales.dao.PromotionDAO;
import com.electriccarsales.model.Promotion;
import com.electriccarsales.model.AdminPromotion;
import util.DBContext;

import java.sql.SQLException;
import java.util.List;

public class PromotionService {
    private PromotionDAO promotionDAO;

    public PromotionService() {
        promotionDAO = new PromotionDAO();
    }

    public static List<AdminPromotion> activeForModel(int modelId) throws SQLException {
        try (var connection = DBContext.openConnection()) {
            return PromotionDAO.findActiveForModel(connection, modelId);
        }
    }

    public List<Promotion> getAllPromotions(){
        return promotionDAO.getAll();
    }

}
