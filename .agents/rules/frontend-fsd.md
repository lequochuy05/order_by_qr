---
trigger: glob
globs: "frontend/src/**"
---

# Frontend Feature-Sliced Design (FSD) Rules (`frontend/src/**`)

Quy tắc kiến trúc này được khóa tự động bằng ESLint trong `frontend/eslint.config.js`. Vi phạm bất kỳ điều nào bên dưới sẽ làm fail `npm --prefix frontend run lint`.

## 1. Quy tắc Import Một Chiều (Strict Layer Hierarchy)

Luồng phụ thuộc duy nhất được phép đi từ trên xuống dưới:

```text
app  ->  pages  ->  widgets  ->  features  ->  entities  ->  shared
```

| Tầng | Path Alias | Được phép import từ | Cấm import từ |
|---|---|---|---|
| `app` | `@app` | `pages`, `widgets`, `features`, `entities`, `shared` | *(tầng cao nhất)* |
| `pages` | `@pages` | `widgets`, `features`, `entities`, `shared` | `@app` |
| `widgets` | `@widgets` | `widgets` (Public API), `features`, `entities`, `shared` | `@app`, `@pages` |
| `features` | `@features` | `entities`, `shared` | `@app`, `@pages`, `@widgets`, **`@features/<slice-khác>`** |
| `entities` | `@entities` | `shared` | `@app`, `@pages`, `@widgets`, `@features`, **`@entities/<slice-khác>`** |
| `shared` | `@shared` | *(nội bộ `@shared` & thư viện ngoài)* | `@app`, `@pages`, `@widgets`, `@features`, `@entities` |

## 2. Cấm Import Chéo Slice Cùng Tầng (`entities` & `features`)

- Một slice trong `src/entities/<slice>` **tuyệt đối không** import từ bất kỳ entity slice khác (`@entities/<other>`).
- Một slice trong `src/features/<slice>` **tuyệt đối không** import từ bất kỳ feature slice khác (`@features/<other>`).
- Khi cần ghép hoặc điều phối nhiều `features` / `entities` với nhau, đưa phần ghép nối lên tầng `widgets/` hoặc `pages/`.

## 3. Layer Aliases & Public API Barrel (`index.js`)

- **Cấm deep relative imports (`../../*` trở lên):** Luôn dùng alias đã cấu hình trong `frontend/vite.config.js`: `@app`, `@pages`, `@widgets`, `@features`, `@entities`, `@shared`.
- **Public API Barrel bắt buộc:** Khi import từ một slice bên ngoài thuộc `@entities/*`, `@features/*`, hoặc `@widgets/*`, **chỉ được import từ root barrel** (ví dụ `@entities/order`, `@features/auth`, `@widgets/admin-layout`), **cấm** trọc sâu vào đường dẫn nội bộ (`@entities/order/model/...`).
- Chỉ được phép import đường dẫn sâu bên trong **chính slice hiện tại** (intra-slice import, ví dụ trong `features/settings-management` được import `@features/settings-management/lib/...`).
- Mọi slice mới tạo trong `entities/`, `features/`, `widgets/` phải có file `index.js` export rõ ràng Public API.

## 4. UI Tokens & i18n

- **Design Tokens & Light/Dark:** Dùng semantic theme tokens trong `frontend/src/app/styles/index.css` (`@theme`, `:root`, `.dark`) hỗ trợ đầy đủ Light/Dark đạt tương phản WCAG AA; **không hardcode mã màu hex** trong component. Ưu tiên tái sử dụng component & style chuẩn trong `@shared/ui` (kiểm tra tự động một phần qua `frontend/tests/unit/designSystem.test.js`).
- **Song ngữ i18n (`VI`/`EN`):** Mọi chuỗi hiển thị mới cho người dùng bắt buộc đi qua hook `useTranslation` từ `@shared/lib/i18n` và cập nhật đồng bộ đủ cả 2 từ điển `frontend/src/shared/lib/i18n/locales/vi.js` và `frontend/src/shared/lib/i18n/locales/en.js`.

## 5. Kiểm chứng Bắt buộc trước khi Hoàn tất

Sau khi sửa code trong `frontend/src/**`, chạy đủ:
- `npm --prefix frontend run lint` (xác minh FSD layer, cross-slice, Public API & React hooks)
- `npm --prefix frontend test` (`node tests/run-tests.js`)
- `npm --prefix frontend run build`
