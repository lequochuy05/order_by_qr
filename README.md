# 🧾 Sắc Màu Quán - QR Ordering System (QROS)

**QROS** is an enterprise-grade, event-driven ordering system using QR codes, designed to optimize the service process in restaurants and eateries. Customers can easily scan the QR code at their table to view the menu, receive smart food recommendations via AI, and order directly without waiting for staff.

---

## 🚀 Key Features

### 1. 📱 Customer Experience (Customer's Perspective)
- **Instant QR Code Scanning:** Automatically identifies the table number and displays the customized menu.
- **Smart Menu:** Clearly categorizes dishes, supports searching and filtering by category.
- **AI Recommendations (Powered by Gemini AI):**
  - Personalized food recommendations based on the time of day and real-time weather conditions.
  - Interactive AI Assistant that can answer questions and suggest cross-selling items (drinks, toppings).
- **Shopping Cart & Checkout:** Fast checkout process integrated seamlessly with modern UI.

### 2. 👔 Store Management (Admin Side)
- **Menu Management:** Add, edit, and delete food combos, categories, and product bundles.
- **Event-Driven Order Management:** Track order statuses in absolute real-time without polling lag, driven by WebSockets.
- **Automated Payment Processing:** Integrated with PayOS for automatic banking reconciliation. Features background cron jobs to auto-clear hanging transactions.
- **Statistics & Reports:** Revenue charts, employee performance, and trending dishes visualization using Recharts.
- **Staff Control:** Granular permission system for Managers and Chefs.

### 3. 🤖 AI & Deep Integrations
- **Computer Vision (Roboflow):** Analyzes food images to automatically fill in information (name, estimated price) for menu management.
- **Conversational AI (Gemini):** Embedded in the customer journey to provide a premium ordering experience.
- **Enterprise-Grade State Machine:** Rigid validation matrices for Order Status transitions to ensure 100% data integrity.

---

## 🛠️ Technologies Used

| Layer | Technology |
|-----------|-----------|
| **Backend Core** | Spring Boot 3, Java 21, Spring Security, MapStruct, Lombok `@SuperBuilder` |
| **Database & Cache** | PostgreSQL, Redis Cloud (High-performance caching & session management) |
| **Frontend** | React 19, Vite, Tailwind CSS 4, Recharts |
| **Realtime Infrastructure**| WebSocket (STOMP & SockJS) with Async Listeners & Heartbeats |
| **State Management** | Zustand (Optimized Global State) |
| **Integrations** | PayOS, Gemini AI, Roboflow, Cloudinary, JavaMailSender |
| **Monitoring** | Micrometer |

---

## 💻 Installation Guide

### System Requirements
- Java 21+
- React 19+
- Maven 3.9+

### 1. Backend Configuration
Create a `.env` file in the root directory and configure the environment variables as defined in `.env.exemple`:

```env
# Database & Cache
DB_URL=jdbc:postgresql://your-db-url
DB_USERNAME=your-username
DB_PASSWORD=your-password
REDIS_HOST=your-host
REDIS_PORT=your-port
REDIS_PASSWORD=your-password

# Security
JWT_SECRET=your-secret-key-must-be-very-long
JWT_EXPIRATION_MS=86400000

# Third-Party Integrations
CLOUDINARY_CLOUD_NAME=your-name
CLOUDINARY_API_KEY=your-key
CLOUDINARY_API_SECRET=your-secret

PAYOS_CLIENT_ID=your-payos-client-id
PAYOS_API_KEY=your-payos-api-key
PAYOS_CHECKSUM_KEY=your-payos-checksum

GEMINI_API_KEY=your-gemini-key
GEMINI_API_URL=your-gemini-url
```

Run the application:
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

### 2. Frontend Configuration
```bash
cd frontend
npm install
npm run dev
```

---

## 📧 Contact & Support

**Author**: Wuchuy  
**Email**: [wuchuy05.dev@gmail.com](mailto:wuchuy.dev@gmail.com)
