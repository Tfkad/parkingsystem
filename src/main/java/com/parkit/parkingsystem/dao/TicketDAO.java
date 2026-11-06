package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class TicketDAO {

    private static final Logger logger = LogManager.getLogger("TicketDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    public boolean saveTicket(Ticket ticket){
        Connection con = null;
        //PreparedStatement ps = null; // tk
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.SAVE_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            //ps.setInt(1,ticket.getId());
            ps.setInt(1,ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new Timestamp(ticket.getInTime().getTime()));
            ps.setTimestamp(5, (ticket.getOutTime() == null)?null: (new Timestamp(ticket.getOutTime().getTime())) );
            //return ps.execute();
            return ps.executeUpdate() == 1; // tk
        } catch (Exception ex) {
            logger.error("Error fetching next available slot",ex); // Erreur lors de la récupération du prochain emplacement disponible
            return false; // tk
        } finally {
            // dataBaseConfig.closeConnection(con);
            // return false; renvoie donc toujours false, même si l'insertion réussit
            // dataBaseConfig.closePreparedStatement(ps);
            dataBaseConfig.closeConnection(con);
        }
    }

    public Ticket getTicket(String vehicleRegNumber) {
        Connection con = null;
        Ticket ticket = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1,vehicleRegNumber);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                ticket = new Ticket();
                ParkingSpot parkingSpot = new ParkingSpot(rs.getInt(1), ParkingType.valueOf(rs.getString(6)),false);
                ticket.setParkingSpot(parkingSpot);
                ticket.setId(rs.getInt(2));
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(rs.getDouble(3));
                ticket.setInTime(rs.getTimestamp(4));
                ticket.setOutTime(rs.getTimestamp(5));
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        } catch (Exception ex) {
            logger.error("Error fetching next available slot",ex);
        } finally {
            dataBaseConfig.closeConnection(con);
            return ticket;
        }
    }
       
    // tk
    public boolean hasExistingTickets(String vehicleRegNumber, int ticketId) {
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps =
                con.prepareStatement(DBConstants.EXIST_TICKET);
            ps.setString(1, vehicleRegNumber);
            ps.setInt(2, ticketId);
            ResultSet rs = ps.executeQuery();
            
            // return rs.next(); renvoie true même lorsqu'il n'y a aucun historique (zéro ticket)
            return rs.next() && rs.getInt(1) > 0; // renvoie true uniquement si un ancien ticket terminé existe
            
        } catch (Exception ex) {
            logger.error("Error checking vehicle history", ex);
            return false;
        } finally {
            dataBaseConfig.closeConnection(con);
        }
    }
    
    public boolean updateTicket(Ticket ticket) {
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_TICKET);
            ps.setDouble(1, ticket.getPrice());
            ps.setTimestamp(2, new Timestamp(ticket.getOutTime().getTime()));
            ps.setInt(3,ticket.getId());
            
            ps.execute();
            return true; // peut renvoyer true sans vérifier qu’un ticket a effectivement été mis à jour
            // int rowsUpdated = ps.executeUpdate();
            // return rowsUpdated == 1;
            
        } catch (Exception ex) {
            logger.error("Error saving ticket info",ex);
        } finally {
            dataBaseConfig.closeConnection(con);
        }
        return false;
    }
}
