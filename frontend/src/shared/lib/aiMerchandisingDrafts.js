export const comboProposalToDraft = (proposal, index = 0) => {
  const safeName = String(proposal?.name || '').trim();
  const slug = safeName
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 24);

  return {
    _draftKey: `ai-combo-${index}-${slug || 'draft'}`,
    name: safeName,
    description: String(proposal?.description || '').trim(),
    price: Math.max(0, Number(proposal?.suggestedPrice ?? 0)),
    active: true,
    available: true,
    displayOrder: 0,
    items: (proposal?.items || [])
      .filter((item) => Number(item?.menuItemId) > 0)
      .map((item) => ({
        menuItemId: Number(item.menuItemId),
        quantity: Math.max(1, Number(item.quantity || 1)),
      })),
  };
};

export const promotionProposalToDraft = (proposal, index = 0) => {
  const safeName = String(proposal?.name || '').trim();
  const slug = safeName
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 24);

  return {
    _draftKey: `ai-promo-${index}-${slug || 'draft'}`,
    name: safeName,
    discountPercent: Math.max(5, Math.min(40, Number(proposal?.discountPercent ?? 10))),
    startTime: proposal?.startTime || '14:00',
    endTime: proposal?.endTime || '17:00',
    daysOfWeek: Array.isArray(proposal?.daysOfWeek) ? [...proposal.daysOfWeek] : [],
    active: true,
  };
};
