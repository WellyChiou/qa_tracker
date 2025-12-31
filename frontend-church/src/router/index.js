import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue')
  },
  {
    path: '/activities',
    name: 'Activities',
    component: () => import('@/views/Activities.vue')
  },
  {
    path: '/contact',
    name: 'Contact',
    component: () => import('@/views/Contact.vue')
  },
  {
    path: '/service-schedule',
    name: 'ServiceSchedule',
    component: () => import('@/views/ServiceSchedule.vue')
  },
  {
    path: '/sunday-messages',
    name: 'SundayMessages',
    component: () => import('@/views/SundayMessages.vue')
  },
  {
    path: '/groups',
    name: 'Groups',
    component: () => import('@/views/Groups.vue')
  },
  {
    path: '/prayer-requests',
    name: 'PrayerRequests',
    component: () => import('@/views/PrayerRequests.vue')
  },
  {
    path: '/announcements',
    name: 'Announcements',
    component: () => import('@/views/Announcements.vue')
  }
]

const router = createRouter({
  history: createWebHistory('/church/'),
  routes
})

export default router
