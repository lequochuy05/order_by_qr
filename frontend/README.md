# 📱 QROS
Providing a modern interface, optimized speed, and a smooth experience for both customers (scanning QR codes to order) and administrators (managing the dashboard).

## ✨ Key Features
- **Customer QR Menu:** Quickly order via QR code, intuitive shopping cart, professional mobile UI.

- **Admin Dashboard:** Comprehensive management of categories, dishes, and real-time order tracking.

- **Offline AI Assistant:** Integrates AI image recognition technology (using YOLOv8 architecture running on **TensorFlow.js**) to recognize dishes and automatically fill in the dish name, price, and category ID without incurring Cloud API costs.

- **Real-time Server:** Instantly displays new orders via WebSocket.

## 💻 End-User Technology
- **Framework:** [React 19](https://react.dev/) (Vite) - Extremely impressive rendering speed, super-fast build.

- **State Management:** [Zustand](https://zustand-demo.pmnd.rs/) - Lightweight state management, eliminating the clutter of Redux.

- **User Interface:** [Tailwind CSS v4](https://tailwindcss.com/) - Modern responsive design.

- **AI Module:** [TensorFlow.js](https://www.tensorflow.org/js) - Loads and runs Neural networks directly in the local browser (Client-side computation).

- **Networking:** [Axios](https://axios-http.com/) integrates JWT for all requests.

- **Connection:** [StompJS](https://stomp-js.github.io/stompjs/) & [SockJS](https://github.com/sockjs/sockjs-client) for stable bidirectional connection.

---

## 🚀 Initial Installation and Run Guide

### 1. System Requirements
- Environment: Node.js (v18 LTS or higher recommended).

- Package Manager: npm or yarn.

### 2. Installing Libraries (Dependencies)
Clone the source code, specify the `frontend` directory and install:
```bash
cd frontend
npm install
```

### 3. Configuring AI Dish Classifier
The AI-powered automatic dish entry feature in the Admin Dashboard requires loading model weights. 1. Training for food image recognition: Read the detailed instructions in the root directory `AI/README.md`.

2. Place the resulting files (e.g., `model.json` and `.bin`) after training the AI, along with the metadata file `labels.json`, into the exact directory:

`public/models/dish-classifier/`

3. Ensure your `labels.json` is a JSON map format that provides sufficient fields for `name`, `categoryid`, and `price` for the AI ​​module to extract and populate the form. (See example in the `AI/README.md` documentation).

### 4. Starting the Dev Environment
After completing the AI ​​data, run the project:
```bash
npm run dev
```
Access the Frontend application via the default port: `http://localhost:5173`.

---

## 📁 System Directory Structure

```text
src/
├── components/ # Shared Components (Layout, UI, Data-table...)
├── context/ # AuthContext manages internal validation status throughout
├── hooks/ # Shared utility hooks (useWebsocket, useApi...)
├── pages/ # Permission-granting Views (Auth, Customer flow, Dashboard)
├── services/ # Services that call external APIs, configure Axios, and especially aiLocalService logic
├── utils/ # Utils that handle pricing, text, datetime formatting
└── App.jsx # Routing defines the overall layout of routes
```
