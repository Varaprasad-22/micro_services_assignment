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

		FlightDto outboundFlight = flightServiceClient.getFlightDetails(data.getOutboundFlightId())
				.orElseThrow(() -> new ResourceNotFoundException("Outbound flight not found"));

		if (outboundFlight.getAvailSeats() < data.getNoOfSeats()) {
			throw new BookingException("Not enough seats in outbound flight.");
		}

		

		BookingEntity bookingEntity = new BookingEntity();
		bookingEntity.setUser(userId);
		bookingEntity.setEmailId(data.getEmailId());
		bookingEntity.setFlight(data.getOutboundFlightId());
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

			if (returnFlight.getAvaliSeats() < data.getNoOfSeats()) {
				throw new BookingException("Not enough seats in return flight.");
			}

			bookingEntity.setReturnFlight(returnFlight);
			returnFlight.setAvaliSeats(returnFlight.getAvaliSeats() - data.getNoOfSeats());
			flightRepository.save(returnFlight);

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
		BookingGetResponse response = new BookingGetResponse();
		response.setPnr(bookingEntity.getPnr());
		response.setFlightId(String.valueOf(bookingEntity.getFlight().getFlightId()));
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
	@Transactional
	public String cancelTicket(String pnr) {
		// TODO Auto-generated method stub
		Optional<BookingEntity> bookingOpt = bookingRepository.findByPnr(pnr);
		if (!bookingOpt.isPresent()) {
			throw new ResourceNotFoundException("Cancellation Failed: PNR not found.");
		}
		BookingEntity bookingEntity = bookingOpt.get();
		LocalDateTime flightDepartureTime = bookingEntity.getFlight().getDepatureTime();
		LocalDateTime currentTime = LocalDateTime.now();
		long hoursRemaining = Duration.between(currentTime, flightDepartureTime).toHours();

		if (hoursRemaining < 24) {
			throw new BookingException(
					"Cancellation Failed: Cannot cancel ticket less than 24 hours before journey date.");
		}
		FlightEntity flightEntity = bookingEntity.getFlight();
		flightEntity.setAvaliSeats(flightEntity.getAvaliSeats() + bookingEntity.getNoOfSeats());
		flightRepository.save(flightEntity);
		bookingRepository.deleteByPnr(pnr);
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
				if (booking.getUser() != null) {
					bookingDto.setName(booking.getUser().getName());
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
