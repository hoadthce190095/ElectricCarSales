<%-- 
    Document   : order-success
    Created on : Sep 12, 2026, 11:36:24 AM
    Author     : Hoa Hà
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Order Success</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
              rel="stylesheet">

        <style>

            .success-box {
                max-width: 650px;
                margin: 80px auto;
                padding: 40px;
                text-align: center;
                border: 1px solid #ddd;
                border-radius: 10px;
                box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
            }

            .success-title {
                font-size: 40px;
                font-weight: 700;
                margin-bottom: 30px;
            }

            .order-info {
                text-align: left;
                margin-top: 30px;
            }

        </style>
    </head>
    <body>
        <div class="container">
            <div class="success-box">

                <h1 class="success-title text-success">
                    Order Successful
                </h1>

                <p class="lead">
                    Your order has been created successful
                </p>

                <div class="order-info">
                    <table class="table table-bordered">
                        <tr>
                            <td>
                               Order ID
                            </td>
                            <td>${orderID}</td>

                        </tr>


                        <tr>
                            <td>
                                Customer Name
                            </td>
                            <td>${customerName}</td>
                        </tr>

                        <tr>
                            <td>Car ID<</td>
                            <td>${carID}</td>
                        </tr>

                        <tr>
                            <td>Order Status</td>
                            <td>
                                <span class="badge bg-warning text-dark">
                                    PENDING
                                </span>
                            </td>
                        </tr>
                    </table>
                </div>

                <div class="mt-4">
                    <a href="${pageContext.request.contextPath}/cars"
                       class="btn btn-primary">
                        Back to Cars
                    </a>
                </div>
                       
            </div>
        </div>
    </body>
</html>
