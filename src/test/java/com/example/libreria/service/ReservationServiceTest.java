package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.dto.UserResponseDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookService bookService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private ReservationService reservationService;
    
    private User testUser;
    private Book testBook;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        
        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateReservation_Success() {
        ReservationRequestDTO requestDTO = new ReservationRequestDTO(
                1L,
                258027L,
                7,
                LocalDate.now()
        );

        UserResponseDTO dto = userService.getUserById(requestDTO.getUserId());
        if (dto == null) {
            throw new RuntimeException("El usuario no existe");
        }

        when(userService.getUserById(1L)).thenReturn(dto);
        when(bookRepository.findById(258027L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        ReservationResponseDTO result = reservationService.createReservation(requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("The Lord of the Rings", result.getBookTitle());
        assertEquals(new BigDecimal("15.99"), result.getDailyRate());
        assertEquals(new BigDecimal("111.93"), result.getTotalFee());
        assertEquals(Reservation.ReservationStatus.ACTIVE, result.getStatus());

        assertEquals(4, testBook.getAvailableQuantity());
    }


    @Test
    void testCreateReservation_BookNotAvailable() {
        testBook.setAvailableQuantity(0);

        ReservationRequestDTO requestDTO = new ReservationRequestDTO(
                1L,
                258027L,
                7,
                LocalDate.now()
        );

        UserResponseDTO userDTO = new UserResponseDTO(
                1L,
                "Juan Pérez",
                "juan@example.com",
                null,
                LocalDateTime.now()
        );

        when(userService.getUserById(1L)).thenReturn(userDTO);
        when(bookRepository.findById(258027L)).thenReturn(Optional.of(testBook));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(requestDTO);
        });

        assertTrue(ex.getMessage().contains("El libro está agotado"));
    }


    @Test
    void testReturnBook_OnTime() {
        LocalDate today = LocalDate.now();
        testReservation.setStartDate(today.minusDays(5));
        testReservation.setExpectedReturnDate(today);
        testReservation.setActualReturnDate(null);
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnBookRequestDTO returnDTO = new ReturnBookRequestDTO(today);

        ReservationResponseDTO result = reservationService.returnBook(1L, returnDTO);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getLateFee());

        assertEquals(6, testBook.getAvailableQuantity());
    }
  
    @Test
    void testReturnBook_Overdue() {
        LocalDate today = LocalDate.now();
        LocalDate expected = today.minusDays(3);

        testReservation.setStartDate(expected.minusDays(7));
        testReservation.setExpectedReturnDate(expected);
        testReservation.setActualReturnDate(null);
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnBookRequestDTO returnDTO = new ReturnBookRequestDTO(today);

        ReservationResponseDTO result = reservationService.returnBook(1L, returnDTO);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.OVERDUE, result.getStatus());

        assertEquals(new BigDecimal("7.20"), result.getLateFee().setScale(2, RoundingMode.HALF_UP));

        assertEquals(6, testBook.getAvailableQuantity());
    }

    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        ReservationResponseDTO result = reservationService.getReservationById(1L);
        
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }
    
    // @Test
    // void testGetAllReservations() {
    //     Reservation reservation2 = new Reservation();
    //     reservation2.setId(2L);
        
    //     when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
        
    //     List<ReservationResponseDTO> result = reservationService.getAllReservations();
        
    //     assertNotNull(result);
    //     assertEquals(2, result.size());
    // }
    
    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));
       
        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);
       
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getActiveReservations();
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

