package com.campus.library.service;

import com.campus.library.domain.Book;
import com.campus.library.domain.Branch;
import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.dto.catalog.*;
import com.campus.library.exception.BadRequestException;
import com.campus.library.exception.ResourceNotFoundException;
import com.campus.library.repository.BookRepository;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final BookRepository bookRepository;
    private final BranchRepository branchRepository;
    private final BranchCopyRepository branchCopyRepository;

    public CatalogService(BookRepository bookRepository,
                          BranchRepository branchRepository,
                          BranchCopyRepository branchCopyRepository) {
        this.bookRepository = bookRepository;
        this.branchRepository = branchRepository;
        this.branchCopyRepository = branchCopyRepository;
    }

    /**
     * FR-1.1 Unified Search Engine: Case-insensitive search supporting title, author, and ISBN with Pageable.
     */
    public Page<BookSummaryResponse> searchCatalog(String query, Pageable pageable) {
        Page<Book> booksPage;
        if (query == null || query.trim().isEmpty()) {
            booksPage = bookRepository.findAll(pageable);
        } else {
            booksPage = bookRepository.searchCatalog(query.trim(), pageable);
        }

        return booksPage.map(this::toSummaryResponse);
    }

    public BookSummaryResponse getBookSummary(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
        return toSummaryResponse(book);
    }

    /**
     * FR-1.2 Real-time Stock Matrix:
     * Available = sum(Copies with status = 'AVAILABLE') per individual campus branch.
     */
    public BookInventoryResponse getBookInventory(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        List<Branch> branches = branchRepository.findAll();
        List<BranchCopy> allCopies = branchCopyRepository.findByBookId(bookId);

        // Group copies by branch ID
        Map<Long, List<BranchCopy>> copiesByBranch = allCopies.stream()
                .collect(Collectors.groupingBy(c -> c.getCurrentBranch().getId()));

        List<BranchStockDto> branchBreakdown = new ArrayList<>();
        long totalCopies = allCopies.size();
        long totalAvailable = 0;

        for (Branch branch : branches) {
            List<BranchCopy> branchCopies = copiesByBranch.getOrDefault(branch.getId(), List.of());
            long available = branchCopies.stream().filter(c -> c.getStatus() == CopyStatus.AVAILABLE).count();
            long reserved = branchCopies.stream().filter(c -> c.getStatus() == CopyStatus.RESERVED).count();
            long inTransit = branchCopies.stream().filter(c -> c.getStatus() == CopyStatus.IN_TRANSIT).count();
            long readyPickup = branchCopies.stream().filter(c -> c.getStatus() == CopyStatus.READY_PICKUP).count();
            long issued = branchCopies.stream().filter(c -> c.getStatus() == CopyStatus.ISSUED).count();

            totalAvailable += available;
            branchBreakdown.add(new BranchStockDto(
                    branch.getId(),
                    branch.getCode(),
                    branch.getName(),
                    available,
                    reserved,
                    inTransit,
                    readyPickup,
                    issued,
                    branchCopies.size()
            ));
        }

        List<CopyDetailDto> copyDetails = allCopies.stream()
                .map(c -> new CopyDetailDto(
                        c.getId(),
                        c.getCurrentBranch().getId(),
                        c.getCurrentBranch().getCode(),
                        c.getCurrentBranch().getName(),
                        c.getBarcode(),
                        c.getShelfLocation(),
                        c.getStatus()
                ))
                .collect(Collectors.toList());

        return new BookInventoryResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getEdition(),
                totalCopies,
                totalAvailable,
                branchBreakdown,
                copyDetails
        );
    }

    public List<BranchDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(b -> new BranchDto(b.getId(), b.getCode(), b.getName(), b.getContactEmail()))
                .collect(Collectors.toList());
    }

    @Transactional
    public BranchDto createBranch(CreateBranchRequest request) {
        if (branchRepository.existsByCode(request.code())) {
            throw new BadRequestException("Branch code already registered: " + request.code());
        }
        Branch branch = branchRepository.save(new Branch(request.code(), request.name(), request.contactEmail()));
        return new BranchDto(branch.getId(), branch.getCode(), branch.getName(), branch.getContactEmail());
    }

    @Transactional
    public BookSummaryResponse createBook(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BadRequestException("ISBN already exists in catalog: " + request.isbn());
        }
        Book book = bookRepository.save(new Book(request.isbn(), request.title(), request.author(), request.edition()));
        return toSummaryResponse(book);
    }

    private BookSummaryResponse toSummaryResponse(Book book) {
        List<BranchCopy> copies = branchCopyRepository.findByBookId(book.getId());
        long available = copies.stream().filter(c -> c.getStatus() == CopyStatus.AVAILABLE).count();
        return new BookSummaryResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getEdition(),
                copies.size(),
                available
        );
    }
}
