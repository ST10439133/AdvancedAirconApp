# INSY7315 - TEAM LOGIX

## Group members
- ST10439133 - Camryn Naidoo
- ST10441399 - Suvan Samlall
- ST10451026 - Calib Frank
- ST10446908 - Caleb Ragaven
- ST10296234 - Joshua Chetty
- ST10451537 - Keshvir Parthab

## Part 2 Video Link:
### Android App Video Link: https: https://youtu.be/sTK4bFBy2yc

## Final POE Links:
### Mobile App Bluestacks Demo YouTube Link: https://youtu.be/AnBUttFr7ts 
### Mobile App Offline Mode, Notifications & Settings (Mobile Phone) Demo YouTube Link: https://youtube.com/shorts/XeOTCNnXSGg 
### Mobile App Hosting Demo YouTube Link: https://youtu.be/qJ55gBSgkCE
### Android App GitHub Link: https://github.com/ST10439133/AdvancedAirconApp.git
### Android App API GitHub Link: https://github.com/ST10439133/arcticflow-api

# Advanced Aircon App — HVAC Service Management Platform
A comprehensive Android application for HVAC service management that connects managers, technicians, and customers through a unified platform. Built with Jetpack Compose, Room, Firebase, and a Node.js backend.

## About
Advanced Aircon App streamlines the entire HVAC service lifecycle — from building registration and service requests to quote generation, job scheduling, real-time technician tracking, and job card completion. The app supports two distinct user roles (Manager and Technician) with role-specific dashboards, offline-first architecture, and multi-language support (English, Afrikaans, isiZulu).

## The Problem It Solves
HVAC service businesses struggle with:
- Fragmented communication between customers, managers, and field technicians
- No real-time visibility into technician locations and job progress
- Paper-based job cards and manual quote processes
- Offline scenarios where technicians can't update job status
Advanced Aircon App solves this with a mobile-first, offline-capable solution that keeps everyone in sync - even when the network drops.

## Key Features
### Authentication & Security
- Firebase Email/Password authentication
- Google Sign-In integration
- Biometric (fingerprint) quick login
- Role-based access control (Manager / Technician)
- JWT-based API authentication with automatic token refresh
- Secure sign-out that preserves active tracking state

### Manager Dashboard
- Real-time overview of buildings, requests, and pending quotes
- Accept or decline quotes with scheduling dialog
- Add and manage buildings with full South African address hierarchy (province to city to suburb)
- View pending service requests from customers
- Quick actions: create requests, view quotes, run BTU calculator, browse products
- Payment Processing: Secure mock checkout flow using an abstract `PaymentProcessor` interface, ready to be swapped for a live gateway (e.g., Paystack, Yoco, Stripe) without changing UI code.
- Automated PDF Receipts: On successful payment, a branded PDF receipt is generated locally and can be viewed, downloaded, or shared via the Android share sheet.

### Technician Dashboard
- Personal job schedule at a glance (today / pending / completed counts)
- View and respond to new service requests
- Create detailed quotes with service fees and parts from catalog
- Real-time "On My Way" tracking — customers see technician location on a map
- Comprehensive job card submission with:
  -Work summary
  -Site photos (camera + gallery)
  -Parts used (catalog or custom)
  -Time tracking (start/stop timer)
  -Additional notes
- Payments Received Ledger: View a running ledger of all paid jobs, including payment references, paid dates, and the ability to view/download the same PDF receipt issued to the manager.

### Decline Request
- Decline job requests with a mandatory reason that surfaces on the manager's dashboard.
- Manager can re-open declined requests, returning them to the technician queue.

### Live Technician Tracking
- Google Maps integration with real-time marker updates
- Dashed polyline from technician to destination
- Status colour coding:
  -Blue — On the way
  -Orange — On site
  -Green — Completed
- Legend, technician list, and "last seen" timestamps
- Foreground service that self-terminates if the JWT expires
- Sticky cache for completed markers (persists even if the backend filters completed rows)

### Product Catalogue
- Browse HVAC units by brand (Samsung, LG, Daikin, Carrier, TCL, Hisense, etc.)
- Search by name, brand, or model
- Sort by price (low/high) or rating
- Product images served from Supabase Storage
- Brand filtering and price range filtering

### BTU Calculator
- Calculate cooling load based on room dimensions (metric)
- Factor in windows, occupants, sun exposure, insulation quality, and room type
- Unit conversion: BTU/hr to kW to Tons
- Suggested product matches from the catalogue (+- 20% of calculated load)

