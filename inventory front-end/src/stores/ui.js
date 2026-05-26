import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const platformOptions = ['亚马逊', 'eBay']
  const selectedPlatform = ref('eBay')

  return {
    platformOptions,
    selectedPlatform,
  }
})
