package com.parkit.parkingsystem;

//new
import com.parkit.parkingsystem.dao.TicketDAO;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    
    private Ticket ticket;
    
    //new
    private TicketDAO ticketDAO;
        
    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest() {
        ticket = new Ticket();
        
        //new
        ticketDAO = new TicketDAO();
    }

    @Test // Vérifier le calcul du tarif pour une voiture
    public void calculateFareCar(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.CAR_RATE_PER_HOUR);
    }

    @Test // Vérifier le calcul du tarif pour une moto
    public void calculateFareBike(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.BIKE_RATE_PER_HOUR);
    }

    @Test // Vérifier que le calcul échoue lorsque le type de place est inconnu ou nul
    public void calculateFareUnkownType(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, null,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test // Vérifier qu'une heure de sortie antérieure à l'heure d'entrée provoque une exception
    public void calculateFareBikeWithFutureInTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test // Vérifier que le tarif tient compte d'une durée inférieure à une heure pour une moto
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }

    @Test // Vérifier que le tarif tient compte d'une durée inférieure à une heure pour une voiture
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (0.75 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test // Vérifier le tarif d'un stationnement d'une journée entière
    public void calculateFareCarWithMoreThanADayParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );//24 hours parking time should give 24 * parking fare per hour
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }
    
    @Test // Vérifier que le stationnement est gratuit lorsqu'il dure moins de 30 minutes
    public void calculateFareCarWithLessThan30minutesParkingTime(){
    	Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  29 * 60 * 1000) );//29 minutes parking time should give 1/2 4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0, ticket.getPrice());	
    }
    
    @Test // Vérifier que la réduction de 5 % est appliquée lorsque le paramètre de réduction vaut true
	public void calculateFareCarWithdiscountApplied() {
		long durationInHour = 1; // test 1h
		Date inTime = new Date();
	    inTime.setTime(System.currentTimeMillis() - (durationInHour * 60 * 60 * 1000)); //test xh x 60mn x 60sec x 1000
	    Date outTime = new Date();
	    ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

	    ticket.setInTime(inTime);
	    ticket.setOutTime(outTime);
	    ticket.setParkingSpot(parkingSpot);

	    fareCalculatorService.calculateFare(ticket, true); // discountApplied forçé à Vrai
	    
	    double prixAttendu = (durationInHour * 1.5 * 0.95); // à calculer et saisir ici (NbHeure x Fare.CAR_RATE_PER_HOUR x Fare.REDUCTION_RATE éventuel)
	    double prixActuel = ticket.getPrice();

	    System.out.println("");
	    System.out.println("discountApplied forcé à vrai");
	    System.out.println("Tarif voiture  = " + Fare.CAR_RATE_PER_HOUR);
	    System.out.println("Prix attendu   = " + prixAttendu);
	    System.out.println("Prix actuel    = " + prixActuel);
	    
	    assertEquals(prixAttendu, prixActuel, 0.01);
	}
	
	
	@Test // Tester le calcul avec la détection d'un client récurrent via le DAO
   	      //Attention : si aucun ancien ticket n'est réellement enregistré dans la base utilisée, ce test ne valide pas correctement le scénario BDD
	public void calculateFareCarBdddiscountApplied() {
		long durationInHour = 1; //test 1h
	    Date inTime = new Date();
	    inTime.setTime(System.currentTimeMillis() - (durationInHour * 60 * 60 * 1000)); //test xh x 60mn x 60sec x 1000
	    Date outTime = new Date();
	    ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

	    ticket.setInTime(inTime);
	    ticket.setOutTime(outTime);
	    ticket.setParkingSpot(parkingSpot);

	    String vehicleRegNumber = "AZERT"; // Immat à saisir
	    ticket.setVehicleRegNumber(vehicleRegNumber);

	    boolean discountApplied = ticketDAO.hasExistingTickets(vehicleRegNumber, ticket.getId());
	    fareCalculatorService.calculateFare(ticket, discountApplied);
	    
	    double prixAttendu = (durationInHour * 1.5 * 0.95); // à calculer et saisir ici (NbHeure x Fare.CAR_RATE_PER_HOUR x Fare.REDUCTION_RATE éventuel)
	    double prixActuel = ticket.getPrice();
	    
	    System.out.println("");
	    System.out.println("Véhicule déjà venu = " + discountApplied + " " + vehicleRegNumber);
	    System.out.println("Tarif voiture      = " + Fare.CAR_RATE_PER_HOUR);
	    System.out.println("Prix attendu       = " + prixAttendu);
	    System.out.println("Prix actuel        = " + prixActuel);

	    assertEquals(prixAttendu, prixActuel, 0.01);
	}
    	
}
