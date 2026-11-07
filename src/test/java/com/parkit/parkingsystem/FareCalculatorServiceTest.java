package com.parkit.parkingsystem;

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
        
    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest() {
        ticket = new Ticket();
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

    // VOITURE
    
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
    
    @Test // Vérifier que le stationnement d'une voiture est gratuit lorsqu'il dure moins de 30 minutes
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
	public void calculateFareCarWithDiscountApplied() {
		// 45 mn = 0.75 en heures décimales (base 10)
		Date inTime = new Date();
	    inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000)); //45mn x 60sec x 1000
	    Date outTime = new Date();
	    ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
	    ticket.setInTime(inTime);
	    ticket.setOutTime(outTime);
	    ticket.setParkingSpot(parkingSpot);
	    fareCalculatorService.calculateFare(ticket, true); // discountApplied forçé à Vrai	    
	    double prixAttendu = (0.75 * Fare.CAR_RATE_PER_HOUR * Fare.REDUCTION_RATE); // durée en heures décimales * taux horaire * remise
	    double prixActuel = ticket.getPrice();
	    assertEquals(prixAttendu, prixActuel, 0.01);
	}
		
	// MOTO
    
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
    
    @Test // Vérifier que le stationnement d'une moto est gratuit lorsqu'il dure moins de 30 minutes
    public void calculateFareBikeWithLessThan30minutesParkingTime () {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (29 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.0, ticket.getPrice());
    }

    @Test // Vérifier que la réduction de 5 % est appliquée lorsque le paramètre de réduction vaut true
	public void calculateFareBikeWithDiscountApplied() {
		// 45 mn = 0.75 en heures décimales (base 10)
		Date inTime = new Date();
	    inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000)); //45mn x 60sec x 1000
	    Date outTime = new Date();
	    ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
	    ticket.setInTime(inTime);
	    ticket.setOutTime(outTime);
	    ticket.setParkingSpot(parkingSpot);
	    fareCalculatorService.calculateFare(ticket, true); // discountApplied forçé à Vrai	    
	    double prixAttendu = (0.75 * Fare.BIKE_RATE_PER_HOUR * Fare.REDUCTION_RATE); // durée en heures décimales * taux horaire * remise
	    double prixActuel = ticket.getPrice();
	    assertEquals(prixAttendu, prixActuel, 0.01);
	}
    
}
