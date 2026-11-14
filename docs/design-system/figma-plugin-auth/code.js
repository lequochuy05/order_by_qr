// ==============================================================================
// QROS HI-FI AUTHENTICATION & SECURITY FLOWS — FIGMA PLUGIN (V2 FIX)
// Style: Modern SaaS & Restaurant POS (Electric Indigo #6366F1, Be Vietnam Pro)
// Target: Run as Figma Plugin
// ==============================================================================

(async function generateHiFiAuthFlows() {
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

  function makeLinearGradient(hexStart, hexEnd, angle = 45) {
    const rad = (angle * Math.PI) / 180;
    const cos = Math.cos(rad);
    const sin = Math.sin(rad);
    return {
      type: "GRADIENT_LINEAR",
      gradientTransform: [
        [cos, sin, 0.5 - 0.5 * cos - 0.5 * sin],
        [-sin, cos, 0.5 + 0.5 * sin - 0.5 * cos],
      ],
      gradientStops: [
        { position: 0, color: { ...hexToRgb(hexStart).color, a: 1 } },
        { position: 1, color: { ...hexToRgb(hexEnd).color, a: 1 } },
      ],
    };
  }

  function createDropShadow(y = 8, blur = 24, opacity = 0.08, colorHex = "#0F172A") {
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
        await figma.loadFontAsync({
          family: fallbackFontFamily,
          style: style === "Black" || style === "ExtraBold" ? "Bold" : style,
        });
        return {
          family: fallbackFontFamily,
          style: style === "Black" || style === "ExtraBold" ? "Bold" : style,
        };
      } catch {
        await figma.loadFontAsync({ family: "Roboto", style: "Regular" });
        return { family: "Roboto", style: "Regular" };
      }
    }
  }

  const fontMedium = await loadFontWithFallback("Medium");
  const fontBold = await loadFontWithFallback("Bold");
  const fontExtraBold = await loadFontWithFallback("ExtraBold");
  const fontBlack = await loadFontWithFallback("Black");

  function makeText(str, font, size, fillHex = "#0F172A", opacity = 1, tracking = 0) {
    const t = figma.createText();
    t.fontName = font;
    t.fontSize = size;
    t.characters = str || " ";
    t.fills = [hexToRgb(fillHex, opacity)];
    if (tracking !== 0) {
      t.letterSpacing = { value: tracking, unit: "PIXELS" };
    }
    return t;
  }

  function getSvgIcon(name, colorHex = "#6366F1", size = 20) {
    const svgs = {
      utensils: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m16 2-2.3 2.3a3 3 0 0 0 0 4.2l1.8 1.8a3 3 0 0 0 4.2 0L22 8Z"/><path d="M15 15 3.3 3.3a4.2 4.2 0 0 0 0 6l7.3 7.3c.7.7 2 .7 2.8 0L15 15Zm0 0 7 7"/><path d="m2.1 21.8 6.4-6.3"/><path d="m19 5-7 7"/></svg>`,
      mail: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>`,
      lock: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>`,
      shield: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/><path d="m9 12 2 2 4-4"/></svg>`,
      key: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"/><circle cx="16.5" cy="7.5" r=".5" fill="${colorHex}"/></svg>`,
      alert: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>`,
      check: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`,
      arrowLeft: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 19-7-7 7-7"/><path d="M19 12H5"/></svg>`,
      eye: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 .696 10.75 10.75 0 0 1-19.876 0"/><circle cx="12" cy="12" r="3"/></svg>`,
      timer: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="10" x2="14" y1="2" y2="2"/><line x1="12" x2="15" y1="14" y2="11"/><circle cx="12" cy="14" r="8"/></svg>`,
    };
    try {
      if (svgs[name]) {
        const node = figma.createNodeFromSvg(svgs[name]);
        node.name = `Icon/${name}`;
        return node;
      }
    } catch {
      // fallback
    }
    const box = figma.createFrame();
    box.resize(size, size);
    box.fills = [hexToRgb(colorHex, 0.2)];
    return box;
  }

  function createInputGroup(label, iconName, placeholder, value = "", isDark = false, isError = false, hasEye = false, rightLinkText = null) {
    const group = figma.createFrame();
    group.layoutMode = "VERTICAL";
    group.itemSpacing = 6;
    group.fills = [];
    group.layoutAlign = "STRETCH";

    const labelRow = figma.createFrame();
    labelRow.layoutMode = "HORIZONTAL";
    labelRow.layoutAlign = "STRETCH";
    labelRow.primaryAxisAlignItems = "SPACE_BETWEEN";
    labelRow.counterAxisAlignItems = "CENTER";
    labelRow.fills = [];

    labelRow.appendChild(makeText(label.toUpperCase(), fontExtraBold, 11, isDark ? "#94A3B8" : "#475569", 1, 0.4));
    if (rightLinkText) {
      labelRow.appendChild(makeText(rightLinkText, fontBold, 12, isDark ? "#818CF8" : "#6366F1"));
    }
    group.appendChild(labelRow);

    const box = figma.createFrame();
    box.layoutMode = "HORIZONTAL";
    box.layoutAlign = "STRETCH";
    box.counterAxisAlignItems = "CENTER";
    box.paddingTop = 13;
    box.paddingBottom = 13;
    box.paddingLeft = 14;
    box.paddingRight = 14;
    box.itemSpacing = 10;
    box.cornerRadius = 16;

    if (isDark) {
      box.fills = [hexToRgb(value ? "#0B0F17" : "#0B0F17", 0.6)];
      box.strokes = [hexToRgb(isError ? "#F43F5E" : value ? "#818CF8" : "#1E293B")];
    } else {
      box.fills = [hexToRgb(value ? "#FFFFFF" : "#F8FAFC", 0.75)];
      box.strokes = [hexToRgb(isError ? "#F43F5E" : value ? "#6366F1" : "#E2E8F0")];
    }
    box.strokeWeight = isError || value ? 1.5 : 1;
    if (value && !isError) {
      box.effects = [createDropShadow(0, 0, 0.15, isDark ? "#818CF8" : "#6366F1")];
    }

    const iconColor = isError ? "#F43F5E" : value ? (isDark ? "#818CF8" : "#6366F1") : "#94A3B8";
    box.appendChild(getSvgIcon(iconName, iconColor, 18));

    const txtBox = figma.createFrame();
    txtBox.layoutMode = "HORIZONTAL";
    txtBox.layoutGrow = 1;
    txtBox.fills = [];
    txtBox.appendChild(
      makeText(
        value || placeholder,
        value ? fontBold : fontMedium,
        13.5,
        value ? (isDark ? "#F8FAFC" : "#0F172A") : isDark ? "#64748B" : "#94A3B8"
      )
    );
    box.appendChild(txtBox);

    if (hasEye) {
      box.appendChild(getSvgIcon("eye", isDark ? "#64748B" : "#94A3B8", 18));
    }

    group.appendChild(box);
    return group;
  }

  function createPrimaryButton(label, isDark = false) {
    const btn = figma.createFrame();
    btn.layoutMode = "HORIZONTAL";
    btn.primaryAxisAlignItems = "CENTER";
    btn.counterAxisAlignItems = "CENTER";
    btn.paddingTop = 15;
    btn.paddingBottom = 15;
    btn.cornerRadius = 16;
    btn.layoutAlign = "STRETCH";

    btn.fills = [makeLinearGradient(isDark ? "#6366F1" : "#6366F1", isDark ? "#8B5CF6" : "#7C3AED", 45)];
    btn.effects = [createDropShadow(10, 24, 0.28, isDark ? "#818CF8" : "#6366F1")];
    btn.appendChild(makeText(label.toUpperCase(), fontBlack, 13.5, "#FFFFFF", 1, 0.5));
    return btn;
  }

  function createAlertBanner(msg, isDark = false, type = "error") {
    const alert = figma.createFrame();
    alert.layoutMode = "HORIZONTAL";
    alert.layoutAlign = "STRETCH";
    alert.counterAxisAlignItems = "CENTER";
    alert.paddingTop = 12;
    alert.paddingBottom = 12;
    alert.paddingLeft = 14;
    alert.paddingRight = 14;
    alert.itemSpacing = 10;
    alert.cornerRadius = 16;

    if (type === "error") {
      alert.fills = [hexToRgb(isDark ? "#881337" : "#FEF2F2", isDark ? 0.35 : 0.95)];
      alert.strokes = [hexToRgb(isDark ? "#BE123C" : "#FECDD3", isDark ? 0.6 : 1)];
      alert.strokeWeight = 1;
      alert.appendChild(getSvgIcon("alert", isDark ? "#FCA5A5" : "#B91C1C", 18));
      alert.appendChild(makeText(msg, fontBold, 12, isDark ? "#FECDD3" : "#B91C1C"));
    } else {
      alert.fills = [hexToRgb(isDark ? "#064E3B" : "#ECFDF5", isDark ? 0.35 : 0.95)];
      alert.strokes = [hexToRgb(isDark ? "#047857" : "#A7F3D0", isDark ? 0.6 : 1)];
      alert.strokeWeight = 1;
      alert.appendChild(getSvgIcon("check", isDark ? "#34D399" : "#047857", 18));
      alert.appendChild(makeText(msg, fontBold, 12, isDark ? "#6EE7B7" : "#047857"));
    }
    return alert;
  }

  function createScreenContainer(title, isDark = false) {
    const screen = figma.createFrame();
    screen.name = `Screen / ${title}`;
    screen.layoutMode = "VERTICAL";
    screen.primaryAxisSizingMode = "AUTO";
    screen.counterAxisSizingMode = "FIXED";
    screen.resize(420, 100);
    screen.cornerRadius = 28;
    screen.paddingTop = 20;
    screen.paddingBottom = 24;
    screen.paddingLeft = 20;
    screen.paddingRight = 20;
    screen.itemSpacing = 16;

    screen.fills = [hexToRgb(isDark ? "#0B0F17" : "#F8FAFC")];
    screen.strokes = [hexToRgb(isDark ? "#1E293B" : "#E2E8F0")];
    screen.strokeWeight = 1.5;

    const tag = figma.createFrame();
    tag.layoutMode = "HORIZONTAL";
    tag.paddingTop = 6;
    tag.paddingBottom = 6;
    tag.paddingLeft = 14;
    tag.paddingRight = 14;
    tag.cornerRadius = 9999;
    tag.fills = [hexToRgb(isDark ? "#1E293B" : "#EEF2FF")];
    tag.appendChild(makeText(title.toUpperCase(), fontExtraBold, 10.5, isDark ? "#818CF8" : "#4338CA", 1, 0.4));
    screen.appendChild(tag);

    const card = figma.createFrame();
    card.name = "Auth Card";
    card.layoutMode = "VERTICAL";
    card.layoutAlign = "STRETCH";
    card.primaryAxisSizingMode = "AUTO";
    card.cornerRadius = 24;
    card.paddingTop = 28;
    card.paddingBottom = 24;
    card.paddingLeft = 24;
    card.paddingRight = 24;
    card.itemSpacing = 18;

    if (isDark) {
      card.fills = [hexToRgb("#131B2E", 0.98)];
      card.strokes = [hexToRgb("#1E293B")];
      card.effects = [createDropShadow(16, 36, 0.4, "#000000")];
    } else {
      card.fills = [hexToRgb("#FFFFFF")];
      card.strokes = [hexToRgb("#E2E8F0")];
      card.effects = [createDropShadow(12, 32, 0.08, "#0F172A")];
    }
    card.strokeWeight = 1;

    screen.appendChild(card);
    return { screen, card };
  }

  function addCardHeader(card, iconName, title, subtitle, isDark = false, gradientStart = "#6366F1", gradientEnd = "#8B5CF6") {
    const head = figma.createFrame();
    head.layoutMode = "VERTICAL";
    head.primaryAxisAlignItems = "CENTER";
    head.counterAxisAlignItems = "CENTER";
    head.layoutAlign = "STRETCH";
    head.itemSpacing = 8;
    head.fills = [];

    const iconBox = figma.createFrame();
    iconBox.resize(52, 52);
    iconBox.cornerRadius = 16;
    iconBox.primaryAxisAlignItems = "CENTER";
    iconBox.counterAxisAlignItems = "CENTER";
    iconBox.layoutMode = "HORIZONTAL";
    iconBox.fills = [makeLinearGradient(gradientStart, gradientEnd, 45)];
    iconBox.effects = [createDropShadow(8, 20, 0.28, gradientStart)];
    iconBox.appendChild(getSvgIcon(iconName, "#FFFFFF", 26));
    head.appendChild(iconBox);

    head.appendChild(makeText(title, fontBlack, 22, isDark ? "#F8FAFC" : "#0F172A"));
    head.appendChild(makeText(subtitle, fontMedium, 12, isDark ? "#94A3B8" : "#64748B"));
    card.appendChild(head);
  }

  function addFooterLink(card, text, isDark = false) {
    const foot = figma.createFrame();
    foot.layoutMode = "HORIZONTAL";
    foot.layoutAlign = "STRETCH";
    foot.primaryAxisAlignItems = "CENTER";
    foot.counterAxisAlignItems = "CENTER";
    foot.paddingTop = 14;
    foot.itemSpacing = 6;
    foot.fills = [];
    foot.strokes = [hexToRgb(isDark ? "#1E293B" : "#E2E8F0")];
    foot.strokeWeight = 1;

    foot.appendChild(getSvgIcon("arrowLeft", isDark ? "#818CF8" : "#6366F1", 14));
    foot.appendChild(makeText(text, fontBold, 12, isDark ? "#818CF8" : "#6366F1"));
    card.appendChild(foot);
  }

  const masterBoard = figma.createFrame();
  masterBoard.name = "🔐 QROS — Authentication & Security Flows (Hi-Fi Prototype)";
  masterBoard.fills = [hexToRgb("#F1F5F9")];
  masterBoard.layoutMode = "VERTICAL";
  masterBoard.primaryAxisSizingMode = "AUTO";
  masterBoard.counterAxisSizingMode = "FIXED";
  masterBoard.resize(1904, 100);
  masterBoard.paddingTop = 56;
  masterBoard.paddingBottom = 80;
  masterBoard.paddingLeft = 64;
  masterBoard.paddingRight = 64;
  masterBoard.itemSpacing = 48;
  masterBoard.cornerRadius = 32;

  const masterHeader = figma.createFrame();
  masterHeader.layoutMode = "VERTICAL";
  masterHeader.layoutAlign = "STRETCH";
  masterHeader.paddingTop = 36;
  masterHeader.paddingBottom = 36;
  masterHeader.paddingLeft = 44;
  masterHeader.paddingRight = 44;
  masterHeader.cornerRadius = 24;
  masterHeader.itemSpacing = 10;
  masterHeader.fills = [makeLinearGradient("#6366F1", "#8B5CF6", 90)];
  masterHeader.effects = [createDropShadow(16, 36, 0.25, "#6366F1")];

  const badge = figma.createFrame();
  badge.paddingTop = 4;
  badge.paddingBottom = 4;
  badge.paddingLeft = 12;
  badge.paddingRight = 12;
  badge.cornerRadius = 9999;
  badge.fills = [hexToRgb("#FFFFFF", 0.2)];
  badge.appendChild(makeText("HI-FI PRODUCTION SPEC • V3.0.0", fontExtraBold, 11, "#FFFFFF", 1, 0.5));
  masterHeader.appendChild(badge);

  masterHeader.appendChild(makeText("QROS AUTHENTICATION & SECURITY FLOWS", fontBlack, 32, "#FFFFFF"));
  masterHeader.appendChild(
    makeText(
      "Giao diện chuẩn xác thực quản trị: Đăng nhập đa vai trò, Xác thực bảo mật 2 bước (2FA OTP), Quên/Khôi phục mật khẩu & Dark Mode.",
      fontMedium,
      15,
      "#EEF2FF",
      0.95
    )
  );
  masterBoard.appendChild(masterHeader);

  function createFlowRow(rowTitle) {
    const rowWrap = figma.createFrame();
    rowWrap.layoutMode = "VERTICAL";
    rowWrap.itemSpacing = 18;
    rowWrap.fills = [];
    rowWrap.layoutAlign = "STRETCH";

    rowWrap.appendChild(makeText(rowTitle, fontBlack, 20, "#0F172A"));
    const grid = figma.createFrame();
    grid.layoutMode = "HORIZONTAL";
    grid.primaryAxisSizingMode = "AUTO";
    grid.counterAxisSizingMode = "AUTO";
    grid.itemSpacing = 32;
    grid.fills = [];
    grid.layoutAlign = "STRETCH";

    rowWrap.appendChild(grid);
    return { rowWrap, grid };
  }

  // Row 1
  const { rowWrap: row1, grid: grid1 } = createFlowRow("Row 1: Luồng Đăng Nhập & Bảo Mật 2FA (Admin)");

  // 01. Login Default
  {
    const { screen, card } = createScreenContainer("01. Login Default");
    addCardHeader(card, "utensils", "Đăng Nhập", "QROS - Hệ thống gọi món bằng mã QR");

    const form = figma.createFrame();
    form.layoutMode = "VERTICAL";
    form.itemSpacing = 14;
    form.layoutAlign = "STRETCH";
    form.fills = [];

    form.appendChild(createInputGroup("Email", "mail", "admin@qros.vn", "wuchuy.dev@gmail.com"));
    form.appendChild(createInputGroup("Mật khẩu", "lock", "••••••••", "••••••••••••", false, false, true, "Quên mật khẩu?"));
    form.appendChild(createPrimaryButton("ĐĂNG NHẬP"));
    card.appendChild(form);

    addFooterLink(card, "Về trang quét mã QR");
    grid1.appendChild(screen);
  }

  // 02. Login Error
  {
    const { screen, card } = createScreenContainer("02. Login Error State");
    addCardHeader(card, "utensils", "Đăng Nhập", "QROS - Hệ thống gọi món bằng mã QR");
    card.appendChild(createAlertBanner("Email hoặc mật khẩu không chính xác"));

    const form = figma.createFrame();
    form.layoutMode = "VERTICAL";
    form.itemSpacing = 14;
    form.layoutAlign = "STRETCH";
    form.fills = [];

    form.appendChild(createInputGroup("Email", "mail", "admin@qros.vn", "wuchuy.dev@gmail.com"));
    form.appendChild(createInputGroup("Mật khẩu", "lock", "••••••••", "123456", false, true, true, "Quên mật khẩu?"));
    form.appendChild(createPrimaryButton("ĐĂNG NHẬP"));
    card.appendChild(form);

    addFooterLink(card, "Về trang quét mã QR");
    grid1.appendChild(screen);
  }

  // 03. Admin 2FA
  {
    const { screen, card } = createScreenContainer("03. Admin 2FA Verification");
    const backBtn = figma.createFrame();
    backBtn.layoutMode = "HORIZONTAL";
    backBtn.counterAxisAlignItems = "CENTER";
    backBtn.itemSpacing = 6;
    backBtn.fills = [];
    backBtn.appendChild(getSvgIcon("arrowLeft", "#64748B", 14));
    backBtn.appendChild(makeText("Quay lại đăng nhập", fontBold, 11, "#64748B"));
    card.appendChild(backBtn);

    addCardHeader(card, "shield", "Xác Thực 2 Bước (2FA)", "Mã OTP 6 chữ số đã được gửi tới email:");
    const emailPill = figma.createFrame();
    emailPill.paddingTop = 6;
    emailPill.paddingBottom = 6;
    emailPill.paddingLeft = 14;
    emailPill.paddingRight = 14;
    emailPill.cornerRadius = 10;
    emailPill.layoutAlign = "CENTER";
    emailPill.fills = [hexToRgb("#EEF2FF")];
    emailPill.appendChild(makeText("admin@qros.vn", fontExtraBold, 12, "#4338CA"));
    card.appendChild(emailPill);

    const otpContainer = figma.createFrame();
    otpContainer.layoutMode = "HORIZONTAL";
    otpContainer.itemSpacing = 8;
    otpContainer.primaryAxisAlignItems = "CENTER";
    otpContainer.layoutAlign = "STRETCH";
    otpContainer.fills = [];

    const sampleDigits = ["8", "4", "2", "9", "1", "•"];
    for (let i = 0; i < 6; i++) {
      const d = figma.createFrame();
      d.resize(44, 50);
      d.cornerRadius = 12;
      d.primaryAxisAlignItems = "CENTER";
      d.counterAxisAlignItems = "CENTER";
      d.layoutMode = "HORIZONTAL";
      const isCurrent = i === 5;
      d.fills = [hexToRgb(sampleDigits[i] !== "•" ? "#FFFFFF" : "#F8FAFC")];
      d.strokes = [hexToRgb(isCurrent ? "#6366F1" : sampleDigits[i] !== "•" ? "#6366F1" : "#E2E8F0")];
      d.strokeWeight = isCurrent || sampleDigits[i] !== "•" ? 1.5 : 1;
      if (isCurrent) {
        d.effects = [createDropShadow(0, 0, 0.25, "#6366F1")];
      }
      d.appendChild(makeText(sampleDigits[i], fontBlack, 20, sampleDigits[i] !== "•" ? "#0F172A" : "#CBD5E1"));
      otpContainer.appendChild(d);
    }
    card.appendChild(otpContainer);

    const timerRow = figma.createFrame();
    timerRow.layoutMode = "HORIZONTAL";
    timerRow.layoutAlign = "STRETCH";
    timerRow.primaryAxisAlignItems = "SPACE_BETWEEN";
    timerRow.counterAxisAlignItems = "CENTER";
    timerRow.fills = [];

    const tLeft = figma.createFrame();
    tLeft.layoutMode = "HORIZONTAL";
    tLeft.itemSpacing = 6;
    tLeft.counterAxisAlignItems = "CENTER";
    tLeft.fills = [];
    tLeft.appendChild(getSvgIcon("timer", "#64748B", 14));
    tLeft.appendChild(makeText("Hết hạn trong 04:52", fontMedium, 11, "#64748B"));
    timerRow.appendChild(tLeft);
    timerRow.appendChild(makeText("Gửi lại mã (42s)", fontBold, 11, "#94A3B8"));
    card.appendChild(timerRow);

    card.appendChild(createPrimaryButton("XÁC NHẬN ĐĂNG NHẬP"));
    grid1.appendChild(screen);
  }

  // 04. Forgot Password
  {
    const { screen, card } = createScreenContainer("04. Forgot Password");
    addCardHeader(card, "key", "Quên Mật Khẩu", "Nhập email đã đăng ký để nhận liên kết khôi phục");

    const form = figma.createFrame();
    form.layoutMode = "VERTICAL";
    form.itemSpacing = 16;
    form.layoutAlign = "STRETCH";
    form.fills = [];

    form.appendChild(createInputGroup("Email", "mail", "admin@qros.vn", "admin@qros.vn"));
    form.appendChild(createPrimaryButton("GỬI LIÊN KẾT XÁC THỰC"));
    card.appendChild(form);

    addFooterLink(card, "Quay lại đăng nhập");
    grid1.appendChild(screen);
  }
  masterBoard.appendChild(row1);

  // Row 2
  const { rowWrap: row2, grid: grid2 } = createFlowRow("Row 2: Khôi Phục Mật Khẩu & Chế Độ Midnight Dark Mode");

  // 05. Success Sent
  {
    const { screen, card } = createScreenContainer("05. Link Sent Success");
    addCardHeader(card, "check", "Đã Gửi Hướng Dẫn", "Chúng tôi đã gửi link đặt lại mật khẩu tới:", false, "#10B981", "#14B8A6");

    const emailBox = figma.createFrame();
    emailBox.layoutMode = "VERTICAL";
    emailBox.primaryAxisAlignItems = "CENTER";
    emailBox.counterAxisAlignItems = "CENTER";
    emailBox.paddingTop = 14;
    emailBox.paddingBottom = 14;
    emailBox.paddingLeft = 16;
    emailBox.paddingRight = 16;
    emailBox.cornerRadius = 16;
    emailBox.layoutAlign = "STRETCH";
    emailBox.fills = [hexToRgb("#ECFDF5")];
    emailBox.strokes = [hexToRgb("#A7F3D0")];
    emailBox.strokeWeight = 1;
    emailBox.appendChild(makeText("admin@qros.vn", fontBlack, 14, "#047857"));
    emailBox.appendChild(makeText("Vui lòng kiểm tra hộp thư đến hoặc thư rác.", fontMedium, 11, "#059669"));
    card.appendChild(emailBox);

    const reBtn = figma.createFrame();
    reBtn.layoutMode = "HORIZONTAL";
    reBtn.primaryAxisAlignItems = "CENTER";
    reBtn.counterAxisAlignItems = "CENTER";
    reBtn.paddingTop = 14;
    reBtn.paddingBottom = 14;
    reBtn.cornerRadius = 16;
    reBtn.layoutAlign = "STRETCH";
    reBtn.fills = [hexToRgb("#EEF2FF")];
    reBtn.strokes = [hexToRgb("#C7D2FE")];
    reBtn.strokeWeight = 1;
    reBtn.appendChild(makeText("Gửi lại email khác", fontBold, 13, "#4338CA"));
    card.appendChild(reBtn);

    addFooterLink(card, "Quay lại đăng nhập");
    grid2.appendChild(screen);
  }

  // 06. Reset Password Form
  {
    const { screen, card } = createScreenContainer("06. Reset Password Form");
    addCardHeader(card, "shield", "Đặt Lại Mật Khẩu", "Tạo mật khẩu mới có độ bảo mật cao");

    const form = figma.createFrame();
    form.layoutMode = "VERTICAL";
    form.itemSpacing = 12;
    form.layoutAlign = "STRETCH";
    form.fills = [];

    form.appendChild(createInputGroup("Mật khẩu mới", "lock", "••••••••", "QrosAdmin@2026", false, false, true));
    form.appendChild(createInputGroup("Xác nhận mật khẩu", "lock", "••••••••", "QrosAdmin@2026", false, false, true));

    const rules = figma.createFrame();
    rules.layoutMode = "VERTICAL";
    rules.itemSpacing = 6;
    rules.paddingTop = 4;
    rules.paddingBottom = 4;
    rules.fills = [];
    rules.layoutAlign = "STRETCH";

    function makeRuleItem(label, checked = true) {
      const r = figma.createFrame();
      r.layoutMode = "HORIZONTAL";
      r.counterAxisAlignItems = "CENTER";
      r.itemSpacing = 6;
      r.fills = [];
      r.appendChild(getSvgIcon("check", checked ? "#10B981" : "#94A3B8", 14));
      r.appendChild(makeText(label, fontMedium, 11, checked ? "#047857" : "#64748B"));
      return r;
    }
    rules.appendChild(makeRuleItem("Tối thiểu 8 ký tự, có chữ hoa & số"));
    rules.appendChild(makeRuleItem("Có ít nhất 1 ký tự đặc biệt (!@#$%)"));
    form.appendChild(rules);

    form.appendChild(createPrimaryButton("CẬP NHẬT MẬT KHẨU"));
    card.appendChild(form);

    addFooterLink(card, "Quay lại đăng nhập");
    grid2.appendChild(screen);
  }

  // 07. Dark Mode Login
  {
    const { screen, card } = createScreenContainer("07. Login (Midnight Dark)", true);
    addCardHeader(card, "utensils", "Đăng Nhập", "QROS - Hệ thống gọi món bằng mã QR", true);

    const form = figma.createFrame();
    form.layoutMode = "VERTICAL";
    form.itemSpacing = 14;
    form.layoutAlign = "STRETCH";
    form.fills = [];

    form.appendChild(createInputGroup("Email", "mail", "admin@qros.vn", "admin@qros.vn", true));
    form.appendChild(createInputGroup("Mật khẩu", "lock", "••••••••", "••••••••", true, false, true, "Quên mật khẩu?"));
    form.appendChild(createPrimaryButton("ĐĂNG NHẬP", true));
    card.appendChild(form);

    addFooterLink(card, "Về trang quét mã QR", true);
    grid2.appendChild(screen);
  }

  // 08. Dark Mode 2FA
  {
    const { screen, card } = createScreenContainer("08. Admin 2FA (Midnight Dark)", true);

    const backBtn = figma.createFrame();
    backBtn.layoutMode = "HORIZONTAL";
    backBtn.counterAxisAlignItems = "CENTER";
    backBtn.itemSpacing = 6;
    backBtn.fills = [];
    backBtn.appendChild(getSvgIcon("arrowLeft", "#94A3B8", 14));
    backBtn.appendChild(makeText("Quay lại đăng nhập", fontBold, 11, "#94A3B8"));
    card.appendChild(backBtn);

    addCardHeader(card, "shield", "Xác Thực 2 Bước (2FA)", "Mã OTP 6 chữ số đã được gửi tới email:", true);

    const emailPill = figma.createFrame();
    emailPill.paddingTop = 6;
    emailPill.paddingBottom = 6;
    emailPill.paddingLeft = 14;
    emailPill.paddingRight = 14;
    emailPill.cornerRadius = 10;
    emailPill.layoutAlign = "CENTER";
    emailPill.fills = [hexToRgb("#312E81", 0.5)];
    emailPill.appendChild(makeText("admin@qros.vn", fontExtraBold, 12, "#A5B4FC"));
    card.appendChild(emailPill);

    const otpContainer = figma.createFrame();
    otpContainer.layoutMode = "HORIZONTAL";
    otpContainer.itemSpacing = 8;
    otpContainer.primaryAxisAlignItems = "CENTER";
    otpContainer.layoutAlign = "STRETCH";
    otpContainer.fills = [];

    const sampleDigits = ["9", "2", "6", "0", "1", "5"];
    for (let i = 0; i < 6; i++) {
      const d = figma.createFrame();
      d.resize(44, 50);
      d.cornerRadius = 12;
      d.primaryAxisAlignItems = "CENTER";
      d.counterAxisAlignItems = "CENTER";
      d.layoutMode = "HORIZONTAL";
      d.fills = [hexToRgb("#0B0F17")];
      d.strokes = [hexToRgb("#818CF8")];
      d.strokeWeight = 1.5;
      d.effects = [createDropShadow(0, 0, 0.25, "#818CF8")];
      d.appendChild(makeText(sampleDigits[i], fontBlack, 20, "#F8FAFC"));
      otpContainer.appendChild(d);
    }
    card.appendChild(otpContainer);

    card.appendChild(createPrimaryButton("XÁC NHẬN ĐĂNG NHẬP", true));
    grid2.appendChild(screen);
  }

  masterBoard.appendChild(row2);

  figma.currentPage.appendChild(masterBoard);
  figma.viewport.scrollAndZoomIntoView([masterBoard]);

  figma.closePlugin("✅ Đã dựng thành công 8 màn hình Hi-Fi Authentication chuẩn tỉ lệ!");
})();
