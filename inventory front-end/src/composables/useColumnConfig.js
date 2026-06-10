import { computed, ref, watch } from 'vue'
import { loadColumnConfig, saveColumnConfig } from '@/api/userColumnConfig'

export function useColumnConfig(fixedKeys, allColumns) {
  const columnKeys = Object.keys(allColumns)
  const showDrawer = ref(false)
  const visibleKeys = ref([...columnKeys])
  const editingKeys = ref([...columnKeys])
  const dragIdx = ref(-1)

  async function init(pageKey) {
    const saved = await loadColumnConfig(pageKey)
    if (saved && saved.length > 0) {
      const f = fixedKeys.filter(k => saved.includes(k))
      const r = saved.filter(k => !fixedKeys.includes(k))
      visibleKeys.value = [...f, ...r]
    } else visibleKeys.value = [...columnKeys]
    editingKeys.value = [...visibleKeys.value]
  }

  function openDrawer() { editingKeys.value = [...visibleKeys.value]; showDrawer.value = true }

  async function apply(pageKey, msg) {
    visibleKeys.value = [...editingKeys.value]
    await saveColumnConfig(visibleKeys.value, pageKey)
    showDrawer.value = false
    if (msg) msg.success('列设置已保存')
  }

  const leftCols = computed(() =>
    columnKeys.map(k => ({ key: k, title: allColumns[k], disabled: fixedKeys.includes(k), checked: editingKeys.value.includes(k) }))
  )
  const selCols = computed(() =>
    editingKeys.value.map(k => ({ key: k, title: allColumns[k] || k, fixed: fixedKeys.includes(k) }))
  )
  const isAllChecked = computed(() => leftCols.value.filter(c => !c.disabled).every(c => c.checked))

  function toggleAll() {
    if (isAllChecked.value) editingKeys.value = [...fixedKeys]
    else editingKeys.value = [...columnKeys]
  }
  function toggleColumn(key) {
    if (fixedKeys.includes(key)) return
    const i = editingKeys.value.indexOf(key)
    if (i >= 0) editingKeys.value.splice(i, 1); else editingKeys.value.push(key)
  }
  function onDragStart(i) { dragIdx.value = i }
  function onDragOver(e) { e.preventDefault() }
  function onDrop(t) {
    const f = dragIdx.value; if (f < 0 || f === t) return
    const a = editingKeys.value; const item = a[f]
    if (fixedKeys.includes(item) || t < fixedKeys.length) return
    a.splice(f, 1); a.splice(t, 0, item); dragIdx.value = -1
  }
  function onDragEnd() { dragIdx.value = -1 }

  return { showDrawer, visibleKeys, editingKeys, leftCols, selCols, isAllChecked, dragIdx,
    init, openDrawer, apply, toggleAll, toggleColumn, onDragStart, onDragOver, onDrop, onDragEnd }
}