### Notifications
- In-app notification centre with tabs (All / Jobs / Quotes / System)
- Unread badges
- Mark individual or all as read
- Triggered on: quote submitted, quote accepted, job scheduled, technician on the way

### Theming
- Light and dark mode
- Dynamic colors (Android 12+)
- Consistent brand design system (navy blue, red accent)

### Offline-First Architecture
- All writes go to Room first
- Automatic queue for offline mutations (sync_queue table)
- Background sync worker (WorkManager, every 15 minutes)
- Pull-sync with server-ID reconciliation (prevents duplicate inserts)
- Conflict resolution: server PENDING never overwrites local ACCEPTED/DECLINED

### End-to-End Payment & Receipt Flow
Advanced Aircon App implements a complete, testable payment lifecycle:
1. **Job Completion:** Technician submits the job card → Job status becomes `COMPLETED` → Quote status becomes `AWAITING_PAYMENT`.
2. **Manager Checkout:** Manager taps "Pay Now" on the dashboard → Directed to a checkout screen with an order summary (service fee, parts, discount, and total).
3. **Mock Gateway:** The `MockPaymentProcessor` simulates a network round-trip and returns a unique payment reference. (Designed to be swapped for a real gateway like Paystack or Yoco by simply implementing the `PaymentProcessor` interface).
4. **Persistence:** The `Quote` entity is updated locally with the reference and `paidAt` timestamp. Notifications are dispatched to both the Manager and the Technician.
5. **Receipt Generation:** A PDF is generated on-device using Android's `PdfDocument` API (zero external dependencies), featuring the company logo, job details, and a full cost breakdown.
6. **Distribution:** Both parties can view the receipt in-app, download it to the device's `Downloads` folder, or share it via email/WhatsApp.

## Architecture
Advanced Aircon App follows MVVM + Repository architecture with a clear separation of concerns.

## Tech Stack
| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose, Material 3, Navigation Compose |
| **Architecture** | MVVM, Repository pattern, StateFlow |
| **Local DB** | Room (SQLite) with TypeConverters |
| **Networking** | Retrofit + OkHttp + Gson |
| **Auth** | Firebase Auth (Email, Google, Biometric) |
| **Realtime** | Firebase Firestore (technician locations) |
| **File Storage** | Supabase Storage (product images, brochures) |
| **Background Work** | WorkManager (periodic sync) |
| **DI** | Manual (Factory pattern) |
| **Maps** | Google Maps Compose |
| **Image Loading** | Coil |
| **Async** | Kotlin Coroutines + Flow |
| **Serialization** | Gson (JSON) |
| **Payments** | Custom `PaymentProcessor` interface (Mock impl. ready for Paystack/Yoco/Stripe) |
| **PDF Generation** | Android `PdfDocument` API (Native) |


## Getting Started
### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+
- Android SDK 34 (compileSdk), minimum SDK 26 (Android 8.0)
- Firebase project with:
  -Email/Password authentication enabled
  -Google Sign-In configured
  -Firestore database created
- Supabase project with a products storage bucket
- Node.js backend deployed and accessible via HTTPS

### Clone the Repository
- git clone https://github.com/yourusername/AdvancedAirconApp.git
- cd AdvancedAirconApp

### Configure Firebase
- Create a Firebase project at console.firebase.google.com
- Add an Android app with package name com.insy7315.advancedairconapp
- Download google-services.json and place it in app/
- Enable Email/Password and Google sign-in methods in Authentication
- Create a Firestore database (start in production mode)
- Deploy the Firestore security rules

### Configure Supabase
- Create a Supabase project
- Create a public storage bucket named products
- Note your project URL and anon key

### Configure Local Secrets
Create local.properties in the project root (this file is gitignored):

### Configure build.gradle.kts (app module)
The app reads secrets from local.properties and exposes them via BuildConfig

### Google Maps API Key
Add your Maps API key to AndroidManifest.xml
And define it in res/values/strings.xml

### Build & Run
open the project in Android Studio and click Run.

##  Security Notes
- JWT storage: JWT is stored securely in SharedPreferences for API authentication. For production, consider EncryptedSharedPreferences from androidx.security:security-crypto.
- Firestore rules: Restrict tech_locations collection so users can only read all, write their own.
- API base URL: Always use HTTPS. The interceptor auto-injects the JWT into every request's Authorization header.
- Sign-out behaviour: The app preserves the server-side tracking row on sign-out (so "On My Way" state survives a role switch). Stale rows are cleaned up on next cold start if there's no active job.
- Payment Gateway Abstraction: All payment logic is isolated behind the `PaymentProcessor` interface (located in `data/payment/`). The current implementation is a `MockPaymentProcessor` for demonstration, but no production card data is ever stored, transmitted, or logged. To go live, you would create a `PaystackPaymentProcessor` and swap it in `PaymentViewModel` — no Compose UI changes required.

