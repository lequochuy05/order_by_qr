export const RISK_LEVELS = {
  CRITICAL: 'CRITICAL',
  HIGH: 'HIGH',
  NORMAL: 'NORMAL',
};

const ALLERGEN_RULES = [
  {
    code: 'ALLERGY',
    foldedKeywords: ['di ung', 'allergy', 'allergic', 'anaphylaxis', 'soc phan ve'],
    rawKeywords: ['dị ứng'],
  },
  {
    code: 'PEANUT_NUTS',
    foldedKeywords: [
      'dau phong',
      'cu lac',
      'dau lac',
      'hat dieu',
      'hanh nhan',
      'peanut',
      'peanuts',
      'almond',
      'cashew',
      'walnut',
    ],
    rawKeywords: ['đậu phộng', 'lạc', 'hạt điều', 'hạnh nhân'],
  },
  {
    code: 'SEAFOOD',
    foldedKeywords: [
      'hai san',
      'seafood',
      'shellfish',
      'tom hum',
      'tom su',
      'nuoc mam',
      'mam tom',
      'shrimp',
      'prawn',
      'crab',
      'lobster',
      'squid',
      'oyster',
    ],
    rawKeywords: ['hải sản', 'tôm', 'cua', 'mực', 'ghẹ', 'sò', 'ngao', 'nghêu', 'ốc', 'hến'],
  },
  {
    code: 'GLUTEN',
    foldedKeywords: ['gluten', 'celiac', 'lua mi', 'bot mi', 'wheat', 'barley'],
    rawKeywords: ['lúa mì', 'bột mì'],
  },
  {
    code: 'DAIRY',
    foldedKeywords: [
      'sua bo',
      'sua tuoi',
      'sua dac',
      'lactose',
      'dairy',
      'pho mai',
      'milk',
      'cheese',
      'butter',
    ],
    rawKeywords: ['sữa', 'phô mai'],
  },
  {
    code: 'EGG',
    foldedKeywords: ['trung ga', 'trung vit', 'trung cut', 'long trang', 'long do', 'egg', 'eggs'],
    rawKeywords: ['trứng'],
  },
  {
    code: 'MSG',
    foldedKeywords: ['mi chinh', 'bot ngot', 'msg'],
    rawKeywords: ['mì chính', 'bột ngọt'],
  },
  {
    code: 'SESAME_SOY',
    foldedKeywords: [
      'me den',
      'me rang',
      'hat me',
      'hat vung',
      'dau vung',
      'dau nanh',
      'tuong nanh',
      'sesame',
      'soy',
      'soybean',
      'tofu',
    ],
    rawKeywords: ['vừng', 'mè', 'đậu nành'],
  },
];

const URGENCY_RULES = [
  {
    code: 'URGENT',
    foldedKeywords: [
      'can gap',
      'lam gap',
      'nau gap',
      'lam nhanh',
      'ra nhanh',
      'len nhanh',
      'khan cap',
      'uu tien',
      'urgent',
      'asap',
      'hurry',
    ],
    rawKeywords: ['gấp', 'vội', 'khẩn', 'nhanh'],
  },
  {
    code: 'KIDS_ELDERLY_PREGNANT',
    foldedKeywords: [
      'tre em',
      'em be',
      'be nho',
      'mang thai',
      'ba bau',
      'nguoi gia',
      'nguoi lon tuoi',
      'toddler',
      'kid',
      'child',
      'pregnant',
      'elderly',
    ],
    rawKeywords: [],
  },
];

const DIETARY_RULES = [
  { code: 'NO_SPICY', foldedKeywords: ['khong cay', 'it cay', 'no spicy', 'mild'], rawKeywords: [] },
  { code: 'VEGETARIAN', foldedKeywords: ['an chay', 'vegetarian', 'vegan', 'thuan chay'], rawKeywords: [] },
  {
    code: 'LESS_SEASONING',
    foldedKeywords: [
      'it duong',
      'khong duong',
      'it da',
      'khong da',
      'khong hanh',
      'khong ngo',
      'khong rau',
      'no onion',
      'less sugar',
      'no ice',
    ],
    rawKeywords: [],
  },
  {
    code: 'NO_PORK_BEEF',
    foldedKeywords: ['khong thit heo', 'khong thit lon', 'khong thit bo', 'halal', 'no pork', 'no beef'],
    rawKeywords: [],
  },
];

