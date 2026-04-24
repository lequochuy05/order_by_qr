# 🍜 Quản lý tài nguyên AI - Order By QR

Thư mục này chứa toàn bộ các tài sản liên quan đến AI của dự án, tách biệt khỏi logic nghiệp vụ chính.

## Cấu trúc thư mục

```
AI/
├── models/
│   └── dish-classifier/       ← Model đã train
│       ├── model.json         ← TF.js graph definition
│       ├── group1-shard*.bin  ← Model weights
│       └── labels.json        ← Mapping: {0: "Phở Bò", 1: "Cơm Tấm", ...}
├── scripts/
│   ├── requirements.txt       ← Python dependencies
│   ├── train_classifier.py    ← Script huấn luyện (chạy trên Colab)
│   └── export_to_tfjs.py      ← Convert model → TF.js
├── data/                      ← Ảnh training (không commit lên git)
└── readme.md
```

## Quy trình Training AI (Step-by-step)

### Bước 1: Thu thập dữ liệu
1. Chụp ảnh mỗi món ăn trong menu (tối thiểu **30-50 ảnh/món**)
2. Đa dạng góc chụp: trên xuống, nghiêng, zoom in/out
3. Đa dạng điều kiện: ánh sáng tự nhiên, đèn vàng, flash
4. Tổ chức vào `/data`:
   ```
   data/
   ├── pho_bo/        ← Tên thư mục = tên class (không dấu)
   │   ├── img001.jpg
   │   ├── img002.jpg
   ├── com_tam/
   │   ├── img001.jpg
   └── tra_sua/
       ├── img001.jpg
   ```

### Bước 2: Gán nhãn trên Roboflow (khuyến nghị)
1. Tạo tài khoản [Roboflow](https://roboflow.com) (free)
2. Tạo project mới → Classification task
3. Upload ảnh và gán nhãn
4. Roboflow tự động: resize, augmentation (xoay, flip, brightness)
5. Export → lấy API key & project info

### Bước 3: Huấn luyện
```bash
# Trên Google Colab (free GPU)
# 1. Upload train_classifier.py lên Colab
# 2. Cấu hình Roboflow API key trong script
# 3. Chạy:
python train_classifier.py
```

**Hoặc không dùng Roboflow:**
```bash
# Tổ chức ảnh vào thư mục dataset/ theo format ở Bước 1
# Script sẽ hỏi đường dẫn khi chạy
python train_classifier.py
```

### Bước 4: Export sang TF.js
```bash
python export_to_tfjs.py
```

### Bước 5: Deploy vào Frontend
```bash
# Copy model files vào Frontend
cp -r AI/models/dish-classifier/* frontend/public/models/dish-classifier/

# Build & test
cd frontend && npm run dev
```

## Yêu cầu phần cứng

| Môi trường | GPU | Thời gian (50 epochs, ~500 ảnh) |
|-----------|-----|------|
| **Google Colab (free)** | T4 | ~10-15 phút |
| **Colab Pro** | A100 | ~3-5 phút |
| **Local (RTX 3060+)** | RTX | ~5-10 phút |
| **Local (CPU only)** | ❌ | ~1-2 giờ (không khuyến nghị) |

## Tips để model chính xác hơn

1. **Nhiều ảnh hơn = chính xác hơn**. Tối thiểu 30 ảnh/món, lý tưởng 100+
2. **Augmentation**: Roboflow tự động augment (flip, rotate, brightness)
3. **Tăng epochs**: Nếu accuracy < 80%, tăng lên 100-200 epochs
4. **Model lớn hơn**: Đổi `yolov8n-cls` → `yolov8s-cls` (11MB vs 3.5MB)
5. **Lưu ý**: Model càng lớn → load trên browser càng chậm

---
*⚠️ Luôn sao lưu mô hình cũ trong `/models/backups` trước khi ghi đè.*
