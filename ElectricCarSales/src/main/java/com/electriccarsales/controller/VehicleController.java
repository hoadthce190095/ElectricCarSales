package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.CarImage;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@WebServlet(name = "VehicleController", urlPatterns = {"/home", "/cars"})
public class VehicleController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        VehicleCatalogService service = new VehicleCatalogService();

        // 1. Lấy danh sách mẫu xe từ CSDL
        List<CarModel> carList = service.getAllCarModels();

        // 2. Lấy hình ảnh tương ứng cho từng xe để trang chủ hiển thị đúng ảnh
        Map<Integer, String> carImagesMap = new HashMap<>();
        for (CarModel car : carList) {
            List<CarImage> images = service.getImagesByModelId(car.getModelID());
            if (images != null && !images.isEmpty()) {
                carImagesMap.put(car.getModelID(), images.get(0).getImageURL());
            } else {
                carImagesMap.put(car.getModelID(), "assets/images/default-car.png");
            }
        }

        // 3. Đẩy dữ liệu sang request attribute
        request.setAttribute("carList", carList);
        request.setAttribute("carImagesMap", carImagesMap);

        // 4. Forward trực tiếp về trang chủ index.jsp của nhóm
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}