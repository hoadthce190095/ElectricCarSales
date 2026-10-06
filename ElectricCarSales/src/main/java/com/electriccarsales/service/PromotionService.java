package com.electriccarsales.service;

import com.electriccarsales.DAO.CarModelDAO;
import com.electriccarsales.DAO.PromotionDAO;
import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.Promotion;

import java.util.List;

public class PromotionService {
    private PromotionDAO promotionDAO;

    public PromotionService() {
        promotionDAO = new PromotionDAO();
    }

    public List<Promotion> getAllPromotions(){
        return promotionDAO.getAll();
    }

}
