import { menuService } from '../api/menuService.js'

export const useCreateOrder = () => ({
  createOrder: menuService.createOrder,
})
