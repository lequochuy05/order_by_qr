# 🧾 QR Code Ordering System (QROS)

**QROS** is a modern ordering system using QR codes, designed to optimize the service process in restaurants and eateries. Customers can easily scan the QR code at their table to view the menu, receive smart food recommendations, and order directly without waiting for staff.

---

## 🚀 Key Features

### 1. 📱 Customer Experience (Customer's Perspective)

- **QR Code Scanning:** Automatically identifies the table number and displays the corresponding menu.

- **Smart Menu:** Clearly categorizes dishes, supports searching and filtering by category.

- **AI Recommendations:**

- Personalized food recommendations based on the time of day and weather conditions.

- Cross-selling suggestions for complementary items (drinks, toppings) when customers add items to their cart.

- **Shopping Cart & Ordering:** Fast checkout and ordering process, smooth interface.

### 2. 👔 Store Management (Admin Side)

- **Menu Management:** Add, edit, and delete food combos, categories, and product bundles.

- **Order Management:** Track order status in real time.

- **Statistics & Reports:** Revenue charts, number of dishes sold, and business performance through Recharts.

- **QR Code Management:** Create and manage custom QR codes for each computer.

### 3. 🤖 Outstanding AI Features

Integrated **AI** system to:

- Analyze food images to automatically fill in information (name, estimated price).

- Provide intelligent food recommendations based on the user's real-world context.

---

## 🛠️ Technologies Used

| Components | Technologies |

|------------|-----------|

| **Backend** | Spring Boot 3, Java 21, Spring Security, Spring Data JPA |

| **Database** | PostgreSQL, Cloudinary |

| **User Interface** | React 19, Vite, Tailwind CSS 4 |

| **Real-time** | WebSocket (STOMP & SockJS) |

| **State Management** | Zustand |

| **Deployment** | Docker, Vercel (User Interface) |

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

CLOUDINARY_CLOUD_NAME=your-name
CLOUDINARY_API_KEY=your-key
CLOUDINARY_API_SECRET=your-secret

ROBOFLOW_API_KEY=your-secret-key
ROBOFLOW_WORKSPACE=your-workspace
ROBOFLOW_PROJECT=your-project
ROBOFLOW_VERSION=your-version
```

Run the application:

``` bash
mvn spring-boot:run
```

### 2. User Interface Configuration
```bash
cd frontend
npm install
npm run dev
```

---
## 📧 Contact & Support

**Author**: Wuchuy </br>
**Email**: [wuchuy05.dev@gmail.com](mailto:wuchuy.dev@gmail.com)
