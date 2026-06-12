import { menuService } from '../api/menuService.js'

export const useCurrentOrder = () => ({
  getCurrentOrderByTableCode: menuService.getCurrentOrderByTableCode,
})
