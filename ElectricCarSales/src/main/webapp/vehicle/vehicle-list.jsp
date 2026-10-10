<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Vehicle List</title>
</head>
<body>

<h1>Electric Vehicle List</h1>

<table border="1">
    <thead>
        <tr>
            <th>Model ID</th>
            <th>Car Line ID</th>
            <th>Model Name</th>
            <th>Description</th>
        </tr>
    </thead>

    <tbody>
        <c:forEach var="carModel" items="${carModels}">
            <tr>
                <td>${carModel.modelID}</td>
                <td>${carModel.carLineID}</td>
                <td>${carModel.modelName}</td>
                <td>${carModel.description}</td>
            </tr>
        </c:forEach>
    </tbody>
</table>

</body>
</html>