## Workflow Diagram
<img width="3410" height="3475" alt="Updated Workflow AA" src="https://github.com/user-attachments/assets/6661cb5a-5b36-436b-a992-61f81e463f62" />

## Hosted On Railway
<img width="1917" height="952" alt="api deployed" src="https://github.com/user-attachments/assets/67e1affa-e300-4a25-9ff8-0c7a41d4f18b" />

## AI Usage: 
1.	Resolving the Technician Role Flip-Flop on First Login
We used AI to diagnose a race condition in the authentication flow where a first-time technician signing in with technician@gmail.com was incorrectly assigned the MANAGER role because the app defaulted to MANAGER for any unknown user before the server responded. AI helped us rewrite the AuthViewModel to implement a server-first role resolution strategy, changing the safe fallback to TECHNICIAN and updating the syncAndResolve method so a local role is only trusted if it came from a deliberate source (existing database row, pre-provisioned email, or Firebase |ROLE annotation). This prevented the app from pushing a wrong role to the backend and fixed the sign-in inconsistency.
AI Used: https://chatgpt.com/c/6ae622c2-0244-83e9-9d76-46c784283a44 

2.	Implementing the Job Request Decline Workflow
We needed a way for technicians to decline a job request with a mandatory reason, and for that reason to surface on the manager's dashboard. AI helped us design the full vertical slice: adding a DECLINED_BY_TECH status and three new columns (declineReason, declinedBy, declinedAt) to the ServiceRequest entity, writing a proper Migration(14, 15) for the Room database to avoid wiping user data, adding DAO queries for declining and re-opening requests, and wiring up a DeclineReasonDialog that forces a minimum-length reason before submitting. AI also helped implement the manager-side DeclinedRequestCard with a re-open action that puts the request back into the technician queue.
AI Used: https://chatgpt.com/c/6ae622c2-0244-83e9-9d76-46c784283a34 

3.	Fixing Button Clipping on the "Create Quote" Action Row
On the technician's Job Requests screen, the "Create Quote" button text was wrapping to two lines and getting clipped on narrower devices because both buttons were using Modifier.weight(1f) and fighting for equal space. AI helped us redesign the action row using proportional weights (weight(0.35f) for the secondary "Decline" button and weight(0.65f) for the primary "Create Quote" button), added maxLines = 1 and contentPadding = PaddingValues(0.dp) to prevent text wrapping, and swapped the dollar-bill icon for a ReceiptLong icon so the layout reads better visually.
AI Used: https://chatgpt.com/c/6ae622c2-0244-83e9-9d76-46c784283a54 

4.	Resolving Dark Mode Visibility Issues Across Multiple Screens
When switching to dark mode, several screens became unusable because they were using hardcoded Color.White and Color.Black instead of theme-aware tokens—most notably, the address autocomplete dropdown in AddBuildingScreen rendered as white text on a white background, and the PendingQuoteCard on the manager dashboard created a stark white card with unreadable text against the dark theme. AI helped us systematically audit these screens and replace every hardcoded color with the appropriate MaterialTheme.colorScheme token (onSurface for primary text, onSurfaceVariant for subtitles, surface for card backgrounds, errorContainer / secondaryContainer for status pills), so the app now respects the system dark mode preference throughout.
AI Used: https://chatgpt.com/c/6ae622c2-0244-83e9-9d76-46c784283b35  


5.	Designing the Offline-First Sync Queue for Declined Requests
To ensure a technician could decline a request while offline (e.g., in a basement plant room with no signal), we used AI to help integrate the decline workflow into the existing SyncManager queue. AI guided us to enqueue a TYPE_REQUEST_STATUS sync item with the DECLINED_BY_TECH payload when the device is offline, and to update syncPendingRequests and syncMyRequests so that a terminal status like DECLINED_BY_TECH isn't overwritten by a stale PENDING from the server. This means the manager sees the decline and the reason immediately on their own device (via Room), and the backend catches up the next time SyncWorker fires.
AI Used: https://chatgpt.com/c/6ae622c2-0244-83e9-9d76-46c784283d75 

