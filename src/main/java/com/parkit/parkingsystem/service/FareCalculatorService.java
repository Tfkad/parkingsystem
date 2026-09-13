package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        //old int inHour = ticket.getInTime().getHours();
        //old int outHour = ticket.getOutTime().getHours();
        
        //new pour tenir comptes des minutes et pas seulement des heures
        //    calcul de la durée de stationnement en millisecondes (type long) car getTime() retourne des millisecondes
        //    heure de sortie en millisecondes moins heure d'entrée en millisecondes
        long dureeMillisecondes = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        //    le prix est en euros par heure donc conversion des millisecondes en heures
        //    1sec = 1000 millisecondes donc 1h en millisecondes = 60mn * 60sec * 1000 
        double duration = dureeMillisecondes / (60.0 * 60.0 * 1000.0);

        //TODO: Some tests are failing here. Need to check if this logic is correct
        //old int duration = outHour - inHour;
        
        //new 
        double prix;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                //old ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                prix = duration * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                //old ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                prix = duration * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        
        //new arrondir le prix à 2 décimales
        prix = Math.round(prix * 100.0) / 100.0;
        ticket.setPrice(prix);
    }
}