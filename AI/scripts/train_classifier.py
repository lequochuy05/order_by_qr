import os
import shutil
from pathlib import Path

# CẤU HÌNH
CONFIG = {
    "roboflow_api_key": "tFdV1c98yEO7oJm1R7l0", 
    "roboflow_workspace": "wuchuyworkspace",
    "roboflow_project": "order_by_qr",
    "roboflow_version": 1,
    "model_size": "yolov8n-cls",
    "epochs": 100,
    "img_size": 224,
    "batch_size": 32,
    "output_dir": "runs/classify",
    "export_dir": "/content/exported_model", 
}

def install_dependencies():
    print("Đang cài đặt thư viện...")
    os.system("pip install -q ultralytics roboflow")
    print("Đã cài đặt dependencies")

def prepare_dataset():
    from roboflow import Roboflow
    rf = Roboflow(api_key=CONFIG["roboflow_api_key"])
    project = rf.workspace(CONFIG["roboflow_workspace"]).project(CONFIG["roboflow_project"])
    dataset = project.version(CONFIG["roboflow_version"]).download("folder")

    old_dir = dataset.location
    new_dir = '/content/final_classification_data'
    if os.path.exists(new_dir): shutil.rmtree(new_dir)

    for root, dirs, files in os.walk(old_dir):
        for file in files:
            if file.lower().endswith(('.jpg', '.jpeg', '.png')):
                class_name = os.path.basename(root)
                if class_name == os.path.basename(old_dir): continue
                for split in ['train', 'val']:
                    dest = os.path.join(new_dir, split, class_name)
                    os.makedirs(dest, exist_ok=True)
                    shutil.copy(os.path.join(root, file), os.path.join(dest, file))
    return new_dir
    
def train_model(dataset_path):
    from ultralytics import YOLO
    model = YOLO(CONFIG["model_size"] + ".pt")
    
    # Chỉnh lại: chỉ để project='runs', YOLO sẽ tự thêm /classify/dish_classifier
    model.train(
        data=dataset_path,
        epochs=CONFIG["epochs"],
        imgsz=CONFIG["img_size"],
        batch=CONFIG["batch_size"],
        project="runs", 
        name="dish_classifier",
        device=0
    )
    
    # Tìm kiếm file best.pt một cách linh hoạt
    import glob
    files = glob.glob("**/dish_classifier/weights/best.pt", recursive=True)
    if files:
        return os.path.abspath(files[0])
    else:
        raise FileNotFoundError("Không tìm thấy file best.pt! Hãy kiểm tra lại thư mục runs.")

def export_to_frontend(model_path):
    from ultralytics import YOLO
    import json
    
    print(f"Đang làm việc với file: {model_path}")
    model = YOLO(model_path)
    
    # Export sang TF.js
    # YOLO sẽ trả về đường dẫn folder chứa model.json
    export_folder = model.export(format="tfjs", imgsz=CONFIG["img_size"])
    
    # Nén và sẵn sàng tải về
    zip_name = "/content/model_for_frontend"
    # export_folder lúc này là một chuỗi đường dẫn, cần đảm bảo nó tồn tại
    if os.path.exists(export_folder):
        shutil.make_archive(zip_name, 'zip', export_folder)
        print(f"✅ Đã đóng gói model tại: {zip_name}.zip")
    else:
        print(f"❌ Lỗi: Không thấy folder export tại {export_folder}")

# MAIN
if __name__ == "__main__":
    install_dependencies()
    path = prepare_dataset()
    best_pt = train_model(path)
    export_to_frontend(best_pt)
    print("XONG! Hãy tải file zip ở mục file bên trái về.")