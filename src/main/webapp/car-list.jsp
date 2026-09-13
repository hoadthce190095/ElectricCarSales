<%-- 
    Document   : carlist
    Created on : Sep 10, 2026, 4:24:20 PM
    Author     : Hoa Hà
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Electric Cars</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
    </head>
    <body>

        <div class="container">
            <h1 class="page-title">Electric Cars</h1>
            <div class="page-title-line"></div>
            <style>
                .page-title {
                    text-align: center;
                    font-size: 48px;
                    font-weight: 700;
                    margin-top: 40px;
                    margin-bottom: 40px;
                    letter-spacing: 3px;
                }

            </style>
            <table class="table table-bordered border-primary"> 
                <tr>
                    <td>ModelID</td>
                    <td>BrandID</td>
                    <td>ModelName</td>
                    <td>BatteryCapacity</td>
                    <td>RangeKM</td>
                    <td>Price</td>
                    <td>Description</td>
                </tr>
                <c:forEach var="car" items="${carList}">

                    <tr>
                        <td>${car.modelID}</td>
                        <td>${car.brandID}</td>
                        <td>${car.modelName}</td>
                        <td>${car.batteryCapacity}</td>
                        <td>${car.rangeKM}</td>
                        <td>${car.price}</td>
                        <td>${car.description}</td>

                        <td> 
                            <a href="${pageContext.request.contextPath}/cars?action=detail&id=${car.modelID}"
                               class="btn btn-primary">

                                Detail

                            </a>
                        </td>
                    </tr>

            </div>
        </c:forEach>
    </table>
</div>
</body>
</html>
