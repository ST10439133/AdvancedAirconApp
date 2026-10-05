# INSY7315 - TEAM LOGIX
## Group members
- ST10439133 - Camryn Naidoo
- ST10441399 - Suvan Samlall
- ST10451026 - Calib Frank
- ST10446908 - Caleb Ragaven
- ST10296234 - Joshua Chetty
- ST10451537 - Keshvir Parthab

## Suvan Samlall (ST10441399) is submitting our Task 2 WIL assignment on behalf of Camryn Naidoo (ST10439133) who is away and is unable to submit

### Android App Video Link: https: https://youtu.be/sTK4bFBy2yc
### Android App API GitHub Link: https://github.com/ST10439133/arcticflow-api
### Website GitHub Link: https://github.com/ST10441359/HvacWebsite.git - you can find the website readme and youtube video in this link
### Website API Link: https://github.com/ST10441359/AdvancedAirAPI.git - you can find the website api readme in this link

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

### Localization
- English, Afrikaans, and isiZulu translations
- Instant locale switching without app restart
- Localized date/time formatting

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
- JWT storage: JWT is stored in SharedPreferences (not encrypted). For production, consider EncryptedSharedPreferences from androidx.security:security-crypto.
- Firestore rules: Restrict tech_locations collection so users can only read all, write their own.
- API base URL: Always use HTTPS. The interceptor auto-injects the JWT into every request's Authorization header.
- Sign-out behaviour: The app preserves the server-side tracking row on sign-out (so "On My Way" state survives a role switch). Stale rows are cleaned up on next cold start if there's no active job.

## Known Limitations
- No real-time push notifications (uses in-app notification center + 15-min background sync)
- Firestore is used only for technician locations — everything else goes through the REST API
- The parseFirebaseNameAndRole function uses | as a delimiter in the Firebase displayName field, which breaks if a user's name contains |
- Room uses fallbackToDestructiveMigration() — schema changes wipe the local DB

## Workflow Diagram
<img width="2474" height="2135" alt="Workflow Diagram for Advanced Air Conditioning" src="https://github.com/user-attachments/assets/a5f72d35-286b-4973-b866-9f2fad5420b8" />

## Hosted On Railway
<img width="1917" height="952" alt="api deployed" src="https://github.com/user-attachments/assets/67e1affa-e300-4a25-9ff8-0c7a41d4f18b" />

## References
Done in code








