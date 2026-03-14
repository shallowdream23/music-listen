<script setup>
import PlayerBar from './components/PlayerBar.vue'
import { usePlayerStore } from './store/player'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ref, computed, onMounted, onUnmounted } from 'vue'

const playerStore = usePlayerStore()
const { currentSong } = storeToRefs(playerStore)
const route = useRoute()
const router = useRouter()

const fromRouteName = ref(null)
const unbind = ref(null)

onMounted(() => {
  unbind.value = router.afterEach((to, from) => {
    fromRouteName.value = from.name
  })
})

onUnmounted(() => {
  unbind.value?.()
})

const transitionName = computed(() => {
  if (route.name === 'player' || fromRouteName.value === 'player') return 'player-page'
  return ''
})

const isPlayerPage = computed(() => route.name === 'player')
</script>

<template>
  <div class="app-wrap" :class="{ 'has-player': currentSong && !isPlayerPage }">
    <router-view v-slot="{ Component }">
      <transition :name="transitionName" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <PlayerBar v-show="!isPlayerPage" />
  </div>
</template>

<style scoped>
.app-wrap.has-player {
  padding-bottom: 72px;
}

/* 播放页上拉/下拉过渡 */
.player-page-enter-active,
.player-page-leave-active {
  transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.player-page-enter-from {
  transform: translateY(100%);
}

.player-page-enter-to {
  transform: translateY(0);
}

.player-page-leave-from {
  transform: translateY(0);
}

.player-page-leave-to {
  transform: translateY(100%);
}
</style>
