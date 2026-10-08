
# Luxury Hotel Dynamic Tariff Optimizer & Guest Preference Matcher

**Project ID:** P68  
**Academic Context:** B.Tech Computer Science & Engineering (Object-Oriented Programming in Java)  
**Stage:** Stage 1 — Project Foundation & Architecture  

---

## 1. Project Objective
The objective of this project is to develop an enterprise-grade, object-oriented hotel management web system built purely in Java using Spring Boot and PostgreSQL. The system manages luxury accommodations and guests, matches guest preferences to optimal suites, and dynamically calculates room tariffs based on real-time hotel occupancy rates.

Stage 1 establishes the complete foundation, domain entity models, OOP class hierarchies, repository layers, service interfaces, database connections, and responsive Thymeleaf UI portal layouts for all three access levels.

---

## 2. Problem Statement
Traditional luxury hotel management systems frequently suffer from two critical inefficiencies:
1. **Static Pricing Inefficiencies:** Hotels often maintain rigid seasonal tariffs that fail to adapt to live inventory occupancy and sudden surges or dips in demand, resulting in sub-optimal revenue yields.
2. **Generic Room Allocation:** Guests have specific personalized preferences (bed configuration, high floors, quiet corners, premium amenities like jacuzzis or panoramic ocean views), yet standard reservation systems often allocate rooms mechanically without intelligent preference alignment.

This project solves both challenges through an Object-Oriented software architecture incorporating a **Dynamic Tariff Optimizer** and a **Guest Preference Matcher Engine**.

---

## 3. Main Users & Three Access Levels

The system is designed around three distinct user roles (`UserRole` enum) with tailored portals:

| User Role | Description | Dedicated Portal Navigation |
|---|---|---|
| **1. Guest** | Hotel customer who books luxury rooms, manages personal profile, and sets stay preferences. | **Dashboard**, **Search Rooms**, **My Bookings**, **Profile & Preferences** |
| **2. Staff** | Hotel front desk and housekeeping personnel managing daily reservations and room turnover. | **Dashboard**, **Bookings**, **Guests**, **Room Status (Live Housekeeping Board)** |
| **3. Admin** | Hotel executives and revenue managers auditing inventory, staffing, tariffs, and revenue metrics. | **Dashboard**, **Rooms**, **Bookings**, **Staff**, **Reports & Dynamic Tariff Analytics** |

---

## 4. Technology Stack

* **Primary Programming Language:** Java 17+ LTS (Tested & verified on OpenJDK 25 / 17 LTS)
* **Backend Framework:** Spring Boot 3.3.4 (Spring MVC, Spring Data JPA, Hibernate ORM)
* **Build Tool:** Apache Maven
* **Database:** PostgreSQL (Primary Production Database) with HikariCP connection pooling
* **Server-Side Rendering (UI):** Thymeleaf Template Engine
* **Frontend Design:** Semantic HTML5, Vanilla CSS3 (Custom Luxury Design System with Gold & Midnight Navy accents, Cinzel & Outfit typography), Font Awesome 6 icons
* **JavaScript:** Kept strictly minimal as per academic guidelines (no React, Node.js, Angular, or external client frameworks)

---

## 5. Core Java OOP Concepts Demonstrated

This project is explicitly structured to demonstrate key Object-Oriented Programming (OOP) concepts for academic viva:

1. **Classes and Objects:**
   * Pure domain modeling across entities: `User`, `Guest`, `Staff`, `Admin`, `Room`, `Booking`, `Amenity`, and `GuestPreference`.

2. **Encapsulation:**
   * All entity state attributes are strictly declared `private`.
   * Access and mutations are governed through standardized getters and setters.
   * State consistency checks and domain methods (e.g., `calculateTotalNights()`, `isAvailable()`).

3. **Inheritance:**
   * A clean class hierarchy where `User` acts as the base entity.
   * `Guest`, `Staff`, and `Admin` extend `User` (`public class Guest extends User`).
   * Relational mapping is executed via JPA `@Inheritance(strategy = InheritanceType.JOINED)` with `@PrimaryKeyJoinColumn`.

4. **Polymorphism & Abstraction via Interfaces:**
   * Core services are decoupled through Java interfaces (`GuestService`, `StaffService`, `AdminService`, `RoomService`, `BookingService`).
   * Concrete implementations (`RoomServiceImpl`, etc.) demonstrate interface polymorphism and dependency injection (`@Autowired`).

5. **Constructor Overloading:**
   * Multiple constructors in entity classes:
     - No-argument default constructors (JPA requirement).
     - Parameterized minimal constructors.
     - Overloaded full-argument constructors chaining via `this(...)` and `super(...)`.

