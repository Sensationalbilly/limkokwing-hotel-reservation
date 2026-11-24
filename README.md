# Limkokwing Hotel Reservation System

A desktop app for a hotel front desk in Sierra Leone, written in **Java Swing** with a **MySQL** database.
Receptionists book rooms, check guests in and out, and look after guest records. Administrators also
manage rooms, staff accounts, attendance and staff performance.

Built for **PROG209 Event Driven Programming** at Limkokwing University of Creative Technology, Sierra Leone.
Released under the [MIT License](LICENSE).

---

## Features

### Start-up and sign-in
| Screen | What it does |
|---|---|
| **Splash screen** | Shows the hotel logo while a progress bar follows real start-up steps, including a live database check, then fades into sign-in. |
| **Who are you?** | Staff pick **Administrator** or **Receptionist / Staff** before signing in. Signing in with the wrong kind of account is refused with a clear message. |
| **Sign in** | Username and password. Passwords are stored as salted PBKDF2 hashes, never as plain text. |
| **Forgot password?** | Staff send a reset request (with an optional note) to the administrators, and can check its status: *Waiting*, *Done* or *Closed*. |
| **Clock-in** | A staff member's first sign-in each day records their arrival time automatically (see [Attendance](#attendance-rules)). |

### Dashboard pages
| Page | Who | What it does |
|---|---|---|
| **Overview** | Everyone | Occupancy, available rooms, today's arrivals and departures, and a room board grouped by floor with filter chips. Clicking a room opens its details. Booked revenue is shown to administrators only; receptionists see guests in house instead. |
| **New booking** | Everyone | Five steps: guest, dates, a free room, extras and payment, and a price summary that updates as you type. Press Ctrl+Enter to confirm. |
| **Bookings** | Everyone | A searchable, sortable list. Check in, check out and cancel; delete is admin only. Double-click or press Enter for details, and right-click for a menu. |
| **Guests** | Everyone | Add, edit, search and delete guest profiles. |
| **Rooms** | Admin edits, staff view | Add rooms and set prices, status and **room features** (20 checkboxes, such as Wi-Fi, hot water, generator backup and breakfast). |
| **Team** | Admin | **Today:** who is in, on time or late. **History:** 7, 30 or 90 days of attendance. **Performance:** a score out of 100 per staff member, top performer, most punctual, and a 1–5 star rating from the admin. |
| **Staff** | Admin | Create accounts, change usernames, names and roles, and handle password-reset requests by setting a temporary password. A red badge on the sidebar shows waiting requests. |

All pop-ups are animated cards that dim the window behind them. Risky actions use red buttons, and in those
pop-ups Enter means *Cancel*, so nothing is deleted by accident.

### Business rules
- A room can't be **double-booked**, even if two receptionists book it at the same moment. A database transaction locks the room.
- Check-in is only allowed from the arrival date. Occupancy changes automatically on check-in and check-out.
- Guests and rooms with booking history can't be deleted.
- **Pricing (polymorphism):** Standard rooms charge the nightly rate. Deluxe rooms take 5% off stays of 5 or more nights. Suites add a 10% service charge.
  Breakfast is free when the room has the *Complimentary breakfast* feature ticked. Otherwise it costs SLE 150 per guest per night. An airport pickup from Lungi costs SLE 450.
- **Passwords:** an admin reset sets a *temporary* password. The staff member must choose their own new password the next time they sign in.
- The last administrator can't be deleted or demoted, and admins can't delete their own account.

### Attendance rules
- Work starts at **8:00 AM**. Anyone signing in **after 8:30 AM is late**.
- The clock-in time comes from the **database server's clock in Freetown time (GMT)**. Staff can't type or change it.
- Staff also enter the time they *say* they arrived. Being late is always decided by the recorded time, and a late arrival must give a reason. The admin sees both times and any difference, for example *"Typed 2 min earlier"*.
- Signing out or closing the app records the time the staff member left.
- **Performance score** = 40% punctuality + 30% attendance + 30% work done. Work done counts bookings, check-ins, check-outs and new guests, which the system records automatically.

---

## Setup

