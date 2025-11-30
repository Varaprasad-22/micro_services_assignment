package com.bookingapp.model;

import java.sql.Date;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Document(collection="booking")
public class BookingEntity {
	
	@Id
	private String bookingId;
	private String emailId;
	
	private String pnr;
	private String userId;
	private Integer flightId;
	private LocalDateTime bookingTime;
	private Integer returnFlight;
	private int noOfSeats;
	private boolean status;
	public String getBookingId() {
		return bookingId;
	}

	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getPnr() {
		return pnr;
	}

	public void setPnr(String pnr) {
		this.pnr = pnr;
	}

	public String getUser() {
		return userId;
	}

	public void setUser(String user) {
		this.userId = user;
	}

	public Integer getFlight() {
		return flightId;
	}

	public void setFlight(Integer flight) {
		this.flightId = flight;
	}

	
	public int getNoOfSeats() {
		return noOfSeats;
	}

	public void setNoOfSeats(int noOfSeats) {
		this.noOfSeats = noOfSeats;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public List<PassengerEntity> getPassengers() {
		return passengers;
	}

	public void setPassengers(List<PassengerEntity> passengers) {
		this.passengers = passengers;
	}

	
	private List<PassengerEntity> passengers = new ArrayList<>();

     public void addPassenger(PassengerEntity passenger) {
        passengers.add(passenger);
    }

	 public void setBookingTime(LocalDateTime bookingTime) {
		 this.bookingTime = bookingTime;
	 }

	 public Integer getReturnFlight() {
		 return returnFlight;
	 }

	 public void setReturnFlight(Integer  returnFlight) {
		 this.returnFlight = returnFlight;
	 }

	 public String getUserId() {
		 return userId;
	 }

	 public void setUserId(String userId) {
		 this.userId = userId;
	 }

	 public Integer getFlightId() {
		 return flightId;
	 }

	 public void setFlightId(Integer flightId) {
		 this.flightId = flightId;
	 }

	 public LocalDateTime getBookingTime() {
		 return bookingTime;
	 }
}