6.	Stripping Out Unused Language Feature from Settings
After removing the language selection UI from the Settings screen, we used AI to identify all the dead code that was left behind—the LocaleManager.kt utility, the attachBaseContext override in MainActivity, and the translation maps in the Translations object. AI also helped us verify that no other screens were still calling rememberTranslation(), so the app now cleanly defaults to the device's system language without the risk of stale locale preferences stored in SharedPreferences.
AI Used: https://chatgpt.com/c/6ae622c2-0244-83e9-9d76-46c784283h67

## CI/CD & Branching Strategy

### Branching Strategy
This project follows a standard Gitflow workflow:
- `master`: Production releases
- `develop`: Integration branch for features
- `feature/*`: Used for developing new features (merged via Pull Requests)

### CI/CD Pipelines
This project uses GitHub Actions for automated builds and deployments:
- **`ci.yml`**: Runs on every push to `master` and `develop`. It builds the project, runs unit tests (`QuoteTest.kt`), and performs linting to ensure code quality.
- **`release.yml`**: Triggers when a new version tag (e.g., `v1.0.0`) is pushed. It automatically builds a signed Release APK and attaches it to the GitHub Release.

## References
1. IEEE Xplore. 2026. A Comparative Study of User Experience in Mobile Banking Authentication Mechanism: Usability Analysis Using Usability Testing and SUS. [Online]. Available at: https://ieeexplore.ieee.org/abstract/document/11323707 [Accessed: 5 November 2026].
2. IEEE Xplore. 2025. Implementation of Biometric Technology in Indonesian Mobile Banking: A TAM Perspective on Enhancing Transaction Security and Enjoyment. [Online]. Available at: https://ieeexplore.ieee.org/abstract/document/11005053 [Accessed: 5 November 2026].
3. Android Developers. 2026. androidx.biometric | API Reference. [Online]. Available at: https://developer.android.com/reference/androidx/biometric/package-summary [Accessed: 3 November 2026].
4. IEEE Xplore. 2026. Building HIPAA-Compliant Offline Edge CRM Workflows with Secure ePHI Management and Resilient Synchronization. [Online]. Available at: https://ieeexplore.ieee.org/document/11476488/similar#similar [Accessed: 2 November 2026].
5. Android Developers. 2026. Build an offline-first app | App architecture. [Online]. Available at: https://developer.android.com/topic/architecture/data-layer/offline-first [Accessed: 2 November 2026].
6. Google for Developers. 2026. Use Kotlin coroutines in your Android app. [Online]. Available at: https://developer.android.com/codelabs/kotlin-coroutines [Accessed: 2 November 2026].
7. Android Developers. 2026. Migrate your Room database | Android Developers. [Online]. Available at: https://developer.android.com/training/data-storage/room/migrating-db-versions [Accessed: 1 November 2026].
8. IEEE Xplore. 2024. Design and Implementation of an Android-Based Building Management System Using Location Services. [Online]. Available at: https://ieeexplore.ieee.org/document/10184486 [Accessed: 1 November 2026].
9. IEEE Xplore. 2025. Design and Implementation of an Intelligent Facility Management Dashboard for Commercial Buildings. [Online]. Available at: https://ieeexplore.ieee.org/document/11038388 [Accessed: 5 November 2026].
10. IEEE Xplore. 2024. A Cloud-Based Platform for HVAC Maintenance Management and Quote Approval Workflows. [Online]. Available at: https://ieeexplore.ieee.org/document/10444775 [Accessed: 1 November 2026].
11. Android Developers. 2026. Material 3 in Compose | Jetpack Compose. [Online]. Available at: https://developer.android.com/develop/ui/compose/designsystems/material3 [Accessed: 1 November 2026].
12. IEEE Xplore. 2024. User-Friendly Data Entry Design for Mobile Facility Management Applications. [Online]. Available at: https://ieeexplore.ieee.org/document/10455021 [Accessed: 4 November 2026].
13. Android Developers. 2026. Dark theme | Jetpack Compose. [Online]. Available at: https://developer.android.com/develop/ui/compose/designsystems/material3#dark-theme [Accessed: 4 November 2026].
14. Android Developers. 2026. Save data on the device | Data and files. [Online]. Available at: https://developer.android.com/training/data-storage/shared-preferences [Accessed: 5 November 2026].
15. Android Developers. 2026. Dynamic color | Jetpack Compose. [Online]. Available at: https://developer.android.com/develop/ui/compose/designsystems/material3#dynamic-color [Accessed: 5 November 2026].