export const ALLERGEN_LABELS = {
  ALLERGY: { vi: 'Dị ứng', en: 'Allergy' },
  PEANUT_NUTS: { vi: 'Đậu phộng / Hạt', en: 'Peanuts / Nuts' },
  SEAFOOD: { vi: 'Hải sản', en: 'Seafood / Shellfish' },
  SHELLFISH: { vi: 'Động vật có vỏ', en: 'Shellfish' },
  GLUTEN: { vi: 'Gluten / Lúa mì', en: 'Gluten / Wheat' },
  DAIRY: { vi: 'Sữa / Lactose', en: 'Dairy' },
  EGG: { vi: 'Trứng', en: 'Egg' },
  MSG: { vi: 'Bột ngọt (MSG)', en: 'MSG' },
  SESAME_SOY: { vi: 'Vừng / Đậu nành', en: 'Sesame / Soy' },
  SESAME: { vi: 'Vừng / Mè', en: 'Sesame' },
};

export const URGENCY_LABELS = {
  URGENT: { vi: 'Cần gấp', en: 'Urgent' },
  KIDS_ELDERLY_PREGNANT: { vi: 'Trẻ em / Người lớn tuổi / Thai phụ', en: 'Kids / Elderly / Pregnant' },
};

export const DIETARY_LABELS = {
  NO_SPICY: { vi: 'Ít / Không cay', en: 'Mild / No spicy' },
  VEGETARIAN: { vi: 'Ăn chay', en: 'Vegetarian' },
  LESS_SEASONING: { vi: 'Giảm gia vị / đá', en: 'Custom seasoning / ice' },
  NO_PORK_BEEF: { vi: 'Kiêng thịt heo / bò', en: 'No pork / beef' },
};

export const foldVietnameseAccents = (input) => {
  if (!input || typeof input !== 'string') {
    return '';
  }
  return input
    .replaceAll('đ', 'd')
    .replaceAll('Đ', 'D')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ');
};

const padAsciiWords = (normalized) =>
  ` ${normalized.replace(/[^a-z0-9 ]/g, ' ').replace(/\s+/g, ' ').trim()} `;

const padUnicodeWords = (rawNote) =>
  ` ${rawNote.normalize('NFC').toLowerCase().replace(/[^\p{L}\p{N}\s]+/gu, ' ').replace(/\s+/g, ' ').trim()} `;

const matchRules = (foldedPadded, rawLowerPadded, rules) =>
  rules
    .filter(
      (rule) =>
        rule.foldedKeywords.some((kw) => foldedPadded.includes(` ${kw} `)) ||
        rule.rawKeywords.some((kw) => rawLowerPadded.includes(` ${kw} `)),
    )
    .map((rule) => rule.code);

export const riskRank = (riskLevel) => {
  const upper = String(riskLevel || '').toUpperCase();
  if (upper === RISK_LEVELS.CRITICAL) return 3;
  if (upper === RISK_LEVELS.HIGH) return 2;
  return 1;
};

export const maxRisk = (first, second) => (riskRank(first) >= riskRank(second) ? first || RISK_LEVELS.NORMAL : second);

export const analyzeCustomerNote = (rawNote) => {
  if (!rawNote || typeof rawNote !== 'string' || !rawNote.trim()) {
    return {
      riskLevel: RISK_LEVELS.NORMAL,
      allergens: [],
      urgencyFlags: [],
      dietaryFlags: [],
      hasAlerts: false,
    };
  }

  const foldedPadded = padAsciiWords(foldVietnameseAccents(rawNote));
  const rawLowerPadded = padUnicodeWords(rawNote);
  const allergens = matchRules(foldedPadded, rawLowerPadded, ALLERGEN_RULES);
  const urgencyFlags = matchRules(foldedPadded, rawLowerPadded, URGENCY_RULES);
  const dietaryFlags = matchRules(foldedPadded, rawLowerPadded, DIETARY_RULES);

  let riskLevel = RISK_LEVELS.NORMAL;
  if (allergens.length > 0) {
    riskLevel = RISK_LEVELS.CRITICAL;
  } else if (urgencyFlags.length > 0) {
    riskLevel = RISK_LEVELS.HIGH;
  }

  return {
    riskLevel,
    allergens,
    urgencyFlags,
    dietaryFlags,
    hasAlerts: allergens.length > 0 || urgencyFlags.length > 0 || dietaryFlags.length > 0,
  };
};

export const getSafetyFlagLabel = (code, language = 'vi') => {
  const lang = language === 'en' ? 'en' : 'vi';
  const entry = ALLERGEN_LABELS[code] || URGENCY_LABELS[code] || DIETARY_LABELS[code];
  return entry ? entry[lang] : code;
};
