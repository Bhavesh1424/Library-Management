package com.campus.library.controller;

import com.campus.library.domain.Book;
import com.campus.library.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BookCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("GET /api/v1/books returns paginated master catalog")
    void testGetAllBooksPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/books?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0));
    }

    @Test
    @DisplayName("FR-1.1: Case-insensitive search by Title, Author, or ISBN")
    void testSearchBooks() throws Exception {
        // 1. Search by title "algorithms" (case-insensitive)
        mockMvc.perform(get("/api/v1/books?query=algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Introduction to Algorithms"));

        // 2. Search by author "Silberschatz" (matches OS and DB books)
        mockMvc.perform(get("/api/v1/books?query=Silberschatz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // 3. Search by ISBN "9780262033848"
        mockMvc.perform(get("/api/v1/books?query=9780262033848"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isbn").value("9780262033848"));
    }

    @Test
    @DisplayName("FR-1.2: Real-time Stock Matrix breakdown across Central and Departmental branches")
    void testGetBookInventoryMatrix() throws Exception {
        Book clrs = bookRepository.findByIsbn("9780262033848").orElseThrow();

        mockMvc.perform(get("/api/v1/books/" + clrs.getId() + "/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(clrs.getId()))
                .andExpect(jsonPath("$.title").value("Introduction to Algorithms"))
                .andExpect(jsonPath("$.totalAvailable").value(2))
                .andExpect(jsonPath("$.branchBreakdown", hasSize(3)))
                // Verify CENTRAL has 0 available
                .andExpect(jsonPath("$.branchBreakdown[?(@.branchCode == 'CENTRAL')].available").value(hasItem(0)))
                // Verify CS-DEPT has 2 available
                .andExpect(jsonPath("$.branchBreakdown[?(@.branchCode == 'CS-DEPT')].available").value(hasItem(2)))
                // Verify individual barcoded copies list
                .andExpect(jsonPath("$.availableCopies", hasSize(2)))
                .andExpect(jsonPath("$.availableCopies[0].barcode").value("BC-CS-101"));
    }

    @Test
    @DisplayName("FR-1.3: List campus branch directory")
    void testGetBranchesDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("CENTRAL", "CS-DEPT", "MECH-DEPT")));
    }
}