### 1. Install the tools
- **JDK 17 or newer**: check with `java -version`.
- **XAMPP** (MySQL/MariaDB), or **MySQL 8** Community Server.
- *(Optional)* **Maven**, only if you want to build with `mvn`.

### 2. Start MySQL and create the database
1. Open the XAMPP Control Panel and click **Start** next to **MySQL**.
2. Create the tables using **one** of these:
   - **phpMyAdmin** (http://localhost/phpmyadmin): Import `database/schema.sql`, then `database/seed.sql`.
   - **Command line:**
     ```bash
     mysql -u root -p < database/schema.sql
     mysql -u root -p < database/seed.sql     # optional sample rooms, guests and bookings
     ```

`schema.sql` is safe to run again. Run `seed.sql` only **once**.
If your database was created with an older version of this project, the app adds any new tables and columns automatically when it starts.

### 3. Configure the connection (only if your MySQL has a password)
Copy `config/db.properties.example` to `config/db.properties` and fill in your details:
```properties
db.url=jdbc:mysql://localhost:3306/hotel_reservation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=your_password
```
This file is git-ignored, so passwords never reach GitHub. The environment variables `HOTEL_DB_URL`,
`HOTEL_DB_USER` and `HOTEL_DB_PASSWORD` override it. If there is no config file, the app uses `root`
with no password, which is the XAMPP default.

### 4. Run
**Windows:** double-click **`run.bat`**. No Maven is needed: it downloads the MySQL driver the first time, compiles the code and starts the app.

**macOS / Linux:** `sh run.sh`

**With Maven:**
```bash
mvn compile exec:java
mvn package    # builds target/hotel-reservation-system-1.0.0-jar-with-dependencies.jar
```
Run the app from the project folder so it can find `config/` and the logo.

### 5. Sign in
The first start creates two accounts:

| Choose | Username | Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Receptionist / Staff | `reception` | `reception123` |

> Change these passwords before any real use (Staff → Reset password).

---

## Keyboard shortcuts
| Keys | Action |
|---|---|
| Ctrl+1 … Ctrl+7 | Switch page (in sidebar order) |
| Ctrl+N | New booking |
| Ctrl+Enter | Confirm booking |
| F5 | Refresh the current page |
| F1 | Shortcut help |
| Enter / Space | Open the focused room card or role card |
| Esc | Close a pop-up, or go back to "Who are you?" |
| Alt + underlined letter | Jump to that field or button |

---

## Project structure

```
hotel-reservation-system/
├── database/            schema.sql (7 tables) and seed.sql (sample data)
├── config/              db.properties.example
├── docs/                wireframes/ and screenshots/ for the report
├── run.bat / run.sh     build and run without Maven
└── src/main/
    ├── resources/images/logo.png
    └── java/com/hotel/
        ├── Main.java          starts Swing on the Event Dispatch Thread, then shows the splash
        ├── model/             Room (abstract) → StandardRoom, DeluxeRoom, SuiteRoom; Customer, Booking, User,
        │                      Attendance, PasswordRequest, StaffPerformance, enums
        ├── dao/               BaseDAO<T> (abstract) → Room, Customer, Booking, User, PasswordRequest,
        │                      Attendance and Activity DAOs (all the SQL lives here)
        ├── db/                DBConnection (config + JDBC), SchemaUpdater (upgrades old databases)
        ├── controller/        business rules, Session, EventBus, ActivityLog
        ├── util/              PasswordUtil (PBKDF2), Formats (money, dates, times)
        └── ui/                SplashWindow, LoginFrame, DashboardFrame, 7 pages (BasePanel subclasses), pop-ups
            └── components/    custom Swing components (buttons, cards, badges, chips, stars, ...)
```

**Layers:** the UI only talks to controllers, controllers only talk to DAOs, and only DAOs contain SQL.
After changing data, a controller publishes an event on the `EventBus` (for example `BOOKINGS_CHANGED`), and every
screen showing that data refreshes itself. Screens never call each other directly.

```
 UI (Swing listeners)  ──calls──▶  Controller (rules)  ──calls──▶  DAO (SQL)  ──JDBC──▶  MySQL
        ▲                                │
        └──────── EventBus.publish ──────┘   (screens refresh themselves)
```

---

## How the assignment requirements are met

| Requirement | Where |
|---|---|
| **Swing components (6+)** | JFrame, JWindow (splash), JDialog (pop-ups), JButton, JToggleButton (filter chips), JTextField, JPasswordField, JTextArea, JComboBox (including editable), JCheckBox, JRadioButton + ButtonGroup, JSpinner (number, date and time), JList with a custom renderer, JTable with sorting and filtering, JTabbedPane, JMenuBar / JMenu / JCheckBoxMenuItem / JRadioButtonMenuItem, JPopupMenu, JScrollPane, JSeparator |
| **Event handling** | `ActionListener` (buttons, menus, combos), `MouseListener` / `MouseMotionListener` (cards, tables, star rating), `KeyListener` and key bindings (Enter, Space, Esc, Ctrl+Enter), `ItemListener` (radios, check boxes, chips), `ChangeListener` (spinners), `ListSelectionListener` (lists, tables), `DocumentListener` (live search), `FocusListener` (focus rings), `WindowListener` (confirm exit, clock-out), `ComponentListener` (room grid columns follow window width), `javax.swing.Timer` (animations, a 15-second check for password requests), `SwingWorker` (start-up, sign-in), and a custom publish/subscribe `EventBus` |
| **Layout managers (2+)** | `BorderLayout`, `GridLayout`, `FlowLayout`, `GridBagLayout` (`FormPanel`, sign-in), `CardLayout` (dashboard pages, sign-in steps, new or returning guest) |
| **Inheritance & polymorphism** | `Room` → 3 room types override `calculateStayCost()`, `getDefaultAmenities()` · `BaseButton` → Primary/Success/Danger/Nav buttons override colours and painting · `RoundedPanel` → `StatCard`, `RoomCard`, `RoleCard` · `ModalCard` (JDialog) → `MessageCard`, `RoomDetailsDialog`, `ForgotPasswordDialog`, `AttendanceDialog`, form card · `BasePanel` → 7 pages override `getTitle()` / `refresh()` · `BaseDAO<T>` → 7 DAOs override `map()` · `ListTableModel<T>`, `StarRating` → `CellRenderer`, custom table and list renderers |
| **Accessibility** | Accessible names and descriptions on controls, `setLabelFor` on every form label, mnemonics and keyboard shortcuts, full keyboard use, visible focus rings, custom cards announced as buttons, **High contrast** and **Text size** (View menu, remembered between sessions), status always written as text (never shown by colour alone) |
| **Data persistence (MySQL)** | 7 tables with foreign keys and CHECK constraints, `PreparedStatement` everywhere (no SQL injection), transactions for booking and check-in/out, `SELECT … FOR UPDATE` against double-booking, automatic schema upgrade |
| **DPG alignment** | See below |

---

## Digital Public Goods alignment

- **Openness:** MIT licence, public source, documented setup, and only open tools (OpenJDK, MySQL/MariaDB, Connector/J).
- **Inclusivity:** works fully from the keyboard, with screen-reader labels, high contrast and larger text. Built for local needs: Leone (SLE) prices, Orange Money and Afrimoney payments, Lungi airport pickup, Freetown time and generator backup as a room feature.
- **Privacy & security:** salted PBKDF2 password hashes; temporary passwords that staff must replace; database credentials kept out of the code and out of Git; revenue visible to administrators only; guest email optional.
- **Scalability & reuse:** layered design. A new room type is one `Room` subclass, a new page extends `BasePanel`, and a new pop-up extends `ModalCard`. The work-start and late times are constants in `AttendanceController`.
- **Accountability:** attendance and staff activity are recorded automatically and fairly; records with history can't be deleted; risky actions always ask first.

---

## Team: Group 5

| No. | Name | Student ID | Role / main contribution |
|---|---|---|---|
| 1 | Jaria Bah | 905004628 | |
| 2 | Mariama Seray Bah | 905005454 | |
| 3 | Mohamed Barry | 905004770 | |
| 4 | Sahr Kemoh Ngaujah | 905005346 | |
| 5 | Mariama Dalan Bah | 905005123 | |

See [CONTRIBUTING.md](CONTRIBUTING.md) for how we use branches, issues and pull requests.

## License

[MIT](LICENSE). You are free to use, change and share this software.
