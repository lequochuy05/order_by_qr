"""
 YOLOv8 Classification Training Script - Order By QR
 Chạy trên Google Colab (free GPU) hoặc máy local có GPU

Hướng dẫn chạy trên Google Colab:
1. Upload file này lên Colab
2. Đổi Runtime → GPU (T4)
3. Chạy từng cell hoặc chạy toàn bộ script

Hướng dẫn chạy local:
$ pip install -r requirements.txt
$ python train_classifier.py
"""

import os
import shutil
from pathlib import Path
from dotenv import load_dotenv

# Tải biến môi trường từ file .env (nếu có)
load_dotenv()

# CẤU HÌNH
CONFIG = {
    # Roboflow - Lấy từ biến môi trường để bảo mật
    "roboflow_api_key": os.getenv("ROBOFLOW_API_KEY"),
    "roboflow_workspace": os.getenv("ROBOFLOW_WORKSPACE"),
    "roboflow_project": os.getenv("ROBOFLOW_PROJECT"),
    "roboflow_version": int(os.getenv("ROBOFLOW_VERSION", 2)),

    # Training
    "model_size": "yolov8n-cls",    
    "epochs": 100,                  
    "img_size": 224,               
    "batch_size": 32,              

    # Output
    "output_dir": "runs/classify",
    "export_dir": "../../frontend/public/models/dish-classifier",
}

# CÀI ĐẶT THƯ VIỆN

def install_dependencies():
    """Cài đặt thư viện cần thiết (dành cho Colab)."""
    os.system("pip install -q ultralytics roboflow")
    print(" Đã cài đặt dependencies")

# TẢI VÀ CẤU TRÚC LẠI DỮ LIỆU 

def prepare_dataset():
    from roboflow import Roboflow
    import os
    import shutil

    if not CONFIG["roboflow_api_key"]:
        raise ValueError("Thiếu ROBOFLOW_API_KEY trong file .env! Hãy kiểm tra lại.")

    # Tải từ Roboflow
    rf = Roboflow(api_key=CONFIG["roboflow_api_key"])
    project = rf.workspace(CONFIG["roboflow_workspace"]).project(CONFIG["roboflow_project"])
    dataset = project.version(CONFIG["roboflow_version"]).download("folder")
    
    old_dir = dataset.location
    new_dir = os.path.join(os.getcwd(), 'final_classification_data')

    if os.path.exists(new_dir): shutil.rmtree(new_dir)
    
    print(f"Đang cấu trúc lại dữ liệu cho YOLOv8-cls tại: {new_dir}")

    # Gom ảnh vào train/val theo cấu trúc YOLOv8-cls (giống logic Colab của bạn)
    for root, dirs, files in os.walk(old_dir):
        for file in files:
            if file.lower().endswith(('.jpg', '.jpeg', '.png')):
                class_name = os.path.basename(root)
                if class_name == os.path.basename(old_dir): continue
                
                for split in ['train', 'val']:
                    dest = os.path.join(new_dir, split, class_name)
                    os.makedirs(dest, exist_ok=True)
                    shutil.copy(os.path.join(root, file), os.path.join(dest, file))

    print(f"Dữ liệu đã sẵn sàng!")
    return new_dir

# HUẤN LUYỆN MODEL

def train_model(dataset_path):
    from ultralytics import YOLO

    model = YOLO(CONFIG["model_size"] + ".pt")

    model.train(
        data=dataset_path,
        epochs=CONFIG["epochs"],
        imgsz=CONFIG["img_size"],
        batch=CONFIG["batch_size"],
        project=CONFIG["output_dir"],
        name="dish_classifier",
        device=0 if os.name != 'nt' else 'cpu' # Auto-detect GPU
    )

    best_model_path = Path(CONFIG["output_dir"]) / "dish_classifier" / "weights" / "best.pt"
    print(f"Training hoàn tất tại: {best_model_path}")

    return str(best_model_path)

# EXPORT THẲNG SANG TF.JS 

def export_to_frontend(model_path):
    from ultralytics import YOLO
    import json
    import os
    import shutil

    model = YOLO(model_path)

    # Export trực tiếp sang TF.js (YOLOv8 hỗ trợ cực tốt)
    print("Đang export model thẳng sang định dạng TF.js...")
    # Quantize=True để model nhẹ nhõm cho browser
    export_folder = model.export(format="tfjs", imgsz=CONFIG["img_size"], int8=True)
    
    # YOLO thường tạo folder có tên 'best_web_model' hoặc tương tự bên trong folder weights
    source_dir = Path(export_folder)
    dest_dir = Path(CONFIG["export_dir"])
    dest_dir.mkdir(parents=True, exist_ok=True)

    print(f"Đang đồng bộ model vào Frontend: {dest_dir}")
    
    # Copy các file .json và .bin
    for file in os.listdir(source_dir):
        if file.endswith(('.json', '.bin')):
            shutil.copy(os.path.join(source_dir, file), os.path.join(dest_dir, file))

    # Tạo lại labels.json từ thực tế model vừa dạy
    class_names = model.names
    labels = {str(idx): name.replace("_", " ").title() for idx, name in class_names.items()}
    
    with open(dest_dir / "labels.json", "w", encoding="utf-8") as f:
        json.dump(labels, f, ensure_ascii=False, indent=2)

    print(f"Hoàn tất! Model và labels.json đã nằm gọn tại {dest_dir}")

# MAIN 

def main():
    print("=" * 60)
    print("AI Dish Classifier Training")
    print("=" * 60)

    # Step 1: Cài đặt (nếu chạy trên Colab)
    if 'google.colab' in str(get_ipython()):
        install_dependencies()

    # Step 2 & 3: Chuẩn bị dữ liệu (Gom ảnh)
    dataset_path = prepare_dataset()

    # Step 4: Huấn luyện
    model_path = train_model(dataset_path)

    # Step 5: Export & Deploy
    export_to_frontend(model_path)

    print("\n" + "=" * 60)
    print(" HOÀN TẤT!")
    print(" Website của bạn đã được cập nhật model mới.")
    print("=" * 60)


if __name__ == "__main__":
    from IPython import get_ipython
    main()
