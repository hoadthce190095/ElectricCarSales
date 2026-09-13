<%-- 
    Document   : order-detail
    Created on : Sep 12, 2026, 2:42:38 PM
    Author     : Hoa Hà
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Order List</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
              rel="stylesheet">

    </head>
    <body>
        <div class="container mt-5">
            <h2 class="mb-4">Order Management</h2>
            <table class="table table-bordered table-hover">

                <thead class="table-dark">
                    <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Phone</th>
                        <th>Order Date</th>
                        <th>Status</th>
                        <th>Total Amount</th>
                        <th>Action</th>
                    </tr>
                </thead>

                <tbody>
                    <c:forEach var="order" items="${orderList}">
                        <tr>
                            <td>${order.orderID}</td>
                            <td>${order.customerName}</td>
                            <td>${order.phone}</td>                           
                            <td>${order.orderDate}</td>

                            <td>
                                <span class="badge bg-warning text-dark">
                                    ${order.orderStatus}
                                </span>
                            </td>

                            <td>${order.totalAmount}</td>

                            <td>
                                <a href="${pageContext.request.contextPath}/order?action=detail&id=${order.orderID}"
                                   class="btn btn-primary btn-sm">
                                    View Detail
                                </a>
                            </td>


                        </tr>
                    </c:forEach>

                    <c:if test="${empty orderList}">
                        <tr>
                            <td colspan="7" class="text-center">
                                No orders found.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>

            <a href="${pageContext.request.contextPath}/cars"
               class="btn btn-secondary">
                Back to Cars
            </a>
               
        </div>
    </body>
</html>
