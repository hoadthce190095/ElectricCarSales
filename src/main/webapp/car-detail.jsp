<%-- 
    Document   : car-detail
    Created on : Sep 11, 2026, 9:17:01 PM
    Author     : Hoa Hà
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Car Detail</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">

        <style>

            .page-title {
                text-align: center;
                font-size: 46px;
                font-weight: 800;
                letter-spacing: 4px;
                margin-top: 45px;
                margin-bottom: 10px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h1 class="page-title">
                Car Detail
            </h1>

            <table class="table table-bordered border-primary"> 
                <tr>
                    <td>ModelID</td>
                    <td>
                        ${carModel.modelID}
                    </td>
                </tr>


                <tr>
                    <td>BrandID</td>
                    <td>
                        ${carModel.brandID}
                    </td>
                </tr>


                <tr>
                    <td>ModelName</td>
                    <td>
                        ${carModel.modelName}
                    </td>
                </tr>


                <tr>
                    <td>BatteryCapacity</td>
                    <td>
                        ${carModel.batteryCapacity} kWh
                    </td>
                </tr>


                <tr>
                    <td>RangeKM</td>
                    <td>
                        ${carModel.rangeKM} km
                    </td>
                </tr>


                <tr>
                    <td>Price</td>
                    <td>
                        ${carModel.price}
                    </td>
                </tr>


                <tr>
                    <td>Description</td>
                    <td>
                        ${carModel.description}
                    </td>
                </tr>
            </table>

            <h2 class="mt-5"> Available Cars</h2>

            <table class="table table-hover">
                <tr>
                    <td>CarID</td>
                    <td>ModelID</td>
                    <td>VIN</td>
                    <td>Color</td>
                    <td>ManufacturingYear</td>
                    <td>CarStatus</td>
                    <td>Action</td>
                </tr>

                <c:forEach var="car" items="${carList}">
                    <tr>
                        <td>${car.carID}</td>
                        <td>${car.modelID}</td>
                        <td>${car.vin}</td>
                        <td>${car.color}</td>
                        <td>${car.manufacturingYear}</td>
                        <td>${car.carStatus}</td>

                        <td>
                            <c:if test="${car.carStatus == 'AVAILABLE'}">

                                <a href="${pageContext.request.contextPath}/cars?action=order&id=${car.carID}"
                                   class="btn btn-success">
                                    Order Now
                                </a>

                            </c:if>
                        </td>
                        
                    </tr>
                </c:forEach>
            </table>
            <a href="${pageContext.request.contextPath}/cars"
               class="btn btn-secondary">

                Back

            </a>
        </div>
    </body>
</html>
