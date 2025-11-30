package com.bookingapp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookingapp.client.FlightServiceClient;
import com.bookingapp.dto.BookingGetResponse; 
import com.bookingapp.dto.Bookingdto;
import com.bookingapp.dto.FlightDto;
import com.bookingapp.dto.Passengers;
import com.bookingapp.exceptions.BookingException;
import com.bookingapp.exceptions.ResourceNotFoundException;
import com.bookingapp.model.BookingEntity;
import com.bookingapp.model.PassengerEntity;
import com.bookingapp.model.User;
import com.bookingapp.repository.BookingRepository;
import com.bookingapp.repository.UserRepository;


@Service
public class BookingServiceImpl implements BookingService {

	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private FlightServiceClient flightserviceclient;

	@Override
	public String bookFlight(Bookingdto data) {
		
		User user = getOrCreateUser(data.getEmailId(), data.getName());
		String userId=user.getUserId();
		
		
		int outboundId = data.getOutboundFlightId();

		FlightDto outboundFlight = flightserviceclient.getFlightDetails(data.getOutboundFlightId())
				.orElseThrow(() -> new ResourceNotFoundException("Outbound flight not found"));

		if (outboundFlight.getTotalSeats() < data.getNoOfSeats()) {
			throw new BookingException("Not enough seats in outbound flight.");
		}

		

		BookingEntity bookingEntity = new BookingEntity();
		bookingEntity.setUserId(userId);
		bookingEntity.setEmailId(data.getEmailId());
		bookingEntity.setFlightId(data.getOutboundFlightId());
		bookingEntity.setNoOfSeats(data.getNoOfSeats());
		bookingEntity.setStatus(true);
		bookingEntity.setBookingTime(LocalDateTime.now());
		bookingEntity.setPnr(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		data.getPassengers().forEach(passengerRequest -> {
			PassengerEntity passengerEntity = new PassengerEntity();
			passengerEntity.setName(passengerRequest.getName());
			passengerEntity.setAge(passengerRequest.getAge());
			passengerEntity.setGender(passengerRequest.getGender());
			passengerEntity.setMeal(passengerRequest.getMeal());
			passengerEntity.setSeatNo(passengerRequest.getSeatNo());
			bookingEntity.addPassenger(passengerEntity);
		});

		flightserviceclient.updateSeats(data.getOutboundFlightId(),data.getNoOfSeats()*-1);
		
		Integer returnId = data.getReturnFlightId();
		if (returnId != null) {

			FlightDto returnFlight = flightserviceclient.getFlightDetails(returnId)
					.orElseThrow(() -> new ResourceNotFoundException("Return flight not found"));

			if (returnFlight.getTotalSeats() < data.getNoOfSeats()) {

				flightserviceclient.updateSeats(data.getOutboundFlightId(),data.getNoOfSeats());
				throw new BookingException("Not enough seats in return flight.");
			}

			bookingEntity.setReturnFlight(returnId);
			flightserviceclient.updateSeats(data.getOutboundFlightId(),data.getNoOfSeats()*-1);
			
			bookingRepository.save(bookingEntity);

			return "Round-trip Booking Successful! PNR: " + bookingEntity.getPnr();
		}

		bookingRepository.save(bookingEntity);

		return "One-way Booking Successful! PNR: " + bookingEntity.getPnr();
	}

	private User getOrCreateUser(String email, String name) {
		Optional<User> userOpt = userRepository.findByEmail(email);

		if (userOpt.isPresent()) {
			return userOpt.get();
		} else {
			// Create a new User
			User newUser = new User();
			newUser.setEmail(email);
			newUser.setName(name);
			return userRepository.save(newUser);
		}
	}

	@Override
	public BookingGetResponse getBookingDetails(String pnr) {
		// TODO Auto-generated method stub
		Optional<BookingEntity> bookingOpt = bookingRepository.findByPnr(pnr);
		if (!bookingOpt.isPresent()) {
			throw new ResourceNotFoundException("No booking found with PNR: " + pnr);
		}

		BookingEntity bookingEntity = bookingOpt.get();
		FlightDto flightDetails = flightserviceclient.getFlightDetails(
			    (bookingEntity.getFlightId())
			).orElse(null);
		BookingGetResponse response = new BookingGetResponse();
		response.setPnr(bookingEntity.getPnr());
		response.setFlightId(String.valueOf(bookingEntity.getFlightId()));
		List<Passengers> passengersList = bookingEntity.getPassengers().stream().map(entity -> {
			Passengers passengerDto = new Passengers();
			passengerDto.setName(entity.getName());
			passengerDto.setAge(entity.getAge());
			passengerDto.setGender(entity.getGender());
			passengerDto.setMeal(entity.getMeal());
			passengerDto.setSeatNo(entity.getSeatNo());
			return passengerDto;
		}).collect(Collectors.toList());

		response.setPassengersList(passengersList);
		return response;
	}

	@Override
	public String cancelTicket(String pnr) {
		// TODO Auto-generated method stub
		Optional<BookingEntity> bookingOpt = bookingRepository.findByPnr(pnr);
		if (!bookingOpt.isPresent()) {
			throw new ResourceNotFoundException("Cancellation Failed: PNR not found.");
		}
		BookingEntity bookingEntity = bookingOpt.get();
		LocalDateTime flightDepartureTime = bookingEntity.getBookingTime();
		LocalDateTime currentTime = LocalDateTime.now();
		long hoursRemaining = Duration.between(currentTime, flightDepartureTime).toHours();

		if (hoursRemaining < 24) {
			throw new BookingException(
					"Cancellation Failed: Cannot cancel ticket less than 24 hours before journey date.");
		}
//		FlightDto flightEntity = bookingEntity.getFlight();
		flightserviceclient.updateSeats(bookingEntity.getFlightId(),bookingEntity.getNoOfSeats());
		bookingEntity.setStatus(false);
		bookingRepository.save(bookingEntity);
		return "Ticket with PNR " + pnr + " successfully cancelled.";
	}

	@Override
	public List<Bookingdto> getHistoryByEmail(String emailId) {
		// TODO Auto-generated method stub
		List<Bookingdto> bookingData = new ArrayList<>();
		try {
			List<BookingEntity> bookingEntityList = bookingRepository.findAllByEmailId(emailId);
			if (bookingEntityList.isEmpty()) {
				throw new ResourceNotFoundException("No booking history found for email: " + emailId);
			}
			for (BookingEntity booking : bookingEntityList) {

				Bookingdto bookingDto = new Bookingdto();
				bookingDto.setEmailId(booking.getEmailId());
				bookingDto.setNoOfSeats(booking.getNoOfSeats());
				if (booking.getUserId() != null) {
					User user=new User();
					Optional<User> name=userRepository.findById(booking.getUserId());
					
					bookingDto.setName(name.get().getName());
				}
				List<Passengers> passengerDtoList = new ArrayList<>();
				for (PassengerEntity passengerEntity : booking.getPassengers()) {

					Passengers passengerDto = new Passengers();

					passengerDto.setName(passengerEntity.getName());
					passengerDto.setAge(passengerEntity.getAge());
					passengerDto.setGender(passengerEntity.getGender());
					passengerDto.setMeal(passengerEntity.getMeal());

					passengerDto.setSeatNo(String.valueOf(passengerEntity.getSeatNo()));

					passengerDtoList.add(passengerDto);
				}
				bookingDto.setPassengers(passengerDtoList);
				bookingData.add(bookingDto);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bookingData;
	}

}