6. **Strongly-Typed Enums with Fields & Methods:**
   * `UserRole`: `GUEST`, `STAFF`, `ADMIN` with display names.
   * `RoomCategory`: `DELUXE`, `SUITE`, `PRESIDENTIAL` with base prices and descriptions.
   * `RoomStatus`: `AVAILABLE`, `OCCUPIED`, `RESERVED`, `CLEANING`, `MAINTENANCE`, `OUT_OF_SERVICE` with bookable flags and UI badge classes.
   * `BookingStatus`: `PENDING`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`.

7. **Exception Handling:**
   * Custom unchecked business exception hierarchy: `RoomUnavailableException`, `BookingException`, `UserNotFoundException`.
   * Centralized `@ControllerAdvice` global handler.

---

## 6. Project Folder Structure

```text
luxury-hotel-management/
├── pom.xml                                     # Maven build and dependency configuration
├── .gitignore                                  # Git ignore rules for Java, Maven, IDEs
├── README.md                                   # Comprehensive project documentation
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── hotel/
        │           ├── HotelManagementApplication.java      # Spring Boot application bootstrap
        │           ├── config/
        │           │   ├── DatabaseConfig.java              # PostgreSQL DataSource configuration & fallback
        │           │   └── DataInitializer.java            # Seed data loader for rooms, users & bookings
        │           ├── controller/
        │           │   ├── HomeController.java              # Landing & sign-in page routing
        │           │   ├── GuestController.java             # Guest portal controller
        │           │   ├── StaffController.java             # Staff operational controller
        │           │   ├── AdminController.java             # Admin dashboard controller
        │           │   ├── RoomController.java              # Room inventory endpoints
        │           │   └── BookingController.java           # Reservation endpoints
        │           ├── dto/
        │           │   ├── RoomSearchDto.java               # Room search criteria DTO
        │           │   ├── BookingRequestDto.java           # Reservation submission DTO
        │           │   ├── GuestRegistrationDto.java        # Guest signup DTO
        │           │   └── UserLoginDto.java                # Authentication credentials DTO
        │           ├── exception/
        │           │   ├── RoomUnavailableException.java    # Room booking status exception
        │           │   ├── BookingException.java            # General booking validation exception
        │           │   ├── UserNotFoundException.java       # User lookup exception
        │           │   └── GlobalExceptionHandler.java      # Application-wide error handler
        │           ├── model/
        │           │   ├── User.java                        # Base user entity (Inheritance root)
        │           │   ├── Guest.java                       # Guest entity (extends User)
        │           │   ├── Staff.java                       # Staff entity (extends User)
        │           │   ├── Admin.java                       # Admin entity (extends User)
        │           │   ├── Room.java                        # Luxury room entity
        │           │   ├── Booking.java                     # Reservation record entity
        │           │   ├── Amenity.java                     # Hotel luxury amenity entity
        │           │   ├── GuestPreference.java             # Matcher criteria entity
        │           │   └── enums/
        │           │       ├── UserRole.java                # GUEST, STAFF, ADMIN
        │           │       ├── RoomCategory.java            # DELUXE, SUITE, PRESIDENTIAL
        │           │       ├── RoomStatus.java              # AVAILABLE, OCCUPIED, etc.
        │           │       └── BookingStatus.java           # PENDING, CONFIRMED, etc.
        │           ├── repository/
        │           │   ├── UserRepository.java              # Base user JPA repository
        │           │   ├── GuestRepository.java             # Guest JPA repository
        │           │   ├── StaffRepository.java             # Staff JPA repository
        │           │   ├── AdminRepository.java             # Admin JPA repository
        │           │   ├── RoomRepository.java              # Room inventory JPA repository
        │           │   ├── BookingRepository.java           # Booking ledger JPA repository
        │           │   ├── AmenityRepository.java           # Amenity JPA repository
        │           │   └── GuestPreferenceRepository.java   # Preference JPA repository
        │           ├── security/
        │           │   └── SecurityPlaceholderConfig.java   # Stage 1 security foundation
        │           ├── service/
        │           │   ├── GuestService.java                # Guest service interface
        │           │   ├── StaffService.java                # Staff service interface
        │           │   ├── AdminService.java                # Admin service interface
        │           │   ├── RoomService.java                 # Room inventory service interface
        │           │   ├── BookingService.java              # Booking service interface
        │           │   └── impl/
        │           │       ├── GuestServiceImpl.java        # Guest service implementation
        │           │       ├── StaffServiceImpl.java        # Staff service implementation
        │           │       ├── AdminServiceImpl.java        # Admin service implementation
        │           │       ├── RoomServiceImpl.java         # Room service implementation
        │           │       └── BookingServiceImpl.java      # Booking service implementation
        │           └── util/
        │               ├── DateTimeUtil.java                # Date calculation & formatting helper
        │               └── TariffCalculationHelper.java     # Occupancy dynamic tariff helper
        └── resources/
            ├── application.properties                  # Primary application configuration (PostgreSQL)
            ├── application-postgres.properties         # Direct PostgreSQL profile
            ├── static/
            │   └── css/
            │       └── style.css                       # Vanilla CSS luxury design system
            └── templates/
                ├── index.html                          # Public hotel home page
                ├── login.html                          # Sign-in & role switcher page
                ├── error.html                          # Error notification template
                ├── fragments/
                │   ├── navbar.html                     # Reusable header navigation
                │   └── footer.html                     # Reusable footer
                ├── guest/
                │   ├── dashboard.html                  # Guest dashboard
                │   ├── search-rooms.html               # Guest room discovery
                │   ├── my-bookings.html                # Guest reservations list
                │   └── profile.html                    # Guest preferences configuration
                ├── staff/
                │   ├── dashboard.html                  # Staff operations dashboard
                │   ├── bookings.html                   # Staff reservations ledger
                │   ├── guests.html                     # Staff guest directory
                │   └── room-status.html                # Staff live housekeeping board
                └── admin/
                    ├── dashboard.html                  # Executive administration dashboard
                    ├── rooms.html                      # Room inventory audit
                    ├── bookings.html                   # Master bookings ledger
                    ├── staff.html                      # Personnel management
                    └── reports.html                    # Dynamic tariff & occupancy analytics
