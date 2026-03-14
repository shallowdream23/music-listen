import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import FollowListView from '../views/FollowListView.vue'
import FollowerListView from '../views/FollowerListView.vue'
import UserHomeView from '../views/UserHomeView.vue'
import PlayerView from '../views/PlayerView.vue'
import PlaylistDetailView from '../views/PlaylistDetailView.vue'
import AlbumDetailView from '../views/AlbumDetailView.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminUserListView from '../views/admin/AdminUserListView.vue'
import AdminSongListView from '../views/admin/AdminSongListView.vue'
import AdminAlbumListView from '../views/admin/AdminAlbumListView.vue'
import { useAuthStore } from '../store/auth'
import { useAdminAuthStore } from '../store/adminAuth'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: '首页 - 在线音乐' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: { requiresAuth: true, title: '个人中心 - 在线音乐' },
  },
  {
    path: '/follow',
    name: 'follow-list',
    component: FollowListView,
    meta: { requiresAuth: true, title: '关注列表 - 在线音乐' },
  },
  {
    path: '/follower',
    name: 'follower-list',
    component: FollowerListView,
    meta: { requiresAuth: true, title: '粉丝列表 - 在线音乐' },
  },
  {
    path: '/user/:id',
    name: 'user-home',
    component: UserHomeView,
    meta: { title: '用户主页 - 在线音乐' },
  },
  {
    path: '/playlist/:id',
    name: 'playlist-detail',
    component: PlaylistDetailView,
    meta: { title: '歌单 - 在线音乐' },
  },
  {
    path: '/album/:id',
    name: 'album-detail',
    component: AlbumDetailView,
    meta: { title: '专辑 - 在线音乐' },
  },
  {
    path: '/player',
    name: 'player',
    component: PlayerView,
    meta: { title: '正在播放 - 在线音乐' },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true, title: '登录 - 在线音乐' },
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { guestOnly: true, title: '注册 - 在线音乐' },
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: AdminLoginView,
    meta: { title: '管理员登录 - 管理后台' },
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdminAuth: true, title: '管理后台 - 在线音乐' },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: AdminDashboardView,
        meta: { title: '仪表盘 - 管理后台' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: AdminUserListView,
        meta: { title: '用户管理 - 管理后台' },
      },
      {
        path: 'songs',
        name: 'admin-songs',
        component: AdminSongListView,
        meta: { title: '歌曲管理 - 管理后台' },
      },
      {
        path: 'albums',
        name: 'admin-albums',
        component: AdminAlbumListView,
        meta: { title: '专辑管理 - 管理后台' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录与管理员校验守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const adminAuthStore = useAdminAuthStore()

  // 前台：已登录但尚未拉取用户信息时拉取一次
  if (authStore.token && !authStore.userInfo) {
    await authStore.fetchUserInfo()
  }

  const requiresAuth = to.matched.some((r) => r.meta?.requiresAuth)
  const requiresAdminAuth = to.matched.some((r) => r.meta?.requiresAdminAuth)

  // 访问 /admin 且未指定子路径时，根据管理员登录状态重定向
  if (to.path === '/admin') {
    if (adminAuthStore.token && !adminAuthStore.userInfo) {
      await adminAuthStore.fetchUserInfo()
    }
    if (adminAuthStore.isAuthenticated && adminAuthStore.isAdmin) {
      next('/admin/dashboard')
      return
    }
    next('/admin/login')
    return
  }

  // 需要管理员登录的页面（/admin/dashboard 等）
  if (requiresAdminAuth) {
    if (adminAuthStore.token && !adminAuthStore.userInfo) {
      await adminAuthStore.fetchUserInfo()
    }
    if (!adminAuthStore.isAuthenticated || !adminAuthStore.isAdmin) {
      next({ name: 'admin-login' })
      return
    }
  }

  // 已登录管理员访问登录页则进后台
  if (to.name === 'admin-login' && adminAuthStore.isAuthenticated && adminAuthStore.isAdmin) {
    next('/admin/dashboard')
    return
  }

  if (requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login' })
    return
  }

  if (to.meta?.guestOnly && authStore.isAuthenticated) {
    next({ name: 'home' })
    return
  }

  next()
})

router.afterEach((to) => {
  if (to.meta?.title) {
    document.title = to.meta.title
  }
})

export default router

