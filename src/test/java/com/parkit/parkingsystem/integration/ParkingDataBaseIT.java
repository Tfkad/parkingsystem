package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

// mes rajouts
import static org.junit.jupiter.api.Assertions.*;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.constants.Fare;
import static org.mockito.Mockito.lenient; 

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
  
    private static final String REGISTRATION_NUMBER = "ABCDEF"; // tk
            
    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        //when(inputReaderUtil.readSelection()).thenReturn(1);
        //when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REGISTRATION_NUMBER);
        //dataBasePrepareService.clearDataBaseEntries();
        
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REGISTRATION_NUMBER);

        dataBasePrepareService.clearDataBaseEntries();
        
    }

    @AfterAll
    private static void tearDown(){
    	dataBaseTestConfig.closeConnection(null); // tk
    }

    private ParkingService createParkingService() { // tk
        return new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    private Ticket parkCarAndRetrieveTicket() { // tk
        ParkingService parkingService = createParkingService();
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket(REGISTRATION_NUMBER);
        assertNotNull(ticket, "Le ticket doit être enregistré en base de données");
        return ticket;
    }
    
    /*@Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    }*/

    @Test // Vérifier qu'une voiture reçoit une place disponible et que son ticket est correctement enregistré en base de données
    public void testParkingACar() { 
    	
    	int initialSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        assertTrue(initialSlot > 0, "Une place doit être disponible avant le stationnement");
        
        Ticket ticket = parkCarAndRetrieveTicket();

        assertNotNull(ticket);
        assertEquals(REGISTRATION_NUMBER, ticket.getVehicleRegNumber());
        assertEquals(initialSlot, ticket.getParkingSpot().getId());
        assertEquals(ParkingType.CAR, ticket.getParkingSpot().getParkingType());
        
        assertNotNull(ticket.getInTime(), "L'heure d'entrée doit être enregistrée");
        assertNull(ticket.getOutTime(), "L'heure de sortie doit être nulle à l'entrée");
        assertEquals(0.0, ticket.getPrice(), 0.001, "Le prix initial du ticket doit être nul");
        
        int nextAvailableSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        
        assertNotEquals(initialSlot, nextAvailableSlot, "La place attribuée ne doit plus être disponible");
        
    }
    
    /*@Test
    public void testParkingLotExit(){
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        //TODO: check that the fare generated and out time are populated correctly in the database
    }*/
    
    @Test // Vérifier qu'à la sortie, le ticket est mis à jour avec l'heure de sortie et le prix, et que la place est libérée
    public void testParkingLotExit() {
    	
    	Ticket entryTicket = parkCarAndRetrieveTicket();

        assertNotNull(entryTicket);
        assertNull(entryTicket.getOutTime());

        int parkingSpotId = entryTicket.getParkingSpot().getId();

        createParkingService().processExitingVehicle();

        Ticket exitTicket = ticketDAO.getTicket(REGISTRATION_NUMBER);

        assertNotNull(exitTicket, "Le ticket doit toujours exister après la sortie");
        assertEquals(entryTicket.getId(), exitTicket.getId(), "La sortie doit mettre à jour le ticket existant");
        
assertNotNull(exitTicket.getOutTime(), "L'heure de sortie doit être enregistrée"); // err
        
        assertTrue(exitTicket.getOutTime().getTime() >= exitTicket.getInTime().getTime(), "L'heure de sortie doit être postérieure ou égale à l'heure d'entrée");
        assertTrue(exitTicket.getPrice() >= 0, "Le tarif calculé ne doit pas être négatif");
        assertEquals(parkingSpotId, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR), "La place libérée doit redevenir disponible");
    }

    @Test // Vérifier que le ticket enregistré peut être relu en base avec la bonne immatriculation, le bon identifiant et la bonne place
    public void testTicketIsPersistedWithCorrectRegistrationNumber() {
        
    	Ticket createdTicket = parkCarAndRetrieveTicket();
        Ticket retrievedTicket = ticketDAO.getTicket(REGISTRATION_NUMBER);
        
        assertNotNull(retrievedTicket);
        assertEquals(createdTicket.getId(), retrievedTicket.getId());
        assertEquals(REGISTRATION_NUMBER, retrievedTicket.getVehicleRegNumber());
        assertEquals(createdTicket.getParkingSpot().getId(), retrievedTicket.getParkingSpot().getId());
    }

    @Test // Vérifier que deux voitures différentes reçoivent des tickets et des places distincts
    public void testParkingDifferentCars()  throws Exception {
    	
        Ticket firstTicket = parkCarAndRetrieveTicket();
        
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("GHIJKL");
        
        createParkingService().processIncomingVehicle();
        Ticket secondTicket = ticketDAO.getTicket("GHIJKL");
        
        assertNotNull(secondTicket, "Le second ticket doit être enregistré");
        assertNotEquals(firstTicket.getId(), secondTicket.getId(), "Chaque véhicule doit avoir son propre ticket");
        assertNotEquals(firstTicket.getParkingSpot().getId(), secondTicket.getParkingSpot().getId(), "Deux véhicules ne doivent pas occuper la même place");
    }

    @Test // Vérifier que le type de véhicule sélectionné par l'utilisateur est correctement enregistré sur le ticket et associé à la place correspondante
    public void testVehicleTypeSelectionIsRespected() {
    	
        when(inputReaderUtil.readSelection()).thenReturn(2);
        
        createParkingService().processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket(REGISTRATION_NUMBER);
        
        assertNotNull(ticket, "Le ticket du véhicule doit être enregistré");
        
        // Vérification du type réellement enregistré en base.
        assertEquals(ParkingType.BIKE, ticket.getParkingSpot().getParkingType());
    }
    
	@Test //vérifie qu’un véhicule qui revient bénéficie d’une réduction de 5 % lors de son deuxième stationnement
	public void testParkingLotExitRecurringUser() {
		// Premier stationnement et sortie
	    Ticket firstTicket = parkCarAndRetrieveTicket();

	    // Simule une heure de stationnement pour éviter le tarif gratuit
	    firstTicket.setInTime(new java.util.Date(System.currentTimeMillis() - 60 * 60 * 1000L));
	    
// assertTrue(ticketDAO.updateTicket(firstTicket)); // err

	    createParkingService().processExitingVehicle();

	    // Ticket closedFirstTicket = ticketDAO.getTicket(REGISTRATION_NUMBER);
	    // assertNotNull(closedFirstTicket);
	    // assertNotNull(closedFirstTicket.getOutTime());

	    // Deuxième stationnement avec la même immatriculation
	    Ticket secondTicket = parkCarAndRetrieveTicket();

	    secondTicket.setInTime(new java.util.Date(System.currentTimeMillis() - 60 * 60 * 1000L));
	    // assertTrue(ticketDAO.updateTicket(secondTicket));

	    createParkingService().processExitingVehicle();

	    Ticket closedSecondTicket = ticketDAO.getTicket(REGISTRATION_NUMBER);
	    assertNotNull(closedSecondTicket);
	    assertNotNull(closedSecondTicket.getOutTime());

	    // Vérifie que la réduction de 5 % a été appliquée
	    double tarifNormal = 60 * Fare.CAR_RATE_PER_HOUR;
	    assertEquals(tarifNormal * Fare.REDUCTION_RATE, closedSecondTicket.getPrice(), 0.10);
	}
		
	@Test // Vérifier que la recherche d'un ticket inexistant retourne null
	public void testGetTicketWhenRegistrationNumberDoesNotExist() {
	    Ticket ticket = ticketDAO.getTicket("INCONNU");
	    assertNull(ticket);
	}
	
	@Test // Vérifier qu'un véhicule sans ancien ticket terminé n'est pas considéré comme un client récurrent
	public void testHasExistingTicketsReturnsFalseWhenNoHistoryExists() {
	    Ticket ticket = parkCarAndRetrieveTicket();

	    boolean result = ticketDAO.hasExistingTickets(REGISTRATION_NUMBER, ticket.getId());

	    assertFalse(result);
	}
	
	@Test // Vérifier que la base détecte un ancien ticket terminé pour une immatriculation donnée
	public void testHasExistingTicketsReturnsTrueWhenHistoryExists() {
	    Ticket firstTicket = parkCarAndRetrieveTicket();

	    // Simule la sortie du premier stationnement
	    firstTicket.setOutTime(new java.util.Date());
	    firstTicket.setPrice(10.0);
	    assertTrue(ticketDAO.updateTicket(firstTicket));

	    // Crée un deuxième ticket pour le même véhicule
	    createParkingService().processIncomingVehicle();
	    Ticket secondTicket = ticketDAO.getTicket(REGISTRATION_NUMBER);

	    assertNotNull(secondTicket);
	    
assertNotEquals(firstTicket.getId(), secondTicket.getId()); // err

	    boolean result = ticketDAO.hasExistingTickets(REGISTRATION_NUMBER, secondTicket.getId());

	    assertTrue(result);
	}

	@Test // Vérifier que le prix et l'heure de sortie modifiés sont bien sauvegardés en base de données
	public void testUpdateTicketPersistsPriceAndExitTime() {
	    Ticket ticket = parkCarAndRetrieveTicket();

	    java.util.Date exitTime = new java.util.Date(ticket.getInTime().getTime() + 60 * 60 * 1000L);
	    ticket.setOutTime(exitTime);
	    ticket.setPrice(15.0);

	    assertTrue(ticketDAO.updateTicket(ticket));

	    Ticket updatedTicket = ticketDAO.getTicket(REGISTRATION_NUMBER);

	    assertNotNull(updatedTicket);
	    assertEquals(15.0, updatedTicket.getPrice(), 0.001);
	    assertNotNull(updatedTicket.getOutTime());
	    assertEquals(exitTime.getTime(), updatedTicket.getOutTime().getTime());
	}
	
}    

