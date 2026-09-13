<%-- 
    Document   : order
    Created on : Sep 11, 2026, 10:40:35 PM
    Author     : Hoa Hà
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Order Car</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
              rel="stylesheet">

        <style>
            .page-title {
                text-align: center;
                font-size: 46px;
                font-weight: 800;
                letter-spacing: 4px;
                margin-top: 45px;
                margin-bottom: 40px;
            }
        </style>


    <body>
        <div class="container">
            <h1 class="page-title">Order Cars</h1>

            <h2>Car Information</h2>

            <table class="table table-bordered">
                <tr>
                    <td>CarID</td>
                    <td>${car.carID}</td>
                </tr>

                <tr>
                    <td>ModelID</td>
                    <td>${car.modelID}</td>
                </tr>

                <tr>
                    <td>VIN</td>
                    <td>${car.vin}</td>
                </tr>

                <tr>
                    <td>Color</td>
                    <td>${car.color}</td>
                </tr>

                <tr>
                    <td>Manufacturing Year</td>
                    <td>${car.manufacturingYear}</td>
                </tr>

                <tr>
                    <td>Status</td>
                    <td>${car.carStatus}</td>
                </tr>             
            </table>

            <h2 class="mt-5">
                Customer Information
            </h2>

            <c:if test="${not empty error}">

                <div class="alert alert-danger">
                    ${error}
                </div>
            </c:if>
                
                <form action="${pageContext.request.contextPath}/cars"
                      method="post">

                    <input type="hidden" name="action" value="order">

                    <input type="hidden" name="carID" value="${car.carID}">


                    <div class="mb-3">
                        <label class="form-label">
                            Customer Name
                        </label>

                        <input type="text" name="customerName" class="form-control" required>
                    </div>


                    <div class="mb-3">
                        <label class="form-label">
                            Phone
                        </label>

                        <input type="text" name="phone" class="form-control" required>
                    </div>


                    <div class="mb-3">
                        <label class="form-label">
                            Email
                        </label>
                        <input type="email" name="email" class="form-control">
                    </div>


                    <div class="mb-3">
                        <label class="form-label">
                            Address
                        </label>

                        <input type="text" name="address" class="form-control" required>
                    </div>


                    <button type="submit" class="btn btn-success">
                        Confirm Order
                    </button>


                    <a href="${pageContext.request.contextPath}/cars?action=detail&id=${car.modelID}"
                       class="btn btn-secondary">
                        Back
                    </a>

                </form>
            </div>
        </body>
    </html>
