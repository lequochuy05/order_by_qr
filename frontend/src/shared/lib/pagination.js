export const normalizePageContent = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
};

export const emptyPage = (page = 0, size = 24) => ({
  content: [],
  number: page,
  size,
  totalElements: 0,
  totalPages: 0,
});

export const normalizePage = (data, page = 0, size = 24, itemMapper = (item) => item) => {
  if (Array.isArray(data)) {
    return {
      ...emptyPage(page, size),
      content: data.map(itemMapper),
      totalElements: data.length,
      totalPages: data.length > 0 ? 1 : 0,
    };
  }
  return {
    ...emptyPage(page, size),
    ...data,
    content: normalizePageContent(data).map(itemMapper),
  };
};
