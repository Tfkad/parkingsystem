package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
	public void calculateFare(Ticket ticket) {
	    calculateFare(ticket, false);
	}
	
	public void calculateFare(Ticket ticket, boolean discountApplied) {
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        /*new pour tenir comptes des minutes et pas seulement des heures
              calcul de la durée de stationnement en millisecondes (type long) car getTime() retourne des millisecondes
              durationInMilliseconds = heure de sortie en millisecondes moins heure d'entrée en millisecondes
        */
        long durationInMilliseconds = ticket.getOutTime().getTime() - ticket.getInTime().getTime(); //entier long sans décimale
        
        /*    le ticketPrice est en euros par heure donc conversion des millisecondes en heures
              1sec = 1000 millisecondes donc 1h en millisecondes = 60mn * 60sec * 1000 
        	  la variable durationInHour doit correspondre à des heures en décimales (en base 10 et pas 60)
                 on divise donc durationInMilliseconds par l'équivalent d'1 heure en millisecondes (60.0 * 60.0 * 1000.0) ou 3 600 000
              exemple pour 30mn : 30mn donne en millisecondes (30 * 60 * 1000) ou 1 800 000
                 et ces millisecondes converti en heure =  1 800 000 / 3 600 000 = 0,5
        */
        double durationInHour = durationInMilliseconds / (60.0 * 60.0 * 1000.0); // heures décimales

        // gratuit si moins de 30mn ou 1/2heure ou 0,5 heures
        if (durationInHour < Fare.FREE_DURATION) {
        	ticket.setPrice(0);
        }
        else
        {
        	
        double ticketPrice;
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticketPrice = durationInHour * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                ticketPrice = durationInHour * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        
        // arrondir le ticketPrice à 2 décimales car sinon 15 chiffres après la virgule
        // ticketPrice = Math.round(ticketPrice * 100.0) / 100.0;  arrondi non demandé !
        
        ticket.setPrice(ticketPrice);
        if (discountApplied) {
        	// appliquer la règle prévue pour un véhicule déjà venu, réduction de x%
        	ticketPrice = ticketPrice * Fare.REDUCTION_RATE;
        	ticket.setPrice(ticketPrice);
        	}
        }
        
    }
}