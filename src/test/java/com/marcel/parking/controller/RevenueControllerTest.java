package com.marcel.parking.controller;

import com.marcel.parking.dto.RevenueResponse;
import com.marcel.parking.service.RevenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RevenueController.class)
public class RevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevenueService revenueService;

    @Test
    @DisplayName("Deve consultar faturamento por setor e data")
    void shouldGetRevenueBySectorAndDate() throws Exception {
        when(revenueService.getRevenue(argThat(request ->
                request.sector().equals("A")
                        && request.date().equals(LocalDate.of(2026, 5, 13))
        ))).thenReturn(new RevenueResponse(
                new BigDecimal("50.00"),
                "BRL",
                Instant.parse("2026-05-13T15:00:00Z")
        ));

        mockMvc.perform(get("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-05-13",
                                  "sector": "A"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-13T15:00:00Z"));

        verify(revenueService).getRevenue(argThat(request ->
                request.sector().equals("A")
                        && request.date().equals(LocalDate.of(2026, 5, 13))
        ));
    }

    @Test
    @DisplayName("Deve retornar 400 quando date não for informado")
    void shouldReturnBadRequestWhenDateIsMissing() throws Exception {
        mockMvc.perform(get("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sector": "A"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(revenueService);
    }

    @Test
    @DisplayName("Deve retornar 400 quando sector estiver vazio")
    void shouldReturnBadRequestWhenSectorIsBlank() throws Exception {
        mockMvc.perform(get("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-05-13",
                                  "sector": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(revenueService);
    }
}
