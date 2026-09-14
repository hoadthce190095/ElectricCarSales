
<%-- 
    Document   : order-success
    Created on : Sep 12, 2026, 11:36:24 AM
    Author     : Hoa Hà
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>

    <head>
        <meta charset="UTF-8">
        <title>Order Detail</title>

        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
              rel="stylesheet">
    </head>

    <body>

        <div class="container mt-5">

            <h2 class="mb-4">Order Detail</h2>

            <!-- Order Information -->
            <div class="card mb-4">

                <div class="card-header">
                    <strong>Order Information</strong>
                </div>

                <div class="card-body">

                    <div class="row">

                        <!-- LEFT -->
                        <div class="col-md-6">

                            <p>
                                <strong>Order ID:</strong>
                                ${order.orderID}
                            </p>

                            <p>
                                <strong>Customer:</strong>
                                ${order.customerName}
                            </p>

                            <p>
                                <strong>Phone:</strong>
                                ${order.phone}
                            </p>

                            <p>
                                <strong>Email:</strong>
                                ${order.email}
                            </p>

                        </div>


                        <!-- RIGHT -->
                        <div class="col-md-6">

                            <p>
                                <strong>Address:</strong>
                                ${order.address}
                            </p>

                            <p>
                                <strong>Order Date:</strong>
                                ${order.orderDate}
                            </p>


                            <!-- STATUS -->
                            <p>
                                <strong>Status:</strong>

                                <span class="badge bg-warning text-dark">
                                    ${order.orderStatus}
                                </span>
                            </p>


                            <!-- EMPLOYEE ASSIGNMENT -->
                            <div class="mt-3">

                                <p>
                                    <strong>Current Employee:</strong>

                                    <c:choose>

                                        <c:when test="${not empty order.employeeName}">
                                            ${order.employeeName}
                                        </c:when>

                                        <c:otherwise>

                                            <span class="text-danger">
                                                Not assigned
                                            </span>

                                        </c:otherwise>

                                    </c:choose>

                                </p>


                                <form action="${pageContext.request.contextPath}/order"
                                      method="post">

                                    <input type="hidden"
                                           name="action"
                                           value="assignEmployee">

                                    <input type="hidden"
                                           name="orderID"
                                           value="${order.orderID}">


                                    <div class="mb-2">

                                        <label class="form-label">

                                            <strong>
                                                Select Sales Employee:
                                            </strong>

                                        </label>


                                        <select name="employeeID"
                                                class="form-select"
                                                required>

                                            <option value="">
                                                -- Select Employee --
                                            </option>


                                            <c:forEach var="employee"
                                                       items="${employeeList}">

                                                <option value="${employee.employeeID}"
                                                        ${employee.employeeID == order.employeeID
                                                          ? 'selected' : ''}>

                                                    ${employee.fullName}
                                                    - ${employee.phone}

                                                </option>

                                            </c:forEach>

                                        </select>

                                    </div>


                                    <button type="submit"
                                            class="btn btn-primary">

                                        Assign Employee

                                    </button>

                                </form>

                            </div>


                            <!-- UPDATE STATUS -->
                            <div class="mt-4">

                                <form action="${pageContext.request.contextPath}/order"
                                      method="post">

                                    <input type="hidden"
                                           name="action"
                                           value="updateStatus">

                                    <input type="hidden"
                                           name="orderID"
                                           value="${order.orderID}">


                                    <div class="mb-3">

                                        <label class="form-label">

                                            <strong>
                                                Update Status:
                                            </strong>

                                        </label>


                                        <select name="status"
                                                class="form-select">


                                            <!-- PENDING -->

                                            <c:if test="${order.orderStatus == 'PENDING'}">

                                                <option value="PENDING"
                                                        selected>
                                                    PENDING
                                                </option>

                                                <option value="CONFIRMED">
                                                    CONFIRMED
                                                </option>

                                                <option value="CANCELLED">
                                                    CANCELLED
                                                </option>

                                            </c:if>


                                            <!-- CONFIRMED -->

                                            <c:if test="${order.orderStatus == 'CONFIRMED'}">

                                                <option value="CONFIRMED"
                                                        selected>
                                                    CONFIRMED
                                                </option>

                                                <option value="COMPLETED">
                                                    COMPLETED
                                                </option>

                                                <option value="CANCELLED">
                                                    CANCELLED
                                                </option>

                                            </c:if>


                                            <!-- COMPLETED -->

                                            <c:if test="${order.orderStatus == 'COMPLETED'}">

                                                <option value="COMPLETED"
                                                        selected>
                                                    COMPLETED
                                                </option>

                                            </c:if>


                                            <!-- CANCELLED -->

                                            <c:if test="${order.orderStatus == 'CANCELLED'}">

                                                <option value="CANCELLED"
                                                        selected>
                                                    CANCELLED
                                                </option>

                                            </c:if>

                                        </select>

                                    </div>


                                    <button type="submit"
                                            class="btn btn-success">

                                        Update Status

                                    </button>

                                </form>

                            </div>


                            <!-- TOTAL -->

                            <p class="mt-3">

                                <strong>
                                    Total Amount:
                                </strong>

                                ${order.totalAmount}

                            </p>

                        </div>

                    </div>

                </div>

            </div>


            <!-- VEHICLE INFORMATION -->

            <div class="card mb-4">

                <div class="card-header">

                    <strong>
                        Vehicle Information
                    </strong>

                </div>


                <div class="card-body">

                    <c:forEach var="detail"
                               items="${detailList}">

                        <table class="table table-bordered">

                            <tr>
                                <th>Car ID</th>
                                <td>${detail.carID}</td>
                            </tr>

                            <tr>
                                <th>VIN</th>
                                <td>${detail.vin}</td>
                            </tr>

                            <tr>
                                <th>Brand</th>
                                <td>${detail.brandName}</td>
                            </tr>

                            <tr>
                                <th>Model</th>
                                <td>${detail.modelName}</td>
                            </tr>

                            <tr>
                                <th>Color</th>
                                <td>${detail.color}</td>
                            </tr>

                            <tr>
                                <th>Manufacturing Year</th>
                                <td>${detail.manufacturingYear}</td>
                            </tr>

                            <tr>
                                <th>Battery Capacity</th>
                                <td>
                                    ${detail.batteryCapacity} kWh
                                </td>
                            </tr>

                            <tr>
                                <th>Range</th>
                                <td>
                                    ${detail.rangeKM} km
                                </td>
                            </tr>

                            <tr>
                                <th>Quantity</th>
                                <td>${detail.quantity}</td>
                            </tr>

                            <tr>
                                <th>Unit Price</th>
                                <td>
                                    ${detail.unitPrice}
                                </td>
                            </tr>

                            <tr>
                                <th>Sub Total</th>
                                <td>
                                    ${detail.subTotal}
                                </td>
                            </tr>

                        </table>

                    </c:forEach>

                </div>

            </div>


            <!-- BACK -->

            <div class="mt-4 mb-5">

                <a href="${pageContext.request.contextPath}/order"
                   class="btn btn-secondary">

                    Back to Orders

                </a>

            </div>

        </div>

    </body>

</html>