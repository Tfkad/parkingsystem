package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach   
    
    /*private void setUpPerTest() {
        parkingService = new ParkingService(
            inputReaderUtil,
            parkingSpotDAO,
            ticketDAO
        );
    }*/
    
    private void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);            
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }
   
    @Test // Vérifier qu'une place disponible est correctement retournée pour une voiture, avec l'identifiant 1
    public void testGetNextParkingNumberIfAvailable() { 
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR))
                .thenReturn(1);

        ParkingSpot result =
                parkingService.getNextParkingNumberIfAvailable();

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(ParkingType.CAR, result.getParkingType());
        assertTrue(result.isAvailable());

        verify(parkingSpotDAO, times(1))
                .getNextAvailableSlot(ParkingType.CAR);
    }

    @Test // Vérifier qu'une place adaptée à une moto est sélectionnée lorsque l'utilisateur choisit le type BIKE
    public void testGetNextParkingNumberIfAvailableForBike() {
        when(inputReaderUtil.readSelection()).thenReturn(2);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE))
                .thenReturn(2);

        ParkingSpot result =
                parkingService.getNextParkingNumberIfAvailable();

        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals(ParkingType.BIKE, result.getParkingType());

        verify(parkingSpotDAO, times(1))
                .getNextAvailableSlot(ParkingType.BIKE);
    }

    @Test // Vérifier que la méthode retourne null lorsqu'aucune place n'est disponible
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() { 
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR))
                .thenReturn(0);

        ParkingSpot result =
                parkingService.getNextParkingNumberIfAvailable();

        assertNull(result);

        verify(parkingSpotDAO, times(1))
                .getNextAvailableSlot(ParkingType.CAR);
    }

    @Test // Vérifier que la saisie d'un type de véhicule invalide, par exemple 3, retourne null
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() { 
        when(inputReaderUtil.readSelection()).thenReturn(3);

        ParkingSpot result =
                parkingService.getNextParkingNumberIfAvailable();

        assertNull(result);

        verify(parkingSpotDAO, never())
                .getNextAvailableSlot(any(ParkingType.class));
    }

    @Test // Vérifier qu'un véhicule peut entrer : sélection du type, attribution d'une place, réservation de celle-ci et sauvegarde du ticket
    public void testProcessIncomingVehicle() throws Exception { // correction du nom processIncomingVehicleTest
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber())
                .thenReturn("ABCDEF");

        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR))
                .thenReturn(1);

        when(parkingSpotDAO.updateParking(any(ParkingSpot.class)))
                .thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO)
                .getNextAvailableSlot(ParkingType.CAR);

        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
        verify(ticketDAO).saveTicket(any(Ticket.class));
        
    }

    @Test // Vérifier qu'aucun ticket n'est créé et qu'aucune place n'est réservée lorsque le parking est complet
    public void testProcessIncomingVehicleWhenParkingIsFull() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR))
                .thenReturn(0);

        parkingService.processIncomingVehicle();

        verify(ticketDAO, never()).saveTicket(any(Ticket.class));
        verify(parkingSpotDAO, never())
                .updateParking(any(ParkingSpot.class));
    }
    
    /*@Test
    public void testProcessExitingVehicle(){
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }*/
    
    @Test // Vérifier que la sortie d'un véhicule se déroule correctement : lecture de l'immatriculation, récupération du ticket, calcul du tarif, mise à jour du ticket et libération de la place
    public void testProcessExitingVehicle() throws Exception {
        ParkingSpot spot =
                new ParkingSpot(1, ParkingType.CAR, false);

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(0);
        ticket.setInTime(
                new Date(System.currentTimeMillis() - 60 * 60 * 1000L)
        );

        when(inputReaderUtil.readVehicleRegistrationNumber())
                .thenReturn("ABCDEF");

        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);

        when(ticketDAO.hasExistingTickets(anyString(), anyInt()))
                .thenReturn(false);

        when(ticketDAO.updateTicket(any(Ticket.class)))
                .thenReturn(true);

        when(parkingSpotDAO.updateParking(any(ParkingSpot.class)))
                .thenReturn(true);

        parkingService.processExitingVehicle();

        assertNotNull(ticket.getOutTime());

        verify(ticketDAO).getTicket("ABCDEF");

        verify(ticketDAO).hasExistingTickets(
                eq("ABCDEF"), eq(ticket.getId())
        );

        verify(ticketDAO).updateTicket(ticket);

        assertTrue(spot.isAvailable());

        verify(parkingSpotDAO).updateParking(spot);
    }

    @Test // Vérifier que si la mise à jour du ticket échoue, la place n'est pas libérée.
    public void testProcessExitingVehicleUnableUpdate()
            throws Exception {

        ParkingSpot spot =
                new ParkingSpot(1, ParkingType.CAR, false);

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(
                new Date(System.currentTimeMillis() - 60 * 60 * 1000L)
        );

        when(inputReaderUtil.readVehicleRegistrationNumber())
                .thenReturn("ABCDEF");

        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);

        when(ticketDAO.hasExistingTickets(anyString(), anyInt()))
                .thenReturn(false);

        when(ticketDAO.updateTicket(any(Ticket.class)))
                .thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO).updateTicket(ticket);

        // La place ne doit pas être libérée si la mise à jour échoue.
        assertFalse(spot.isAvailable());

        verify(parkingSpotDAO, never())
                .updateParking(any(ParkingSpot.class));
    }
   
    @Test // Vérifier le comportement lorsque le DAO échoue à mettre à jour la disponibilité de la place (tester l’échec de réservation d’une place à l’entrée)
    public void testProcessIncomingVehicleWhenParkingUpdateFails() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR))
                .thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber())
                .thenReturn("ABCDEF");
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class)))
                .thenReturn(false);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
        verify(ticketDAO).saveTicket(any(Ticket.class));
    }
    
    @Test // Vérifier que le service intercepte l'erreur lorsqu'aucun ticket n'existe pour l'immatriculation saisie, sans mettre à jour de ticket ni de place (tester l’absence de ticket à la sortie)
    public void testProcessExitingVehicleWhenTicketNotFound() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber())
                .thenReturn("UNKNOWN");
        when(ticketDAO.getTicket("UNKNOWN")).thenReturn(null);

        assertDoesNotThrow(() -> parkingService.processExitingVehicle());

        verify(ticketDAO, never()).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }
    
}
