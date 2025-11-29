package com.flightapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flightapp.dto.Flight;
import com.flightapp.dto.Search;
import com.flightapp.dto.SearchResult;
import com.flightapp.repository.FlightRepository;
import com.flightapp.service.FlightService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1.0/flights")
public class FlightController {
	
	@Autowired
	private FlightService flightService;

	@PostMapping("/airline/inventory/addFlight")
	public ResponseEntity<Integer> addFlight(@RequestBody Flight flightEntry){
		int flightId=flightService.addFlight(flightEntry);
		return ResponseEntity.status(HttpStatus.CREATED).body(flightId);
	}
	@PostMapping("/search")
	public SearchResult searchFlights(@Valid @RequestBody Search data) {
		return flightService.search(data);
	}
}
