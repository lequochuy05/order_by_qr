// /static/js/dashboard.js

const urlParams = new URLSearchParams(location.search);
const tableCode = urlParams.get("tableCode");

// Lấy tableId từ localStorage (nếu có)
fetch(`${BASE_URL}/api/tables/code/${tableCode}`)
  .then(r => (r.ok ? r.json() : null))
  .then(t => {
    document.getElementById("tableNumber").textContent = t?.tableNumber ?? "—";
  })
  .catch(() => {
    document.getElementById("tableNumber").textContent = "—";
  });

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
        location.href = `/menu.html?tableCode=${encodeURIComponent(tableCode)}&categoryId=${cate.id}`;
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

// /** Slide đơn giản */
// let currentSlide = 0;
// const slides = document.querySelectorAll(".slide-item");
// if (slides.length > 0) {
//   setInterval(() => {
//     slides[currentSlide].classList.remove("active");
//     currentSlide = (currentSlide + 1) % slides.length;
//     slides[currentSlide].classList.add("active");
//   }, 5000);
// }

// Boot
document.addEventListener("DOMContentLoaded", () => {
  loadCategories();
  connectWS({ "/topic/categories": loadCategories });
});
