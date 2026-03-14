import { defineStore } from 'pinia'

const IDB_NAME = 'music_download_db'
const IDB_STORE = 'settings'
const IDB_KEY = 'directoryHandle'

function openDB() {
  return new Promise((resolve, reject) => {
    const r = indexedDB.open(IDB_NAME, 1)
    r.onerror = () => reject(r.error)
    r.onsuccess = () => resolve(r.result)
    r.onupgradeneeded = (e) => {
      if (!e.target.result.objectStoreNames.contains(IDB_STORE)) {
        e.target.result.createObjectStore(IDB_STORE)
      }
    }
  })
}

function saveHandleToIDB(handle) {
  return openDB().then((db) => {
    return new Promise((resolve, reject) => {
      const tx = db.transaction(IDB_STORE, 'readwrite')
      const store = tx.objectStore(IDB_STORE)
      store.put(handle, IDB_KEY)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  })
}

function loadHandleFromIDB() {
  return openDB().then((db) => {
    return new Promise((resolve, reject) => {
      const tx = db.transaction(IDB_STORE, 'readonly')
      const req = tx.objectStore(IDB_STORE).get(IDB_KEY)
      req.onsuccess = () => resolve(req.result || null)
      req.onerror = () => reject(req.error)
    })
  })
}

function removeHandleFromIDB() {
  return openDB().then((db) => {
    return new Promise((resolve, reject) => {
      const tx = db.transaction(IDB_STORE, 'readwrite')
      tx.objectStore(IDB_STORE).delete(IDB_KEY)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  })
}

export const useDownloadStore = defineStore('download', {
  state: () => ({
    directoryHandle: null,
    directoryName: '',
  }),
  getters: {
    hasChosenFolder: (state) => !!state.directoryHandle,
  },
  actions: {
    async chooseFolder() {
      if (typeof window === 'undefined' || !window.showDirectoryPicker) {
        return { ok: false, message: '当前浏览器不支持选择文件夹，请使用 Chrome 或 Edge' }
      }
      try {
        const handle = await window.showDirectoryPicker()
        const name = handle.name || ''
        this.directoryHandle = handle
        this.directoryName = name
        try {
          await saveHandleToIDB(handle)
        } catch (e) {
          console.warn('保存下载目录到本地失败', e)
        }
        return { ok: true, name }
      } catch (e) {
        if (e.name === 'AbortError') {
          return { ok: false, message: '已取消选择' }
        }
        return { ok: false, message: e.message || '选择失败' }
      }
    },
    async loadStoredHandle() {
      try {
        const handle = await loadHandleFromIDB()
        if (handle) {
          const permission = await handle.requestPermission?.({ mode: 'readwrite' }).catch(() => null)
          if (permission === 'granted' || permission === null) {
            this.directoryHandle = handle
            this.directoryName = handle.name || ''
            return true
          }
        }
      } catch (_) {}
      this.directoryHandle = null
      this.directoryName = ''
      return false
    },
    async clearFolder() {
      this.directoryHandle = null
      this.directoryName = ''
      try {
        await removeHandleFromIDB()
      } catch (_) {}
    },
    async getWriteableHandle() {
      if (!this.directoryHandle) return null
      try {
        const permission = await this.directoryHandle.requestPermission?.({ mode: 'readwrite' }).catch(() => null)
        if (permission === 'denied') return null
        return this.directoryHandle
      } catch (_) {
        return null
      }
    },
  },
})
