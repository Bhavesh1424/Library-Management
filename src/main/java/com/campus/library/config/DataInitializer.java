package com.campus.library.config;

import com.campus.library.domain.Book;
import com.campus.library.domain.Branch;
import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.User;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.domain.enums.UserRole;
import com.campus.library.repository.BookRepository;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import com.campus.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds sample collegiate master data, departmental libraries, and distributed book copies.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BranchRepository branchRepository;
    private final BookRepository bookRepository;
    private final BranchCopyRepository branchCopyRepository;
    private final UserRepository userRepository;

    public DataInitializer(BranchRepository branchRepository,
                           BookRepository bookRepository,
                           BranchCopyRepository branchCopyRepository,
                           UserRepository userRepository) {
        this.branchRepository = branchRepository;
        this.bookRepository = bookRepository;
        this.branchCopyRepository = branchCopyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (branchRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial fixtures.");
            return;
        }

        log.info("Starting collegiate library exchange data seeding...");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        // 1. Branches
        Branch central = branchRepository.save(new Branch("CENTRAL", "Central University Library", "central.lib@campus.edu"));
        Branch cs = branchRepository.save(new Branch("CS-DEPT", "Computer Science Department Library", "cs.lib@campus.edu"));
        Branch mech = branchRepository.save(new Branch("MECH-DEPT", "Mechanical Engineering Department Library", "mech.lib@campus.edu"));

        // 2. Users (Student, Librarians, Admin)
        userRepository.save(new User("STU-2024-001", "Aarav Sharma", "student@campus.edu", encoder.encode("password123"), UserRole.ROLE_STUDENT));
        userRepository.save(new User("LIB-CS-001", "Priya Nair (CS Desk)", "cs.librarian@campus.edu", encoder.encode("password123"), UserRole.ROLE_LIBRARIAN));
        userRepository.save(new User("LIB-CEN-001", "Rajesh Kumar (Central Desk)", "central.librarian@campus.edu", encoder.encode("password123"), UserRole.ROLE_LIBRARIAN));
        userRepository.save(new User("ADM-001", "Chief Administrator", "admin@campus.edu", encoder.encode("admin123"), UserRole.ROLE_ADMIN));

        // 3. Master Books
        // Book 1: CLRS Algorithms (0 at Central, 2 at CS - Exact PRD scenario)
        Book clrs = bookRepository.save(new Book("9780262033848", "Introduction to Algorithms", "Thomas H. Cormen, Charles E. Leiserson", "3rd Edition"));
        branchCopyRepository.save(new BranchCopy(clrs, cs, "BC-CS-101", "Rack A1, Shelf 2"));
        branchCopyRepository.save(new BranchCopy(clrs, cs, "BC-CS-102", "Rack A1, Shelf 2"));

        // Book 2: Operating System Concepts (1 at Central, 1 at CS)
        Book os = bookRepository.save(new Book("9781118063330", "Operating System Concepts", "Abraham Silberschatz, Peter B. Galvin", "10th Edition"));
        branchCopyRepository.save(new BranchCopy(os, central, "BC-CEN-201", "Stack 3, Shelf B"));
        branchCopyRepository.save(new BranchCopy(os, cs, "BC-CS-202", "Rack B4, Shelf 1"));

        // Book 3: Database System Concepts (1 at CS, 1 at Mech)
        Book db = bookRepository.save(new Book("9780073523323", "Database System Concepts", "Abraham Silberschatz, Henry F. Korth", "7th Edition"));
        branchCopyRepository.save(new BranchCopy(db, cs, "BC-CS-301", "Rack C2, Shelf 3"));
        branchCopyRepository.save(new BranchCopy(db, mech, "BC-MECH-302", "Section E, Bay 1"));

        // Book 4: Fluid Mechanics (2 at Mech, 0 at Central)
        Book fluid = bookRepository.save(new Book("9780073380322", "Fluid Mechanics: Fundamentals & Applications", "Yunus A. Cengel, John M. Cimbala", "4th Edition"));
        branchCopyRepository.save(new BranchCopy(fluid, mech, "BC-MECH-401", "Bay M1, Shelf 4"));
        branchCopyRepository.save(new BranchCopy(fluid, mech, "BC-MECH-402", "Bay M1, Shelf 4"));

        // Book 5: Computer Networks (1 at CS, 1 at Central)
        Book networks = bookRepository.save(new Book("9780132126953", "Computer Networks", "Andrew S. Tanenbaum, David J. Wetherall", "5th Edition"));
        branchCopyRepository.save(new BranchCopy(networks, cs, "BC-CS-501", "Rack D1, Shelf 5"));
        branchCopyRepository.save(new BranchCopy(networks, central, "BC-CEN-502", "Stack 2, Shelf D"));

        log.info("Collegiate library exchange master data successfully seeded!");
    }
}
