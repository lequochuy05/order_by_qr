// ==============================================================================
// QROS MASTER DESIGN SYSTEM — FIGMA PLUGIN CODE (COMPREHENSIVE EDITION)
// Target: Run as Figma Plugin
// ==============================================================================

(async function generateComprehensiveDesignSystem() {
  function hexToRgb(hex, opacity = 1) {
    hex = hex.replace("#", "");
    if (hex.length === 3) hex = hex.split("").map((c) => c + c).join("");
    const num = parseInt(hex, 16);
    return {
      type: "SOLID",
      color: {
        r: ((num >> 16) & 255) / 255,
        g: ((num >> 8) & 255) / 255,
        b: (num & 255) / 255,
      },
      opacity: opacity,
    };
  }

  function createDropShadow(y = 4, blur = 12, opacity = 0.08, colorHex = "#0F172A") {
    const rgb = hexToRgb(colorHex).color;
    return {
      type: "DROP_SHADOW",
      color: { ...rgb, a: opacity },
      offset: { x: 0, y: y },
      radius: blur,
      spread: 0,
      visible: true,
      blendMode: "NORMAL",
    };
  }

  const primaryFontFamily = "Be Vietnam Pro";
  const fallbackFontFamily = "Inter";

  async function loadFontWithFallback(style) {
    try {
      await figma.loadFontAsync({ family: primaryFontFamily, style: style });
      return { family: primaryFontFamily, style: style };
    } catch {
      try {
        await figma.loadFontAsync({ family: fallbackFontFamily, style: style === "Black" || style === "ExtraBold" ? "Bold" : style });
        return { family: fallbackFontFamily, style: style === "Black" || style === "ExtraBold" ? "Bold" : style };
      } catch {
        await figma.loadFontAsync({ family: "Roboto", style: "Regular" });
        return { family: "Roboto", style: "Regular" };
      }
    }
  }

  const fontRegular = await loadFontWithFallback("Regular");
  const fontMedium = await loadFontWithFallback("Medium");
  const fontBold = await loadFontWithFallback("Bold");
  const fontExtraBold = await loadFontWithFallback("ExtraBold");
  const fontBlack = await loadFontWithFallback("Black");

  function makeText(str, font, size, fillHex = "#0F172A", opacity = 1) {
    const t = figma.createText();
    t.fontName = font;
    t.fontSize = size;
    t.characters = str || "";
    t.fills = [hexToRgb(fillHex, opacity)];
    return t;
  }

  const root = figma.createFrame();
  root.name = "📐 QROS Master Design System — Production Kit (v3.0)";
  root.fills = [hexToRgb("#F8FAFC")];
  root.layoutMode = "VERTICAL";
  root.primaryAxisSizingMode = "AUTO";
  root.counterAxisSizingMode = "FIXED";
  root.resize(1580, 5200);
  root.paddingTop = 64;
  root.paddingBottom = 100;
  root.paddingLeft = 72;
  root.paddingRight = 72;
  root.itemSpacing = 56;
  root.cornerRadius = 24;

  // Header Banner
  const header = figma.createFrame();
  header.name = "Hero Header";
  header.layoutMode = "VERTICAL";
  header.fills = [hexToRgb("#6366F1")];
  header.cornerRadius = 28;
  header.paddingTop = 48;
  header.paddingBottom = 48;
  header.paddingLeft = 56;
  header.paddingRight = 56;
  header.itemSpacing = 16;
  header.layoutAlign = "STRETCH";
  header.effects = [createDropShadow(16, 32, 0.25, "#6366F1")];

  const badgeHeader = figma.createFrame();
  badgeHeader.layoutMode = "HORIZONTAL";
  badgeHeader.paddingTop = 6;
  badgeHeader.paddingBottom = 6;
  badgeHeader.paddingLeft = 14;
  badgeHeader.paddingRight = 14;
  badgeHeader.cornerRadius = 9999;
  badgeHeader.fills = [hexToRgb("#FFFFFF", 0.18)];
  badgeHeader.appendChild(makeText("ENTERPRISE POS & KDS DESIGN SYSTEM • V3.0", fontExtraBold, 11, "#FFFFFF"));
  header.appendChild(badgeHeader);

  header.appendChild(makeText("QROS DESIGN SYSTEM", fontBlack, 38, "#FFFFFF"));
  header.appendChild(
    makeText(
      "Bộ thiết kế quy chuẩn toàn diện cho Nhà hàng & F&B SaaS: Bàn ăn (Table Grid), Gọi món (Menu Order), Bếp (KDS Kanban), Quản trị & Doanh thu (Analytics).",
      fontMedium,
      16,
      "#EEF2FF",
      0.95
    )
  );
  root.appendChild(header);

  function createSection(title, subtitle) {
    const sec = figma.createFrame();
    sec.name = `Section - ${title}`;
    sec.layoutMode = "VERTICAL";
    sec.fills = [];
    sec.itemSpacing = 24;
    sec.layoutAlign = "STRETCH";

    const titleBox = figma.createFrame();
    titleBox.layoutMode = "VERTICAL";
    titleBox.itemSpacing = 4;
    titleBox.fills = [];

    titleBox.appendChild(makeText(title, fontBlack, 24, "#0F172A"));
    if (subtitle) {
      titleBox.appendChild(makeText(subtitle, fontMedium, 14, "#64748B"));
    }
    sec.appendChild(titleBox);
    return sec;
  }

  // 1. COLORS
  const colorSec = createSection("1. Hệ Thống Màu Đầy Đủ (Color Palettes & Tints)", "Các dải màu đồng bộ từ 50 đến 950 hỗ trợ WCAG 2.1 AA Contrast");
  const palettes = [
    {
      name: "Electric Indigo (Primary Brand)",
      shades: [
        { level: "50", hex: "#EEF2FF", dark: false },
        { level: "100", hex: "#E0E7FF", dark: false },
        { level: "200", hex: "#C7D2FE", dark: false },
        { level: "300", hex: "#A5B4FC", dark: false },
        { level: "400", hex: "#818CF8", dark: true },
        { level: "500", hex: "#6366F1", dark: true, note: "★ Brand Main" },
        { level: "600", hex: "#4F46E5", dark: true, note: "Hover" },
        { level: "700", hex: "#4338CA", dark: true },
        { level: "800", hex: "#3730A3", dark: true },
        { level: "900", hex: "#312E81", dark: true },
      ],
    },
    {
      name: "Royal Violet (Secondary Accent)",
      shades: [
        { level: "50", hex: "#F5F3FF", dark: false },
        { level: "100", hex: "#EDE9FE", dark: false },
        { level: "200", hex: "#DDD6FE", dark: false },
        { level: "300", hex: "#C4B5FD", dark: false },
        { level: "400", hex: "#A78BFA", dark: true },
        { level: "500", hex: "#8B5CF6", dark: true, note: "★ Secondary" },
        { level: "600", hex: "#7C3AED", dark: true },
        { level: "700", hex: "#6D28D9", dark: true },
        { level: "800", hex: "#5B21B6", dark: true },
        { level: "900", hex: "#4C1D95", dark: true },
      ],
    },
    {
      name: "Slate Neutrals (Surface & Ink Foundations)",
      shades: [
        { level: "50", hex: "#F8FAFC", dark: false, note: "Light Canvas" },
        { level: "100", hex: "#F1F5F9", dark: false, note: "Card Hover" },
        { level: "200", hex: "#E2E8F0", dark: false, note: "Border" },
        { level: "300", hex: "#CBD5E1", dark: false },
        { level: "400", hex: "#94A3B8", dark: false, note: "Muted" },
        { level: "500", hex: "#64748B", dark: true },
        { level: "600", hex: "#475569", dark: true, note: "Secondary" },
        { level: "700", hex: "#334155", dark: true },
        { level: "800", hex: "#1E293B", dark: true, note: "Dark Border" },
        { level: "900", hex: "#0F172A", dark: true, note: "Headings" },
        { level: "950", hex: "#0B0F17", dark: true, note: "Dark Canvas" },
      ],
    },
    {
      name: "Semantic Status Systems (Emerald, Amber, Rose, Sky)",
      shades: [
        { level: "Emerald 500", hex: "#10B981", dark: true, note: "Success / Paid" },
        { level: "Emerald 50", hex: "#ECFDF5", dark: false, note: "Success Bg" },
        { level: "Amber 500", hex: "#F59E0B", dark: true, note: "Warning / Wait" },
        { level: "Amber 50", hex: "#FFFBEB", dark: false, note: "Warning Bg" },
        { level: "Rose 500", hex: "#F43F5E", dark: true, note: "Danger / Alert" },
        { level: "Rose 50", hex: "#FEF2F2", dark: false, note: "Danger Bg" },
        { level: "Sky 500", hex: "#0EA5E9", dark: true, note: "Info / Tech" },
        { level: "Sky 50", hex: "#F0F9FF", dark: false, note: "Info Bg" },
      ],
    },
  ];

  for (const pal of palettes) {
    const pBox = figma.createFrame();
    pBox.layoutMode = "VERTICAL";
    pBox.itemSpacing = 10;
    pBox.fills = [];
    pBox.layoutAlign = "STRETCH";

    pBox.appendChild(makeText(pal.name, fontBold, 15, "#1E293B"));
    const swatches = figma.createFrame();
    swatches.layoutMode = "HORIZONTAL";
    swatches.itemSpacing = 10;
    swatches.fills = [];

    for (const sh of pal.shades) {
      const sw = figma.createFrame();
      sw.name = `${pal.name} ${sh.level}`;
      sw.layoutMode = "VERTICAL";
      sw.primaryAxisAlignItems = "SPACE_BETWEEN";
      sw.resize(120, 96);
      sw.cornerRadius = 14;
      sw.paddingTop = 10; sw.paddingBottom = 10; sw.paddingLeft = 12; sw.paddingRight = 12;
      sw.fills = [hexToRgb(sh.hex)];
      sw.strokes = [hexToRgb("#E2E8F0")];
      sw.strokeWeight = 1;

      const txtColor = sh.dark ? "#FFFFFF" : "#0F172A";
      sw.appendChild(makeText(sh.level, fontBold, 13, txtColor));

      const bot = figma.createFrame();
      bot.layoutMode = "VERTICAL";
      bot.itemSpacing = 2;
      bot.fills = [];
      bot.appendChild(makeText(sh.hex, fontMedium, 11, txtColor, 0.8));
      if (sh.note) {
        bot.appendChild(makeText(sh.note, fontExtraBold, 9, sh.dark ? "#FDE047" : "#4338CA"));
      }
      sw.appendChild(bot);
      swatches.appendChild(sw);
    }
    pBox.appendChild(swatches);
    colorSec.appendChild(pBox);
  }
  root.appendChild(colorSec);

  // 2. TYPOGRAPHY
  const typoSec = createSection("2. Hệ Thống Kiểu Chữ (Typography Hierarchy)", "Font chính: Be Vietnam Pro (Hỗ trợ 100% tiếng Việt, optical tracking chuẩn)");
  const typoBox = figma.createFrame();
  typoBox.layoutMode = "VERTICAL";
  typoBox.fills = [hexToRgb("#FFFFFF")];
  typoBox.strokes = [hexToRgb("#E2E8F0")];
  typoBox.strokeWeight = 1;
  typoBox.cornerRadius = 24;
  typoBox.paddingTop = 28; typoBox.paddingBottom = 28; typoBox.paddingLeft = 36; typoBox.paddingRight = 36;
  typoBox.itemSpacing = 20;
  typoBox.layoutAlign = "STRETCH";

  const typeItems = [
    { name: "Display Hero", font: fontBlack, size: 36, lh: 44, sample: "Hệ Thống Quản Lý Gọi Món Nhà Hàng QROS" },
    { name: "Page Header", font: fontExtraBold, size: 28, lh: 36, sample: "Tổng quan vận hành & Báo cáo doanh thu" },
    { name: "Section Title", font: fontBold, size: 20, lh: 28, sample: "Sơ đồ trạng thái bàn ăn (Tầng 1 & VIP)" },
    { name: "Card Heading", font: fontBold, size: 16, lh: 24, sample: "Bàn số 04 • 4 Khách • Gọi món lúc 18:30" },
    { name: "Body Standard", font: fontMedium, size: 14, lh: 20, sample: "Bò Fuji áp chảo sốt tiêu đen, thêm khoai tây nghiền và nước ép bưởi hồng." },
    { name: "Body Small", font: fontMedium, size: 12, lh: 16, sample: "Cập nhật 2 phút trước bởi Thu ngân: Nguyễn Văn A (Mã đơn: #10492)" },
    { name: "Micro Tag / Pill", font: fontExtraBold, size: 11, lh: 14, sample: "ĐANG NẤU • PHỤC VỤ NGAY • VIP AREA" },
    { name: "Financial Metric", font: fontBlack, size: 28, lh: 34, sample: "45.890.000 ₫" },
  ];

  for (const t of typeItems) {
    const row = figma.createFrame();
    row.layoutMode = "HORIZONTAL";
    row.layoutAlign = "STRETCH";
    row.primaryAxisAlignItems = "SPACE_BETWEEN";
    row.counterAxisAlignItems = "CENTER";
    row.fills = [];

    const left = figma.createFrame();
    left.layoutMode = "VERTICAL";
    left.itemSpacing = 4;
    left.fills = [];

    left.appendChild(makeText(t.sample, t.font, t.size, "#0F172A"));
    row.appendChild(left);
    row.appendChild(makeText(`${t.name} • ${t.size}px / Line ${t.lh}px`, fontMedium, 12, "#94A3B8"));
    typoBox.appendChild(row);
  }
  typoSec.appendChild(typoBox);
  root.appendChild(typoSec);

  // 3. TOKENS: RADIUS & SHADOWS
  const tokenSec = createSection("3. Bo Góc, Đổ Bóng & Khoảng Cách (Radius, Elevation, Spacing)");
  const tokenGrid = figma.createFrame();
  tokenGrid.layoutMode = "HORIZONTAL";
  tokenGrid.itemSpacing = 24;
  tokenGrid.fills = [];
  tokenGrid.layoutAlign = "STRETCH";

  const radCard = figma.createFrame();
  radCard.layoutMode = "VERTICAL";
  radCard.itemSpacing = 16;
  radCard.paddingTop = 24; radCard.paddingBottom = 24; radCard.paddingLeft = 24; radCard.paddingRight = 24;
  radCard.cornerRadius = 20;
  radCard.fills = [hexToRgb("#FFFFFF")];
  radCard.strokes = [hexToRgb("#E2E8F0")];
  radCard.resize(460, 240);
  radCard.appendChild(makeText("Border Radius System", fontBold, 15, "#0F172A"));

  const radRow = figma.createFrame();
  radRow.layoutMode = "HORIZONTAL";
  radRow.itemSpacing = 14;
  radRow.fills = [];

  const radii = [
    { name: "lg (8px)", r: 8 },
    { name: "xl (12px)", r: 12 },
    { name: "2xl (16px)", r: 16 },
    { name: "3xl (24px)", r: 24 },
    { name: "full", r: 9999 },
  ];

  for (const rd of radii) {
    const box = figma.createFrame();
    box.resize(72, 72);
    box.cornerRadius = rd.r;
    box.fills = [hexToRgb("#EEF2FF")];
    box.strokes = [hexToRgb("#6366F1")];
    box.strokeWeight = 1.5;
    box.layoutMode = "VERTICAL";
    box.primaryAxisAlignItems = "CENTER";
    box.counterAxisAlignItems = "CENTER";
    box.appendChild(makeText(rd.name.split(" ")[0], fontBold, 11, "#4338CA"));
    radRow.appendChild(box);
  }
  radCard.appendChild(radRow);
  tokenGrid.appendChild(radCard);

  const elCard = figma.createFrame();
  elCard.layoutMode = "VERTICAL";
  elCard.itemSpacing = 16;
  elCard.paddingTop = 24; elCard.paddingBottom = 24; elCard.paddingLeft = 24; elCard.paddingRight = 24;
  elCard.cornerRadius = 20;
  elCard.fills = [hexToRgb("#FFFFFF")];
  elCard.strokes = [hexToRgb("#E2E8F0")];
  elCard.resize(460, 240);
  elCard.appendChild(makeText("Elevation & Shadows", fontBold, 15, "#0F172A"));

  const elRow = figma.createFrame();
  elRow.layoutMode = "HORIZONTAL";
  elRow.itemSpacing = 16;
  elRow.fills = [];

  const shadows = [
    { name: "Default (Sm)", fx: createDropShadow(2, 6, 0.06) },
    { name: "Hover (Xl)", fx: createDropShadow(12, 28, 0.12) },
    { name: "Brand Glow", fx: createDropShadow(8, 20, 0.3, "#6366F1") },
  ];

  for (const sh of shadows) {
    const b = figma.createFrame();
    b.resize(116, 80);
    b.cornerRadius = 16;
    b.fills = [hexToRgb("#FFFFFF")];
    b.strokes = [hexToRgb("#E2E8F0")];
    b.strokeWeight = 1;
    b.effects = [sh.fx];
    b.layoutMode = "VERTICAL";
    b.primaryAxisAlignItems = "CENTER";
    b.counterAxisAlignItems = "CENTER";
    b.appendChild(makeText(sh.name, fontBold, 12, "#1E293B"));
    elRow.appendChild(b);
  }
  elCard.appendChild(elRow);
  tokenGrid.appendChild(elCard);
  tokenSec.appendChild(tokenGrid);
  root.appendChild(tokenSec);

  // 4. BUTTONS & INPUTS
  const compSec = createSection("4. Nút Bấm & Thành Phần Nhập Liệu (Buttons & Inputs)", "Bao gồm tất cả biến thể nút: Primary, Secondary, Ghost, Danger và trạng thái Focus input");
  const btnGrid = figma.createFrame();
  btnGrid.layoutMode = "HORIZONTAL";
  btnGrid.itemSpacing = 24;
  btnGrid.fills = [];
  btnGrid.layoutAlign = "STRETCH";

  const btnFrame = figma.createFrame();
  btnFrame.layoutMode = "VERTICAL";
  btnFrame.itemSpacing = 16;
  btnFrame.paddingTop = 24; btnFrame.paddingBottom = 24; btnFrame.paddingLeft = 24; btnFrame.paddingRight = 24;
  btnFrame.cornerRadius = 24;
  btnFrame.fills = [hexToRgb("#FFFFFF")];
  btnFrame.strokes = [hexToRgb("#E2E8F0")];
  btnFrame.resize(460, 360);
  btnFrame.appendChild(makeText("Button Variants & States", fontBold, 15, "#0F172A"));

  function makeButton(label, bgHex, textHex, borderHex = null) {
    const btn = figma.createFrame();
    btn.layoutMode = "HORIZONTAL";
    btn.primaryAxisAlignItems = "CENTER";
    btn.counterAxisAlignItems = "CENTER";
    btn.itemSpacing = 8;
    btn.paddingTop = 12; btn.paddingBottom = 12; btn.paddingLeft = 20; btn.paddingRight = 20;
    btn.cornerRadius = 14;
    btn.fills = [hexToRgb(bgHex)];
    if (borderHex) {
      btn.strokes = [hexToRgb(borderHex)];
      btn.strokeWeight = 1.5;
    }
    btn.appendChild(makeText(label, fontBold, 14, textHex));
    return btn;
  }

  const bRow1 = figma.createFrame();
  bRow1.layoutMode = "HORIZONTAL";
  bRow1.itemSpacing = 12;
  bRow1.fills = [];
  bRow1.appendChild(makeButton("Xác nhận gọi món", "#6366F1", "#FFFFFF"));
  bRow1.appendChild(makeButton("Thanh toán (Cash)", "#8B5CF6", "#FFFFFF"));
  btnFrame.appendChild(bRow1);

  const bRow2 = figma.createFrame();
  bRow2.layoutMode = "HORIZONTAL";
  bRow2.itemSpacing = 12;
  bRow2.fills = [];
  bRow2.appendChild(makeButton("Ghép / Chuyển bàn", "#EEF2FF", "#4338CA", "#C7D2FE"));
  bRow2.appendChild(makeButton("Huỷ món ăn", "#FEF2F2", "#B91C1C", "#FECDD3"));
  btnFrame.appendChild(bRow2);

  const bRow3 = figma.createFrame();
  bRow3.layoutMode = "HORIZONTAL";
  bRow3.itemSpacing = 12;
  bRow3.fills = [];
  bRow3.appendChild(makeButton("Xem chi tiết hoá đơn", "#F8FAFC", "#475569", "#E2E8F0"));
  bRow3.appendChild(makeButton("Đang cập nhật...", "#94A3B8", "#FFFFFF"));
  btnFrame.appendChild(bRow3);
  btnGrid.appendChild(btnFrame);

  const inputFrame = figma.createFrame();
  inputFrame.layoutMode = "VERTICAL";
  inputFrame.itemSpacing = 16;
  inputFrame.paddingTop = 24; inputFrame.paddingBottom = 24; inputFrame.paddingLeft = 24; inputFrame.paddingRight = 24;
  inputFrame.cornerRadius = 24;
  inputFrame.fills = [hexToRgb("#FFFFFF")];
  inputFrame.strokes = [hexToRgb("#E2E8F0")];
  inputFrame.resize(500, 360);
  inputFrame.appendChild(makeText("Form Controls & Search Bar", fontBold, 15, "#0F172A"));

  const searchInput = figma.createFrame();
  searchInput.layoutMode = "HORIZONTAL";
  searchInput.counterAxisAlignItems = "CENTER";
  searchInput.paddingTop = 12; searchInput.paddingBottom = 12; searchInput.paddingLeft = 16; searchInput.paddingRight = 16;
  searchInput.cornerRadius = 14;
  searchInput.fills = [hexToRgb("#F8FAFC")];
  searchInput.strokes = [hexToRgb("#E2E8F0")];
  searchInput.strokeWeight = 1.5;
  searchInput.layoutAlign = "STRETCH";
  searchInput.itemSpacing = 10;
  const sDot = figma.createEllipse();
  sDot.resize(10, 10);
  sDot.fills = [hexToRgb("#94A3B8")];
  searchInput.appendChild(sDot);
  searchInput.appendChild(makeText("Tìm kiếm món ăn, bàn hoặc mã đơn...", fontMedium, 13, "#94A3B8"));
  inputFrame.appendChild(searchInput);

  const focusedInput = figma.createFrame();
  focusedInput.layoutMode = "VERTICAL";
  focusedInput.itemSpacing = 6;
  focusedInput.fills = [];
  focusedInput.layoutAlign = "STRETCH";
  focusedInput.appendChild(makeText("Ghi chú đặc biệt cho Bếp (Focused)", fontBold, 12, "#4338CA"));

  const fBox = figma.createFrame();
  fBox.layoutMode = "HORIZONTAL";
  fBox.paddingTop = 12; fBox.paddingBottom = 12; fBox.paddingLeft = 16; fBox.paddingRight = 16;
  fBox.cornerRadius = 14;
  fBox.fills = [hexToRgb("#FFFFFF")];
  fBox.strokes = [hexToRgb("#6366F1")];
  fBox.strokeWeight = 2;
  fBox.effects = [createDropShadow(0, 0, 0.2, "#6366F1")];
  fBox.layoutAlign = "STRETCH";
  fBox.appendChild(makeText("Ít cay, không lấy hành lá, sốt để riêng", fontMedium, 14, "#0F172A"));
  focusedInput.appendChild(fBox);
  inputFrame.appendChild(focusedInput);

  btnGrid.appendChild(inputFrame);
  compSec.appendChild(btnGrid);
  root.appendChild(compSec);

  // 5. DOMAIN COMPONENTS (TABLE, DISH, KDS, KPI)
  const domainSec = createSection("5. Thành Phần Nghiệp Vụ F&B Thực Tế (POS, Table, Dish & KDS)", "Các card nghiệp vụ quan trọng nhất của QROS");
  const domainRow = figma.createFrame();
  domainRow.layoutMode = "HORIZONTAL";
  domainRow.itemSpacing = 24;
  domainRow.fills = [];
  domainRow.layoutAlign = "STRETCH";

  // Table Card
  const tableCard = figma.createFrame();
  tableCard.name = "Table Card / Occupied";
  tableCard.layoutMode = "VERTICAL";
  tableCard.resize(320, 310);
  tableCard.cornerRadius = 24;
  tableCard.paddingTop = 20; tableCard.paddingBottom = 20; tableCard.paddingLeft = 20; tableCard.paddingRight = 20;
  tableCard.itemSpacing = 14;
  tableCard.fills = [hexToRgb("#FFFFFF")];
  tableCard.strokes = [hexToRgb("#C7D2FE")];
  tableCard.strokeWeight = 1.5;
  tableCard.effects = [createDropShadow(8, 20, 0.08)];

  const tHeader = figma.createFrame();
  tHeader.layoutMode = "HORIZONTAL";
  tHeader.layoutAlign = "STRETCH";
  tHeader.primaryAxisAlignItems = "SPACE_BETWEEN";
  tHeader.counterAxisAlignItems = "CENTER";
  tHeader.fills = [];

  const tNumBox = figma.createFrame();
  tNumBox.layoutMode = "HORIZONTAL";
  tNumBox.itemSpacing = 8;
  tNumBox.counterAxisAlignItems = "CENTER";
  tNumBox.fills = [];
  const tCircle = figma.createEllipse();
  tCircle.resize(10, 10);
  tCircle.fills = [hexToRgb("#10B981")];
  tNumBox.appendChild(tCircle);
  tNumBox.appendChild(makeText("BÀN 04", fontBlack, 18, "#0F172A"));
  tHeader.appendChild(tNumBox);

  const tPill = figma.createFrame();
  tPill.paddingTop = 4; tPill.paddingBottom = 4; tPill.paddingLeft = 10; tPill.paddingRight = 10;
  tPill.cornerRadius = 9999;
  tPill.fills = [hexToRgb("#ECFDF5")];
  tPill.appendChild(makeText("Đang ăn • 38p", fontBold, 11, "#047857"));
  tHeader.appendChild(tPill);
  tableCard.appendChild(tHeader);

  const tMeta = figma.createFrame();
  tMeta.layoutMode = "VERTICAL";
  tMeta.itemSpacing = 6;
  tMeta.paddingTop = 10; tMeta.paddingBottom = 10;
  tMeta.fills = [];
  tMeta.appendChild(makeText("Khu vực: Tầng trệt • 4 Khách", fontMedium, 13, "#64748B"));
  tMeta.appendChild(makeText("Tổng tạm tính:", fontMedium, 12, "#94A3B8"));
  tMeta.appendChild(makeText("680.000 ₫", fontBlack, 22, "#6366F1"));
  tableCard.appendChild(tMeta);

  const tBtns = figma.createFrame();
  tBtns.layoutMode = "HORIZONTAL";
  tBtns.itemSpacing = 8;
  tBtns.layoutAlign = "STRETCH";
  tBtns.fills = [];
  tBtns.appendChild(makeButton("Thanh toán", "#6366F1", "#FFFFFF"));
  tBtns.appendChild(makeButton("+ Món", "#EEF2FF", "#4338CA"));
  tableCard.appendChild(tBtns);
  domainRow.appendChild(tableCard);

  // Dish Card
  const dishCard = figma.createFrame();
  dishCard.name = "Dish Menu Card";
  dishCard.layoutMode = "VERTICAL";
  dishCard.resize(320, 310);
  dishCard.cornerRadius = 24;
  dishCard.itemSpacing = 12;
  dishCard.fills = [hexToRgb("#FFFFFF")];
  dishCard.strokes = [hexToRgb("#E2E8F0")];
  dishCard.strokeWeight = 1;
  dishCard.effects = [createDropShadow(6, 16, 0.06)];
  dishCard.clipsContent = true;

  const dishPhoto = figma.createFrame();
  dishPhoto.resize(320, 130);
  dishPhoto.fills = [hexToRgb("#E0E7FF")];
  dishPhoto.layoutMode = "HORIZONTAL";
  dishPhoto.primaryAxisAlignItems = "SPACE_BETWEEN";
  dishPhoto.paddingTop = 12; dishPhoto.paddingLeft = 12; dishPhoto.paddingRight = 12;

  const dishTag = figma.createFrame();
  dishTag.paddingTop = 4; dishTag.paddingBottom = 4; dishTag.paddingLeft = 10; dishTag.paddingRight = 10;
  dishTag.cornerRadius = 9999;
  dishTag.fills = [hexToRgb("#6366F1")];
  dishTag.appendChild(makeText("Bán chạy ★", fontBold, 11, "#FFFFFF"));
  dishPhoto.appendChild(dishTag);
  dishCard.appendChild(dishPhoto);

  const dishContent = figma.createFrame();
  dishContent.layoutMode = "VERTICAL";
  dishContent.itemSpacing = 4;
  dishContent.paddingLeft = 16; dishContent.paddingRight = 16;
  dishContent.fills = [];
  dishContent.appendChild(makeText("Bò Fuji Sốt Tiêu Đen", fontBold, 15, "#0F172A"));
  dishContent.appendChild(makeText("Thịt bò mềm mọng nước ăn kèm khoai tây nghiền kiểu Pháp", fontRegular, 12, "#64748B"));
  dishCard.appendChild(dishContent);

  const dishFooter = figma.createFrame();
  dishFooter.layoutMode = "HORIZONTAL";
  dishFooter.layoutAlign = "STRETCH";
  dishFooter.primaryAxisAlignItems = "SPACE_BETWEEN";
  dishFooter.counterAxisAlignItems = "CENTER";
  dishFooter.paddingLeft = 16; dishFooter.paddingRight = 16; dishFooter.paddingBottom = 16;
  dishFooter.fills = [];
  dishFooter.appendChild(makeText("185.000 ₫", fontBlack, 18, "#0F172A"));
  dishFooter.appendChild(makeButton("+ Thêm", "#6366F1", "#FFFFFF"));
  dishCard.appendChild(dishFooter);
  domainRow.appendChild(dishCard);

  // KDS Card
  const kdsCard = figma.createFrame();
  kdsCard.name = "KDS Kitchen Ticket";
  kdsCard.layoutMode = "VERTICAL";
  kdsCard.resize(320, 310);
  kdsCard.cornerRadius = 24;
  kdsCard.paddingTop = 18; kdsCard.paddingBottom = 18; kdsCard.paddingLeft = 18; kdsCard.paddingRight = 18;
  kdsCard.itemSpacing = 12;
  kdsCard.fills = [hexToRgb("#FFFFFF")];
  kdsCard.strokes = [hexToRgb("#FDE68A")];
  kdsCard.strokeWeight = 2;
  kdsCard.effects = [createDropShadow(6, 16, 0.08)];

  const kHeader = figma.createFrame();
  kHeader.layoutMode = "HORIZONTAL";
  kHeader.layoutAlign = "STRETCH";
  kHeader.primaryAxisAlignItems = "SPACE_BETWEEN";
  kHeader.counterAxisAlignItems = "CENTER";
  kHeader.fills = [];
  kHeader.appendChild(makeText("VÉ BẾP #108", fontBlack, 16, "#0F172A"));

  const kTimer = figma.createFrame();
  kTimer.paddingTop = 4; kTimer.paddingBottom = 4; kTimer.paddingLeft = 8; kTimer.paddingRight = 8;
  kTimer.cornerRadius = 8;
  kTimer.fills = [hexToRgb("#FEF3C7")];
  kTimer.appendChild(makeText("⏱ 14 phút", fontExtraBold, 11, "#B45309"));
  kHeader.appendChild(kTimer);
  kdsCard.appendChild(kHeader);

  const kItems = figma.createFrame();
  kItems.layoutMode = "VERTICAL";
  kItems.itemSpacing = 8;
  kItems.fills = [];
  kItems.layoutAlign = "STRETCH";

  function makeKdsItem(qty, name, note, done = false) {
    const it = figma.createFrame();
    it.layoutMode = "HORIZONTAL";
    it.itemSpacing = 10;
    it.counterAxisAlignItems = "CENTER";
    it.fills = [];

    const q = figma.createFrame();
    q.resize(24, 24);
    q.cornerRadius = 6;
    q.fills = [hexToRgb(done ? "#ECFDF5" : "#EEF2FF")];
    q.layoutMode = "HORIZONTAL";
    q.primaryAxisAlignItems = "CENTER";
    q.counterAxisAlignItems = "CENTER";
    q.appendChild(makeText(`${qty}`, fontBlack, 12, done ? "#047857" : "#4338CA"));
    it.appendChild(q);

    const txt = figma.createFrame();
    txt.layoutMode = "VERTICAL";
    txt.fills = [];
    txt.appendChild(makeText(name, fontBold, 13, done ? "#94A3B8" : "#0F172A"));
    if (note) txt.appendChild(makeText(note, fontRegular, 11, "#B45309"));
    it.appendChild(txt);
    return it;
  }

  kItems.appendChild(makeKdsItem(2, "Bò Fuji Áp Chảo", "Không hành, chín vừa"));
  kItems.appendChild(makeKdsItem(1, "Mỳ Ý Hải Sản Cay", "Ít cay"));
  kItems.appendChild(makeKdsItem(3, "Trà Đào Cam Sả", "Đã trả món", true));
  kdsCard.appendChild(kItems);

  const btnKdsDone = makeButton("✓ Hoàn tất tất cả món", "#10B981", "#FFFFFF");
  btnKdsDone.layoutAlign = "STRETCH";
  kdsCard.appendChild(btnKdsDone);
  domainRow.appendChild(kdsCard);

  // KPI Card
  const kpiCard = figma.createFrame();
  kpiCard.name = "KPI Stat Card";
  kpiCard.layoutMode = "VERTICAL";
  kpiCard.resize(320, 310);
  kpiCard.cornerRadius = 24;
  kpiCard.paddingTop = 22; kpiCard.paddingBottom = 22; kpiCard.paddingLeft = 22; kpiCard.paddingRight = 22;
  kpiCard.itemSpacing = 16;
  kpiCard.fills = [hexToRgb("#FFFFFF")];
  kpiCard.strokes = [hexToRgb("#E2E8F0")];
  kpiCard.strokeWeight = 1;
  kpiCard.effects = [createDropShadow(8, 24, 0.08)];

  const kpiTop = figma.createFrame();
  kpiTop.layoutMode = "HORIZONTAL";
  kpiTop.layoutAlign = "STRETCH";
  kpiTop.primaryAxisAlignItems = "SPACE_BETWEEN";
  kpiTop.counterAxisAlignItems = "CENTER";
  kpiTop.fills = [];

  const kpiTitles = figma.createFrame();
  kpiTitles.layoutMode = "VERTICAL";
  kpiTitles.itemSpacing = 4;
  kpiTitles.fills = [];
  kpiTitles.appendChild(makeText("DOANH THU HÔM NAY", fontExtraBold, 11, "#64748B"));
  kpiTitles.appendChild(makeText("34.850.000 ₫", fontBlack, 24, "#0F172A"));
  kpiTop.appendChild(kpiTitles);

  const kpiIconBox = figma.createFrame();
  kpiIconBox.resize(48, 48);
  kpiIconBox.cornerRadius = 16;
  kpiIconBox.fills = [hexToRgb("#6366F1")];
  kpiIconBox.effects = [createDropShadow(6, 14, 0.3, "#6366F1")];
  kpiTop.appendChild(kpiIconBox);
  kpiCard.appendChild(kpiTop);

  const kpiGrowth = figma.createFrame();
  kpiGrowth.layoutMode = "HORIZONTAL";
  kpiGrowth.itemSpacing = 8;
  kpiGrowth.counterAxisAlignItems = "CENTER";
  kpiGrowth.fills = [];
  const gPill = figma.createFrame();
  gPill.paddingTop = 4; gPill.paddingBottom = 4; gPill.paddingLeft = 8; gPill.paddingRight = 8;
  gPill.cornerRadius = 6;
  gPill.fills = [hexToRgb("#ECFDF5")];
  gPill.appendChild(makeText("↗ +24.8%", fontBold, 12, "#047857"));
  kpiGrowth.appendChild(gPill);
  kpiGrowth.appendChild(makeText("so với cùng kỳ tuần trước", fontMedium, 12, "#64748B"));
  kpiCard.appendChild(kpiGrowth);

  const kpiList = figma.createFrame();
  kpiList.layoutMode = "VERTICAL";
  kpiList.itemSpacing = 6;
  kpiList.paddingTop = 8;
  kpiList.fills = [];
  kpiList.appendChild(makeText("• Đơn hoàn tất: 68 đơn (TB: 512k / đơn)", fontMedium, 12, "#475569"));
  kpiList.appendChild(makeText("• Thanh toán QR / PayOS: 78.4%", fontMedium, 12, "#475569"));
  kpiCard.appendChild(kpiList);
  domainRow.appendChild(kpiCard);

  domainSec.appendChild(domainRow);
  root.appendChild(domainSec);

  // 6. DARK MODE MIDNIGHT PREVIEW
  const darkSec = createSection("6. Giao Diện Chế Độ Ban Đêm (Deep Midnight Dark Mode)", "Phối màu Midnight Luxury cao cấp cho màn hình POS và KDS trong bếp tối");
  const darkFrame = figma.createFrame();
  darkFrame.name = "Dark Mode Showcase Board";
  darkFrame.layoutMode = "HORIZONTAL";
  darkFrame.resize(1400, 360);
  darkFrame.cornerRadius = 28;
  darkFrame.paddingTop = 28; darkFrame.paddingBottom = 28; darkFrame.paddingLeft = 28; darkFrame.paddingRight = 28;
  darkFrame.itemSpacing = 24;
  darkFrame.fills = [hexToRgb("#0B0F17")];
  darkFrame.strokes = [hexToRgb("#1E293B")];
  darkFrame.strokeWeight = 1.5;

  const dTable = figma.createFrame();
  dTable.resize(320, 300);
  dTable.cornerRadius = 20;
  dTable.paddingTop = 20; dTable.paddingBottom = 20; dTable.paddingLeft = 20; dTable.paddingRight = 20;
  dTable.itemSpacing = 14;
  dTable.layoutMode = "VERTICAL";
  dTable.fills = [hexToRgb("#131B2E")];
  dTable.strokes = [hexToRgb("#1E293B")];
  dTable.strokeWeight = 1;

  const dtHead = figma.createFrame();
  dtHead.layoutMode = "HORIZONTAL";
  dtHead.layoutAlign = "STRETCH";
  dtHead.primaryAxisAlignItems = "SPACE_BETWEEN";
  dtHead.counterAxisAlignItems = "CENTER";
  dtHead.fills = [];
  dtHead.appendChild(makeText("BÀN 02 (VIP)", fontBlack, 16, "#F8FAFC"));
  const dtPill = figma.createFrame();
  dtPill.paddingTop = 4; dtPill.paddingBottom = 4; dtPill.paddingLeft = 8; dtPill.paddingRight = 8;
  dtPill.cornerRadius = 9999;
  dtPill.fills = [hexToRgb("#064E3B")];
  dtPill.appendChild(makeText("Có khách", fontBold, 11, "#34D399"));
  dtHead.appendChild(dtPill);
  dTable.appendChild(dtHead);

  dTable.appendChild(makeText("Tạm tính: 1.250.000 ₫", fontBlack, 20, "#818CF8"));
  dTable.appendChild(makeText("6 món • Chờ món cuối: Lẩu cá hồi Na Uy", fontMedium, 12, "#94A3B8"));
  const btnD1 = makeButton("Thanh toán PayOS", "#818CF8", "#0B0F17");
  btnD1.layoutAlign = "STRETCH";
  dTable.appendChild(btnD1);
  darkFrame.appendChild(dTable);

  const dKds = figma.createFrame();
  dKds.resize(320, 300);
  dKds.cornerRadius = 20;
  dKds.paddingTop = 20; dKds.paddingBottom = 20; dKds.paddingLeft = 20; dKds.paddingRight = 20;
  dKds.itemSpacing = 14;
  dKds.layoutMode = "VERTICAL";
  dKds.fills = [hexToRgb("#131B2E")];
  dKds.strokes = [hexToRgb("#881337")];
  dKds.strokeWeight = 2;

  const dkHead = figma.createFrame();
  dkHead.layoutMode = "HORIZONTAL";
  dkHead.layoutAlign = "STRETCH";
  dkHead.primaryAxisAlignItems = "SPACE_BETWEEN";
  dkHead.counterAxisAlignItems = "CENTER";
  dkHead.fills = [];
  dkHead.appendChild(makeText("BẾP #102 (QUÁ HẠN)", fontBlack, 15, "#F87171"));
  const dkPill = figma.createFrame();
  dkPill.paddingTop = 4; dkPill.paddingBottom = 4; dkPill.paddingLeft = 8; dkPill.paddingRight = 8;
  dkPill.cornerRadius = 8;
  dkPill.fills = [hexToRgb("#881337")];
  dkPill.appendChild(makeText("🔥 26 phút", fontBlack, 11, "#FECDD3"));
  dkHead.appendChild(dkPill);
  dKds.appendChild(dkHead);

  dKds.appendChild(makeText("1. Sườn Heo Nướng BBQ (x2)", fontBold, 13, "#F8FAFC"));
  dKds.appendChild(makeText("2. Khoai Tây Chiên Phô Mai", fontBold, 13, "#F8FAFC"));
  const btnD2 = makeButton("Ra món ngay", "#F87171", "#0B0F17");
  btnD2.layoutAlign = "STRETCH";
  dKds.appendChild(btnD2);
  darkFrame.appendChild(dKds);

  darkSec.appendChild(darkFrame);
  root.appendChild(darkSec);

  figma.currentPage.appendChild(root);
  figma.viewport.scrollAndZoomIntoView([root]);

  figma.closePlugin("✅ Đã tạo thành công Master Design System bản chuẩn sản xuất!");
})();
