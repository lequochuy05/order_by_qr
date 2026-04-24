## 💻 Technologies Used
Frontend is built on a modern platform, focusing on performance and user experience:
- **Framework:** [React 19](https://react.dev/) (Vite) - Extremely fast rendering speed.
- **State Management:** [Zustand](https://zustand-demo.pmnd.rs/) - Lightweight and efficient state management.
- **Styling:** [Tailwind CSS v4](https://tailwindcss.com/) - Modern, adaptive interface design.
- **Icons:** [Lucide React](https://lucide.dev/) & [React Icons](https://react-icons.github.io/react-icons/).
- **Charts:** [Recharts](https://recharts.org/) - Visual charts for Admin.
- **Networking:** [Axios](https://axios-http.com/) integrates JWT authentication.
- **Real-time:** [StompJS](https://stomp-js.github.io/stompjs/) & [SockJS](https://github.com/sockjs/sockjs-client) provide stable bidirectional WebSocket connections.

---

##  Cấu trúc thư mục

```text
src/
├── components/    # Common components (Layout, Admin UI, Auth...)
├── context/       # AuthContext manages login
├── hooks/         # Custom hooks (useWebsocket, API hook...)
├── pages/         # Pages (Admin, Customer, Auth)
├── services/      # Configure Axios and WebSocket APIs
├── utils/         # Utility functions, currency formatting, date and time
└── App.jsx        # Routing and main application structure
```
