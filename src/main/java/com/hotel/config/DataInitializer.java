package com.hotel.config;

import com.hotel.model.*;
import com.hotel.model.enums.AccountStatus;
import com.hotel.model.enums.RoomCategory;
import com.hotel.model.enums.RoomStatus;
import com.hotel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Initializes baseline entities, Stage 2 Admin accounts, geographic hierarchy,
 * and verified luxury hotels across Indian States and Union Territories.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;
    private final AdminRepository adminRepository;
    private final HotelRepository hotelRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final ExperienceRepository experienceRepository;
    private final IndiaHotelDataSeeder indiaHotelDataSeeder;
    private final CompleteDirectoryHotelSeeder completeDirectoryHotelSeeder;
    private final PasswordEncoder passwordEncoder;
    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;
    private final OfferRepository offerRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final InspectionRepository inspectionRepository;

    @Value("${app.security.admin.gagan.username:GAGAN}")
    private String gaganUsername;

    @Value("${app.security.admin.gagan.password:}")
    private String gaganPassword;

    @Value("${app.security.admin.darshan.username:DARSHAN}")
    private String darshanUsername;

    @Value("${app.security.admin.darshan.password:}")
    private String darshanPassword;

    @Autowired
    public DataInitializer(AmenityRepository amenityRepository,
                           RoomRepository roomRepository,
                           AdminRepository adminRepository,
                           HotelRepository hotelRepository,
                           CountryRepository countryRepository,
                           StateRepository stateRepository,
                           DistrictRepository districtRepository,
                           ExperienceRepository experienceRepository,
                           IndiaHotelDataSeeder indiaHotelDataSeeder,
                           @Autowired(required = false) CompleteDirectoryHotelSeeder completeDirectoryHotelSeeder,
                           PasswordEncoder passwordEncoder,
                           GuestRepository guestRepository,
                           StaffRepository staffRepository,
                           OfferRepository offerRepository,
                           MaintenanceRecordRepository maintenanceRecordRepository,
                           InspectionRepository inspectionRepository) {
        this.amenityRepository = amenityRepository;
        this.roomRepository = roomRepository;
        this.adminRepository = adminRepository;
        this.hotelRepository = hotelRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.experienceRepository = experienceRepository;
        this.indiaHotelDataSeeder = indiaHotelDataSeeder;
        this.completeDirectoryHotelSeeder = completeDirectoryHotelSeeder;
        this.passwordEncoder = passwordEncoder;
        this.guestRepository = guestRepository;
        this.staffRepository = staffRepository;
        this.offerRepository = offerRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    public void run(String... args) {
        log.info(">>> Initializing Stage 2 Security, Database Structure, and India Luxury Hotels... <<<");

        // 1. Initialize Baseline Amenities and Rooms if not present
        if (roomRepository.count() == 0) {
            log.info("Seeding foundational hotel rooms and amenities...");
            Amenity wifi = amenityRepository.save(new Amenity("High-Speed Fiber Wi-Fi", "Dedicated 1Gbps wireless network", "fas fa-wifi", 0.0));
            Amenity jacuzzi = amenityRepository.save(new Amenity("Private Balcony Jacuzzi", "Heated hydrotherapy whirlpool spa", "fas fa-hot-tub", 1200.0));
            Amenity butler = amenityRepository.save(new Amenity("24/7 Dedicated Butler Service", "Personalized VIP concierge & butler", "fas fa-concierge-bell", 2500.0));
            Amenity oceanView = amenityRepository.save(new Amenity("Panoramic Ocean View", "Full floor-to-ceiling sea horizon view", "fas fa-water", 1000.0));
            Amenity minibar = amenityRepository.save(new Amenity("Artisanal Mini Bar", "Curated beverages and gourmet snacks", "fas fa-glass-cheers", 500.0));

            Room r101 = new Room("101", RoomCategory.DELUXE, RoomStatus.AVAILABLE, 4500.00, 1, 2,
                    "Elegant deluxe room featuring king size bed, ergonomic work desk, and rain shower.",
                    "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
            r101.setAmenities(Set.of(wifi, minibar));
            roomRepository.save(r101);

            Room r201 = new Room("201", RoomCategory.DELUXE, RoomStatus.OCCUPIED, 4800.00, 2, 2,
                    "Deluxe garden-facing room with private sitting balcony.",
                    "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80");
            r201.setAmenities(Set.of(wifi, minibar));
            roomRepository.save(r201);

            Room r301 = new Room("301", RoomCategory.SUITE, RoomStatus.AVAILABLE, 8500.00, 3, 4,
                    "Executive suite with master bedroom, separate lounge, marble bath, and sea-view balcony.",
                    "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
            r301.setAmenities(Set.of(wifi, jacuzzi, oceanView, minibar));
            roomRepository.save(r301);

            Room r401 = new Room("PH-01", RoomCategory.PRESIDENTIAL, RoomStatus.AVAILABLE, 18000.00, 4, 6,
                    "Top-tier Presidential Penthouse with panoramic ocean view, private butler, and infinity jacuzzi.",
                    "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80");
            r401.setAmenities(Set.of(wifi, jacuzzi, butler, oceanView, minibar));
            roomRepository.save(r401);
        }

        // 2. Initialize exactly TWO initial Admin accounts: GAGAN and DARSHAN
        seedAdminAccount(gaganUsername, gaganPassword);
        seedAdminAccount(darshanUsername, darshanPassword);

        // Remove legacy mock admin "admin" if present from Stage 1
        Optional<Admin> legacyAdmin = adminRepository.findByUsernameIgnoreCase("admin");
        legacyAdmin.ifPresent(admin -> {
            adminRepository.delete(admin);
            log.info("Removed legacy Stage 1 default admin account.");
        });

        // 3. Initialize Location Hierarchy and Verified India Hotels
        indiaHotelDataSeeder.seedHierarchy(countryRepository, stateRepository, districtRepository, experienceRepository);
        indiaHotelDataSeeder.seedAllHotels(hotelRepository);
        if (completeDirectoryHotelSeeder != null) {
            completeDirectoryHotelSeeder.seedCompleteDirectory(hotelRepository);
        }

        // 4. Initialize foundational Guest account if not present
        if (guestRepository.count() == 0) {
            Guest demoGuest = new Guest("guest", passwordEncoder.encode("Guest@123"), "Alex Morgan", "alex.guest@royalindianstays.com");
            demoGuest.setStatus(AccountStatus.ACTIVE);
            demoGuest.setActive(true);
            guestRepository.save(demoGuest);
            log.info("Provisioned foundational demo Guest account: guest / Guest@123");
        }

        // 5. Initialize foundational Staff accounts if not present
        if (staffRepository.count() == 0) {
            Staff s1 = new Staff("staff", passwordEncoder.encode("password"), "Rajesh Kumar", "rajesh.staff@royalindianstays.com", "9876543210", "STF-1001", "Front Desk", "Operations Lead", "Morning (07:00 - 15:30)");
            s1.setActivityStatus("Working");
            staffRepository.save(s1);

            Staff s2 = new Staff("priya_sharma", passwordEncoder.encode("password"), "Priya Sharma", "priya.staff@royalindianstays.com", "9876543211", "STF-1002", "Guest Relations", "Senior Concierge", "Morning (07:00 - 15:30)");
            s2.setActivityStatus("Working");
            staffRepository.save(s2);

            Staff s3 = new Staff("rahul_verma", passwordEncoder.encode("password"), "Rahul Verma", "rahul.staff@royalindianstays.com", "9876543212", "STF-1003", "Housekeeping", "Floor Supervisor", "Evening (15:00 - 23:30)");
            s3.setActivityStatus("Working");
            staffRepository.save(s3);

            Staff s4 = new Staff("arjun_nair", passwordEncoder.encode("password"), "Arjun Nair", "arjun.staff@royalindianstays.com", "9876543213", "STF-1004", "Engineering", "Maintenance Specialist", "Night (23:00 - 07:30)");
            s4.setActivityStatus("Not Working");
            staffRepository.save(s4);

            Staff s5 = new Staff("ananya_sen", passwordEncoder.encode("password"), "Ananya Sen", "ananya.staff@royalindianstays.com", "9876543214", "STF-1005", "Culinary", "Sous Chef", "General (09:00 - 18:00)");
            s5.setActivityStatus("On Leave");
            staffRepository.save(s5);
            log.info("Provisioned foundational Staff accounts across operational departments.");
        }

        // 6. Initialize foundational Luxury Offers if not present
        if (offerRepository.count() == 0) {
            offerRepository.save(new Offer(
                    "Kodagu & Western Ghats Serenade",
                    "Karnataka Season",
                    "Valid across Coorg & Chikkamagaluru resorts",
                    "25% tariff reduction on 3+ night bookings\nComplimentary coffee estate plantation walking tour\nDaily artisanal buffet breakfast & high tea\n20% discount on Ayurvedic spa treatments",
                    "GH-COORG25",
                    25.0,
                    "Kodagu",
                    "/hotels?district=Kodagu"
            ));
            offerRepository.save(new Offer(
                    "Royal Mysuru & Hampi Heritage",
                    "Heritage Escape",
                    "Immerse in royal palaces & UNESCO monuments",
                    "Complimentary palace heritage tour chaperone\nRoyal candlelit private dinner for two\nGuaranteed late checkout until 3:00 PM\nWelcome basket with Mysore Pak & royal fragrances",
                    "GH-HERITAGE",
                    20.0,
                    "Mysuru",
                    "/hotels?district=Mysuru"
            ));
            offerRepository.save(new Offer(
                    "Bengaluru Executive & Weekend Haven",
                    "Metropolitan Retreat",
                    "City center opulence with executive club privileges",
                    "Complimentary upgrade to Executive Suite (subject to availability)\nAccess to exclusive 24th-floor Executive Club Lounge\nAirport luxury sedan transfers included\nHigh-speed fiber connectivity & private meeting room access",
                    "GH-BLRDEAL",
                    15.0,
                    "Bengaluru Urban",
                    "/hotels?district=Bengaluru+Urban"
            ));
            offerRepository.save(new Offer(
                    "Coastal Karavali & Western Sunset Escape",
                    "Coastal Getaway",
                    "Unwind alongside pristine Arabian Sea shorelines",
                    "Exclusive private beach cabana for sunset viewing\nComplimentary coastal seafood barbeque dining experience\nWater sports voucher (Jet ski / Parasailing)\nSignature coconut oil Ayurvedic head massage",
                    "GH-COASTAL",
                    30.0,
                    "Dakshina Kannada",
                    "/hotels?district=Dakshina+Kannada"
            ));
            offerRepository.save(new Offer(
                    "Chikkamagaluru Coffee Estate Haven",
                    "Hill Station Special",
                    "Misty peaks, artisanal Arabica aromas, and luxury bungalows",
                    "Private guided trek to Mullayanagiri peak\nArtisanal coffee tasting masterclass with resident planter\nBonfire night with local Malnad regional feast\nComplimentary handcrafted coffee beans gift box",
                    "GH-CHIKK20",
                    20.0,
                    "Chikkamagaluru",
                    "/hotels?district=Chikkamagaluru"
            ));
            offerRepository.save(new Offer(
                    "Hassan & Belur Architectural Pilgrimage",
                    "Temple Trail",
                    "Marvel at Hoysala stone carvings and heritage sanctuaries",
                    "Curated guided excursion to Belur and Halebidu temples\nTraditional South Indian thali feast served on banana leaf\nEarly morning temple chariot procession priority viewing\nComplimentary hand-carved stone memento",
                    "GH-HOYSALA",
                    18.0,
                    "Hassan",
                    "/hotels?district=Hassan"
            ));
            log.info("Provisioned verified Royal Indian Stays promotional offers.");
        }

        // 7. Initialize Maintenance Records and Property Inspections if not present
        List<Hotel> sampleHotels = hotelRepository.findAll();
        if (!sampleHotels.isEmpty()) {
            if (maintenanceRecordRepository.count() == 0) {
                Hotel h1 = sampleHotels.get(0);
                Hotel h2 = sampleHotels.size() > 1 ? sampleHotels.get(1) : h1;
                Hotel h3 = sampleHotels.size() > 2 ? sampleHotels.get(2) : h1;

                maintenanceRecordRepository.save(new MaintenanceRecord(h1, "Suite 302", "HVAC Cooling Servicing", "IN_PROGRESS", LocalDate.now().minusDays(1), "Arjun Nair", "Seasonal compressor lubrication and filter change"));
                maintenanceRecordRepository.save(new MaintenanceRecord(h1, "Presidential Penthouse", "Jacuzzi Jet Filter Replacement", "SCHEDULED", LocalDate.now().plusDays(1), "Arjun Nair", "Hydrotherapy pump pressure calibration"));
                maintenanceRecordRepository.save(new MaintenanceRecord(h2, "Room 104", "Smart Keyless Lock Firmware", "IN_PROGRESS", LocalDate.now(), "Vikram Tech", "NFC lock firmware synchronization with central desk"));
                maintenanceRecordRepository.save(new MaintenanceRecord(h3, "Grand Ballroom", "Chandelier LED Array Rewiring", "COMPLETED", LocalDate.now().minusDays(3), "Southern Electric Co.", "Restoration of vintage crystal lighting"));
                log.info("Provisioned verified property maintenance records.");
            }

            if (inspectionRepository.count() == 0) {
                Hotel h1 = sampleHotels.get(0);
                Hotel h2 = sampleHotels.size() > 1 ? sampleHotels.get(1) : h1;
                Hotel h3 = sampleHotels.size() > 2 ? sampleHotels.get(2) : h1;

                inspectionRepository.save(new Inspection(h1, "Entire Property", "Priya Sharma", LocalDate.now().minusDays(2), "COMPLETED", "98%", "Excellent luxury compliance, spotless gardens, and impeccable linen service."));
                inspectionRepository.save(new Inspection(h1, "Kitchen & Culinary Wing", "Chef Ananya Sen", LocalDate.now().minusDays(1), "INSPECTED", "96%", "HACCP hygiene audit passed with zero violations. Cold storage temperatures optimal."));
                inspectionRepository.save(new Inspection(h2, "Pool & Spa Facilities", "Rajesh Kumar", LocalDate.now().plusDays(2), "DUE", "Pending", "Scheduled bi-weekly hydro-chemistry and wellness sanitation inspection."));
                inspectionRepository.save(new Inspection(h3, "Fire & Life Safety Systems", "Chief Safety Officer", LocalDate.now().plusDays(4), "PENDING", "Pending", "Annual smoke sensor and automated sprinkler pressure verification."));
                log.info("Provisioned verified operational inspections.");
            }
        }

        log.info(">>> Stage 2 Database Initialization Complete. Authorized Admins: {}, {} <<<",
                gaganUsername, darshanUsername);
    }

    private void seedAdminAccount(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            log.warn("Admin username or password configuration missing for initialization.");
            return;
        }

        String cleanUsername = username.trim().toUpperCase();
        Optional<Admin> existing = adminRepository.findByUsernameIgnoreCase(cleanUsername);

        if (existing.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(rawPassword.trim());
            Admin admin = new Admin(cleanUsername, encodedPassword);
            admin.setStatus(AccountStatus.ACTIVE);
            admin.setActive(true);
            adminRepository.save(admin);
            log.info("Successfully provisioned initial Admin account: {}", cleanUsername);
        } else {
            Admin admin = existing.get();
            if (!passwordEncoder.matches(rawPassword.trim(), admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(rawPassword.trim()));
                admin.setStatus(AccountStatus.ACTIVE);
                admin.setActive(true);
                adminRepository.save(admin);
                log.info("Updated password hash for initial Admin account: {}", cleanUsername);
            }
        }
    }
}
