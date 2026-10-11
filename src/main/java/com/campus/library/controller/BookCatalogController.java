package com.campus.library.controller;

import com.campus.library.dto.catalog.*;
import com.campus.library.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog & Stock Discovery", description = "Endpoints for searching books across all campus branches and viewing real-time branch inventory")
public class BookCatalogController {

    private final CatalogService catalogService;

    public BookCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * FR-1.1 Unified Search Engine: Case-insensitive search supporting title, author, and ISBN with paginated JPA queries (Pageable).
     */
    @GetMapping("/books")
    @Operation(summary = "Search catalog", description = "Paginated case-insensitive search by title, author, or ISBN across all campus libraries")
    public ResponseEntity<Page<BookSummaryResponse>> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BookSummaryResponse> response = catalogService.searchCatalog(query, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/books/{id}")
    @Operation(summary = "Get book summary", description = "Retrieves master bibliographic definition and copy metrics")
    public ResponseEntity<BookSummaryResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getBookSummary(id));
    }

    /**
     * FR-1.2 Real-time Stock Matrix: Real-time available copy breakdown per individual branch.
     */
    @GetMapping("/books/{id}/inventory")
    @Operation(summary = "Get branch inventory matrix", description = "Breakdown of available, reserved, in-transit, and ready copies across Central, CS, Mech, etc.")
    public ResponseEntity<BookInventoryResponse> getBookInventory(@PathVariable Long id) {
        BookInventoryResponse inventory = catalogService.getBookInventory(id);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/books")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create master book", description = "Admin-only endpoint to define a new book with unique 13-digit ISBN")
    public ResponseEntity<BookSummaryResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookSummaryResponse created = catalogService.createBook(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * FR-1.3 Branch Master Directory
     */
    @GetMapping("/branches")
    @Operation(summary = "List campus branches", description = "Retrieves directory of all campus libraries (Central, CS-DEPT, MECH-DEPT, etc.)")
    public ResponseEntity<List<BranchDto>> getBranches() {
        return ResponseEntity.ok(catalogService.getAllBranches());
    }

    @PostMapping("/branches")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Register new campus branch", description = "Admin-only facility directory registration")
    public ResponseEntity<BranchDto> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        BranchDto created = catalogService.createBranch(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
