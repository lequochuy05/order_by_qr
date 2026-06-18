import { categoryService as categoryApi } from '@entities/category/api/categoryService.js';
import { createCachedRequest, createKeyedCachedRequest } from '@shared/lib/cacheUtils.js';

const CATEGORY_LIST_CACHE_MS = 15_000;

const stableStringify = (value) => {
  if (!value) return 'all';
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  if (typeof value === 'object') {
    return `{${Object.keys(value)
      .sort()
      .map((k) => `${k}:${stableStringify(value[k])}`)
      .join(',')}}`;
  }
  return String(value);
};

const normalizePageContent = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
};

const emptyPage = (page = 0, size = 24) => ({
  content: [],
  number: page,
  size,
  totalElements: 0,
  totalPages: 0,
});

const normalizePage = (data, page = 0, size = 24) => {
  if (Array.isArray(data)) {
    return {
      ...emptyPage(page, size),
      content: data,
      totalElements: data.length,
      totalPages: data.length > 0 ? 1 : 0,
    };
  }

  return {
    ...emptyPage(page, size),
    ...data,
    content: normalizePageContent(data),
  };
};

const { requestFn: getCategoryList, clearCache: clearCategoryListCache } = createCachedRequest(
  (options = {}) =>
    categoryApi
      .getAll({ size: 1000, sort: 'displayOrder,asc', ...options })
      .then(normalizePageContent),
  CATEGORY_LIST_CACHE_MS,
);

const { requestFn: getCategoryPage, clearCache: clearCategoryPageCache } = createKeyedCachedRequest(
  (params = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    const requestParams = {
      page,
      size,
      sort: 'displayOrder,asc',
      ...params,
    };
    if (!requestParams.q?.trim()) delete requestParams.q;

    return categoryApi.getPage(requestParams).then((data) => normalizePage(data, page, size));
  },
  CATEGORY_LIST_CACHE_MS,
  stableStringify,
);

const clearCategoryCache = () => {
  clearCategoryListCache();
  clearCategoryPageCache();
};

const toCategoryPayload = (data) => {
  if (typeof data === 'string') {
    return { name: data };
  }

  return {
    name: data.name,
    img: data.img || null,
    description: data.description?.trim() || null,
    active: data.active ?? true,
    displayOrder: Number(data.displayOrder) || 0,
  };
};

const toImageFormData = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
};

const mutateCategory = async (mutation) => {
  clearCategoryCache();
  const result = await mutation();
  clearCategoryCache();
  return result;
};

export const categoryService = {
  getAll: getCategoryList,

  getPage: (params, options) => getCategoryPage(params, options),

  search: (q, page = 0, size = 12) => categoryApi.search(q, page, size),

  create: (data) => mutateCategory(() => categoryApi.create(toCategoryPayload(data))),

  update: (id, data) => mutateCategory(() => categoryApi.update(id, toCategoryPayload(data))),

  uploadImage: (id, file) =>
    mutateCategory(() => categoryApi.uploadImage(id, toImageFormData(file))),

  delete: (id) => mutateCategory(() => categoryApi.delete(id)),
};
