package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationRequest;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC queries follow the provided ElectricCarSalesDB.sql exactly. */
public class ConsultationRequestDAO {
    public List<ConsultationRequest> findAll() {
        List<ConsultationRequest> result=new ArrayList<>();
        String sql="""
            SELECT cr.ConsultationRequestID, cr.CustomerID, cr.EmployeeID,cr.ModelID,
                cr.ConsultationStatusID,cr.RequestContent,cr.RequestDate,cs.StatusName,
                c.CustomerName, c.Phone AS CustomerPhone,c.Email AS CustomerEmail,
                cm.ModelName, e.EmployeeName
            FROM ConsultationRequests cr
            JOIN ConsultationStatuses cs ON cs.ConsultationStatusID=cr.ConsultationStatusID
            JOIN Customers c ON c.CustomerID=cr.CustomerID
            LEFT JOIN CarModels cm ON cm.ModelID=cr.ModelID
            LEFT JOIN Employees e ON e.EmployeeID=cr.EmployeeID
            ORDER BY cr.RequestDate DESC
            """;
        try(Connection cn=DBContext.openConnection();
            PreparedStatement ps=cn.prepareStatement(sql);ResultSet rs=ps.executeQuery()) {
            while(rs.next()) {
                ConsultationRequest row=new ConsultationRequest();
                row.setId(rs.getInt("ConsultationRequestID"));
                row.setCustomerId(rs.getInt("CustomerID"));
                row.setStaffId(rs.getInt("EmployeeID"));
                row.setModelId(rs.getInt("ModelID"));
                row.setStatusId(rs.getInt("ConsultationStatusID"));
                row.setContent(rs.getString("RequestContent"));
                row.setCreatedAt(rs.getTimestamp("RequestDate"));
                row.setStatus(rs.getString("StatusName"));
                row.setCustomerName(rs.getString("CustomerName"));
                row.setCustomerPhone(rs.getString("CustomerPhone"));
                row.setCustomerEmail(rs.getString("CustomerEmail"));
                row.setModelName(rs.getString("ModelName"));
                row.setEmployeeName(rs.getString("EmployeeName"));
                result.add(row);
            }
        } catch(SQLException e){ throw new IllegalStateException("Cannot load consultation requests",e); }
        return result;
    }

    public void insert(ConsultationRequest request) {
        if(request.getCustomerId()<=0 || request.getContent()==null || request.getContent().isBlank())
            throw new IllegalArgumentException("Customer and request content are required");
        String sql="""
            INSERT INTO ConsultationRequests(CustomerID,EmployeeID,ModelID,ConsultationStatusID,RequestContent)
            SELECT ?,NULL,?,ConsultationStatusID,? FROM ConsultationStatuses WHERE StatusName='PENDING'
            """;
        try(Connection cn=DBContext.openConnection();PreparedStatement ps=cn.prepareStatement(sql)){
            ps.setInt(1,request.getCustomerId());
            if(request.getModelId()>0) ps.setInt(2,request.getModelId());
            else ps.setNull(2,Types.INTEGER);
            ps.setNString(3,request.getContent());
            if(ps.executeUpdate()!=1) throw new IllegalStateException("PENDING consultation status does not exist");
        }catch(SQLException e){throw new IllegalStateException("Cannot create consultation request",e);}
    }

    public void updateStatus(int id,int statusId,Integer employeeId){
        String sql="UPDATE ConsultationRequests SET ConsultationStatusID=?,EmployeeID=? WHERE ConsultationRequestID=?";
        try(Connection cn=DBContext.openConnection();PreparedStatement ps=cn.prepareStatement(sql)) {
            ps.setInt(1,statusId);
            if(employeeId==null || employeeId<=0) ps.setNull(2,Types.INTEGER); else ps.setInt(2,employeeId);
            ps.setInt(3,id);
            if(ps.executeUpdate()!=1) throw new IllegalArgumentException("Consultation request not found");
        }catch(SQLException e){throw new IllegalStateException("Cannot update consultation request",e);}
    }
}
