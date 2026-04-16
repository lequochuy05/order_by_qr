# 🧾 QR Ordering System

**QR Ordering System** is a modern ordering system using QR codes, designed to optimize the service process in restaurants and eateries. Customers can easily scan QR codes at their table to view menus, receive smart food recommendations, and order directly without waiting for staff.

---

## 🚀 Key Features

### 1. 📱 Customer Experience (Customer Side)

- **QR Code Scanning:** Automatically identifies table numbers and displays the corresponding menu.

- **Smart Menu:** Clearly categorizes dishes, supports searching and filtering by category.

- **AI Recommendations:**

- Personalized food recommendations based on time of day and weather conditions.

- Cross-sell suggestions for accompanying items (drinks, toppings) when customers add items to their cart.

- **Shopping Cart & Ordering:** Fast checkout and ordering process, smooth interface.

### 2. 👔 Store Management (Admin Side)
- **Menu Management:** Add, edit, and delete dishes, categories, and combo packages.

- **Order Management:** Track order status in real time.

- **Statistics & Reports:** Revenue charts, best-selling dishes, and business performance via Recharts.

- **QR Code Management:** Create and manage unique QR codes for each table.

### 3. 🤖 Outstanding AI Features

The system integrates **AI** to:

- Analyze food images to automatically fill in information (name, estimated price).

- Provide intelligent food suggestions based on the user's real-world context.

---

## 🛠️ Technology Used

| Components | Technology |
|-----------|-----------|
| **Backend** | Spring Boot 3, Java 21, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL, Cloudinary |
| **Frontend** | React 19, Vite, Tailwind CSS 4 |
| **Realtime** | WebSocket (STOMP & SockJS) |
| **State Management** | Zustand |
| **Deployment** | Docker, Vercel (Frontend) |

---

## 💻 Installation Guide

### System Requirements
- Java 21+
- React 19+
- Maven 3.9+

### 1. Backend Configuration
Create a `.env` file in the root directory with the following parameters:
```env
DB_URL=jdbc:postgresql://your-db-url
DB_USERNAME=your-username
DB_PASSWORD=your-password
JWT_SECRET=your-secret-key
GEMINI_API_KEY=your-gemini-api-key
CLOUDINARY_CLOUD_NAME=your-name
CLOUDINARY_API_KEY=your-key
CLOUDINARY_API_SECRET=your-secret
```

Run the application:
```bash
mvn spring-boot:run
```

### 2. Configuration Frontend
```bash
cd order-by-qr-frontend
npm install
npm run dev
```


---
## 📧 Contact & Support

**Author**: Wuchuy </br>
**Email**: [wuchuy05.dev@gmail.com](mailto:wuchuy.dev@gmail.com)
