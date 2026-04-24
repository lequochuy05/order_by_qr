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
├── components/   # Các component dùng chung (Layout, Admin UI, Auth...)
├── context/      # AuthContext quản lý đăng nhập
├── hooks/        # Custom hooks (useWebsocket, API hooks...)
├── pages/        # Các trang (Admin, Customer, Auth)
├── services/     # Cấu hình API Axios và WebSocket
├── utils/        # Các hàm tiện ích, định dạng tiền tệ, ngày tháng
└── App.jsx       # Routing và cấu trúc chính của ứng dụng
```
