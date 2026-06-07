import { menuService } from '../api/menuService.js'

export const useCurrentOrder = () => ({
  getCurrentOrderByTable: menuService.getCurrentOrderByTable,
})