```

---

## 7. How to Run the Project

### Prerequisites
* Java JDK 17 or higher (Java 17, 21, or 25)
* Apache Maven 3.8+
* PostgreSQL 12+ (Optional for Stage 1 initial test run)

### Step 1: Clone or Navigate to the Project Directory
```bash
cd "c:\Users\gagan\Downloads\Java project"
```

### Step 2: PostgreSQL Database Setup (Production Mode)
1. Open PostgreSQL (`pgAdmin` or `psql`) and create a database:
   ```sql
   CREATE DATABASE luxury_hotel_db;
   ```
2. In `src/main/resources/application.properties`, update your PostgreSQL credentials if different:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/luxury_hotel_db
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   ```
3. *Note on Stage 1 Standalone Run:* If your local PostgreSQL service is currently offline or not yet installed, the included `DatabaseConfig.java` automatically detects this and provides an embedded in-memory database fallback in PostgreSQL compatibility mode, allowing the application to compile and run immediately for evaluation without errors!

### Step 3: Build the Project
```bash
mvn clean compile
```

### Step 4: Run the Application
```bash
mvn spring-boot:run
```

### Step 5: Access the Web Application
Open your web browser and navigate to:
* **Home Page:** [http://localhost:8080/](http://localhost:8080/)
* **Sign In / Portal Selector:** [http://localhost:8080/login](http://localhost:8080/login)
* **Guest Portal:** [http://localhost:8080/guest/dashboard](http://localhost:8080/guest/dashboard)
* **Staff Portal:** [http://localhost:8080/staff/dashboard](http://localhost:8080/staff/dashboard)
* **Admin Portal:** [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard)

---

## 8. Pre-Seeded Sample Accounts for Testing / Viva

| Access Level | Username | Password | Role Details |
|---|---|---|---|
| **Guest** | `alex_morgan` | `password123` | VIP Guest with preferences set |
| **Staff** | `sarah_desk` | `password123` | Senior Concierge (Front Office) |
| **Admin** | `admin` | `admin123` | Chief Operating Officer (Super Admin) |

---

## 9. Stage 1 Scope vs. Stage 2 Roadmap

### Completed in Stage 1:
* Maven Spring Boot project setup with full Java 17+ compatibility.
* Complete Clean Architecture package structure (`config`, `controller`, `service`, `repository`, `model`, `dto`, `exception`, `security`, `util`).
* Basic entity model with full JPA relationships and OOP inheritance (`User`, `Guest`, `Staff`, `Admin`, `Room`, `Booking`, `Amenity`, `GuestPreference`).
* Enums (`UserRole`, `RoomCategory`, `RoomStatus`, `BookingStatus`).
* Spring Data JPA repositories with query helper methods.
* Service abstraction interfaces and concrete implementations.
* Controllers with page and placeholder mappings for all 3 access levels.
* Thymeleaf UI layouts and responsive Vanilla CSS luxury design system.
* Navigation bars for Guest (4 tabs), Staff (4 tabs), and Admin (5 tabs).
* Pre-seeded sample data in `DataInitializer` for instant demonstration.
* PostgreSQL database configuration with safe fallback for seamless startup.

### Intentionally Postponed to Stage 2:
* Full booking lifecycle logic (conflict prevention, dates overlap validation, automatic confirmation).
* Payment gateway and billing transaction processing.
* Algorithmic Dynamic Tariff Optimizer (real-time recalculation based on demand thresholds).
* Algorithmic Guest Preference Matcher (scoring and recommendation engine).
* Full Spring Security authentication/authorization filters and JWT/session guards.
* Loyalty points accrual and redemption engine.
* Advanced PDF report generation and email notifications.
