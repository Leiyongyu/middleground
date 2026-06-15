import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const platformOptions = ['eBay', '亚马逊']
  const selectedPlatform = ref('eBay')

  function setPlatform(platform) {
    selectedPlatform.value = platform
  }

  return {
    platformOptions,
    selectedPlatform,
    setPlatform,
  }
})
