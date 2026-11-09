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

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingServiceTest {

    private static ParkingService parkingService;
    
    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach   
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
   
    @Test // Vérifier que la sortie d'un véhicule se passe bien
    public void testProcessExitingVehicle() {
    	 when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
         parkingService.processExitingVehicle();         
         verify(ticketDAO).updateTicket(any(Ticket.class));         
         verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
    }
    
    @Test // Vérifier que la sortie d'un véhicule avec la réduction se passe bien
    public void testProcessExitingVehicleWithDiscount() {
    	when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);
        parkingService.processExitingVehicle();
        verify(ticketDAO).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
    }
    
    @Test // Vérifier que l'entrée d'un véhicule se passe bien
    public void testProcessIncomingVehicle() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); //
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1); //
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
        parkingService.processIncomingVehicle();
        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
        verify(ticketDAO).saveTicket(any(Ticket.class)); 
    }
    
    @Test // Vérifier que si la mise à jour du ticket échoue, la place n'est pas libérée.
    public void testProcessExitingVehicleUnableUpdate() {
    	when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
    	when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        parkingService.processExitingVehicle();
        verify(ticketDAO).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }
    
    @Test // Vérifier qu'une place disponible est correctement retournée
    public void testGetNextParkingNumberIfAvailable() {
    	 when(inputReaderUtil.readSelection()).thenReturn(1);
         when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
         ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
         verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
         assertEquals(1, result.getId());
         assertTrue(result.isAvailable());    	
    }
    
    @Test // Vérifier que la méthode retourne null lorsqu'aucune place n'est disponible
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
    	when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertNull(result);
    }
    
    @Test // Vérifier que la saisie d'un type de véhicule invalide, par exemple 3, retourne null
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
    	when(inputReaderUtil.readSelection()).thenReturn(3);
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
        verify(parkingSpotDAO, Mockito.never()).getNextAvailableSlot(any(ParkingType.class));
        assertNull(result);
    }

    @Test // Vérifier qu'aucun ticket n'est créé et qu'aucune place n'est réservée lorsque le parking est complet
    public void testProcessIncomingVehicleWhenParkingIsFull() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class)))
                .thenReturn(0);
        parkingService.processIncomingVehicle();
        verify(ticketDAO, never()).saveTicket(any(Ticket.class));
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
    
    // Permet de simuler une saisie au clavier et de réinitialiser le Scanner
    private void setScannerInput(String input) throws Exception {
        System.setIn(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)));
        Field field = InputReaderUtil.class.getDeclaredField("scan");
        field.setAccessible(true);
        field.set(null, new Scanner(System.in));
    }
    
    @Test // Vérifier que la lecture d'un nombre valide fonctionne, on simule la saisie de 1 au clavier
    void testReadSelectionValidInput() throws Exception {
        setScannerInput("1\n");
        InputReaderUtil reader = new InputReaderUtil();
        assertEquals(1, reader.readSelection());
    }

    @Test // Vérifier que la méthode gère correctement une saisie invalide, on simule la saisie de abc au clavier
    void testReadSelectionInvalidInput() throws Exception {
        setScannerInput("abc\n");
        InputReaderUtil reader = new InputReaderUtil();
        assertEquals(-1, reader.readSelection());
    }

    @Test // Vérifier que la lecture d'une plaque d'immatriculation valide fonctionne, on simule la saisie de ABCDEF au clavier
    void testReadVehicleRegistrationNumberValidInput() throws Exception {
        setScannerInput("ABCDEF\n");
        InputReaderUtil reader = new InputReaderUtil();
        assertEquals("ABCDEF", reader.readVehicleRegistrationNumber());
    }

    @Test // Vérifier que le programme refuse une plaque d'immatriculation vide, on simule une saisie vide (l'utilisateur appuie simplement sur Entrée)
    void testReadVehicleRegistrationNumberEmptyInput() throws Exception {
        setScannerInput("\n");
        InputReaderUtil reader = new InputReaderUtil();
        assertThrows(IllegalArgumentException.class,
                () -> reader.readVehicleRegistrationNumber());
    }
    
}
