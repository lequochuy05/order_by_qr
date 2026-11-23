const VALID_TYPES = new Set(['MENU_ITEM', 'COMBO']);
const MAX_SUGGESTIONS = 5;
const MAX_QUANTITY = 10;

export const normalizeSuggestionType = (rawType) => {
  if (typeof rawType !== 'string') return null;
  const upper = rawType.trim().toUpperCase();
  if (upper === 'COMBO') return 'COMBO';
  if (upper === 'MENU_ITEM' || upper === 'ITEM') return 'MENU_ITEM';
  return VALID_TYPES.has(upper) ? upper : null;
};

export const normalizeChatSuggestions = (rawSuggestions) => {
  if (!Array.isArray(rawSuggestions) || rawSuggestions.length === 0) {
    return [];
  }

  const normalized = [];
  const seenKeys = new Set();

  for (const item of rawSuggestions) {
    if (normalized.length >= MAX_SUGGESTIONS) break;
    if (!item || typeof item !== 'object') continue;

    const type = normalizeSuggestionType(item.type);
    const numId = Number(item.id);
    const name = typeof item.name === 'string' ? item.name.trim() : '';

    if (!type || !Number.isInteger(numId) || numId <= 0 || !name) {
      continue;
    }

    const dedupKey = `${type}:${numId}`;
    if (seenKeys.has(dedupKey)) {
      continue;
    }
    seenKeys.add(dedupKey);

    const parsedPrice = Number(item.price);
    const price = Number.isFinite(parsedPrice) && parsedPrice >= 0 ? parsedPrice : 0;
    const rawQty = Number(item.quantity);
    const quantity = Number.isInteger(rawQty) && rawQty >= 1 ? Math.min(rawQty, MAX_QUANTITY) : 1;

    normalized.push({
      type,
      id: numId,
      name,
      price,
      imageUrl: typeof item.imageUrl === 'string' && item.imageUrl.trim() ? item.imageUrl.trim() : null,
      categoryName:
        typeof item.categoryName === 'string' && item.categoryName.trim()
          ? item.categoryName.trim()
          : null,
      quantity,
      reason: typeof item.reason === 'string' ? item.reason.trim() : '',
      hasOptions: Boolean(item.hasOptions),
      available: item.available !== false,
    });
  }

  return normalized;
};

export const getQuickAddableSuggestions = (suggestions) =>
  Array.isArray(suggestions)
    ? suggestions.filter((item) => item && item.available !== false && !item.hasOptions)
    : [];
