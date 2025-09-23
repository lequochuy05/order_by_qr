// /static/js/dashboard.js
const BASE_URL = window.APP_BASE_URL || location.origin;

const urlParams = new URLSearchParams(location.search);
const tableId = urlParams.get("tableId");

// Gán số bàn (không crash khi lỗi)
fetch(`${BASE_URL}/api/tables/${tableId}`)
  .then(r => (r.ok ? r.json() : null))
  .then(t => {
    document.getElementById("tableNumber").textContent = t?.tableNumber ?? tableId ?? "";
  })
  .catch(() => {
    document.getElementById("tableNumber").textContent = tableId ?? "";
  });

/** Chuẩn hoá đường dẫn ảnh (hỗ trợ /uploads, uploads, http(s)://) */
function toImgUrl(u = "") {
  try {
    if (!u) return "";
    if (u.startsWith("http://") || u.startsWith("https://")) return u;
    if (u.startsWith("/")) return new URL(u, BASE_URL).href;
    return new URL("/" + u, BASE_URL).href;
  } catch {
    return "";
  }
}

/** Tải & render danh mục */
async function loadCategories() {
  const container = document.getElementById("categoryList");
  container.innerHTML = "";

  try {
    const res = await fetch(`${BASE_URL}/api/categories`);
    const data = res.ok ? await res.json() : [];

    data.forEach(cate => {
      const div = document.createElement("div");
      div.className = "category";
      div.onclick = () => {
        location.href = `/menu.html?tableId=${encodeURIComponent(tableId)}&categoryId=${cate.id}`;
      };

      // Cache-busting nhỏ để tránh dính cache 404 ngay sau upload
      const imgUrl = toImgUrl(cate.img || "");
      const src = imgUrl ? `${imgUrl}${imgUrl.includes("?") ? "&" : "?"}v=${Date.now()}` : "";

      div.innerHTML = `
        <img src="${src}" alt="${(cate.name || "").replace(/"/g, "&quot;")}" loading="lazy">
        <div>${cate.name || ""}</div>
      `;
      container.appendChild(div);
    });
  } catch {
    container.innerHTML = `<div style="padding:12px;color:#e11d48">Không tải được danh mục</div>`;
  }
}

/** Slide đơn giản */
let currentSlide = 0;
const slides = document.querySelectorAll(".slide-item");
if (slides.length > 0) {
  setInterval(() => {
    slides[currentSlide].classList.remove("active");
    currentSlide = (currentSlide + 1) % slides.length;
    slides[currentSlide].classList.add("active");
  }, 5000);
}

/** WebSocket: nghe thay đổi danh mục và reload */
function connectWS() {
  try {
    const socket = new SockJS(`${BASE_URL}/ws`);
    const stomp = Stomp.over(socket);
    stomp.debug = () => {};

    let retry = 0;
    const reconnect = () =>
      setTimeout(
        () => connectWS(),
        Math.min(30000, 1000 * Math.pow(2, Math.min(6, ++retry)))
      );

    stomp.connect(
      {},
      () => {
        retry = 0;
        // Delay nhẹ 250ms để chắc backend commit xong và file đã sẵn sàng
        stomp.subscribe("/topic/categories", () => setTimeout(loadCategories, 250));
      },
      reconnect
    );
  } catch {
    // bỏ qua
  }
}

// Boot
document.addEventListener("DOMContentLoaded", () => {
  loadCategories();
  connectWS();
});
