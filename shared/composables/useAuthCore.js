import { ref } from 'vue'

export function useAuthCore({
  fetchCurrentUser,
  loginFn,
  logoutFn,
  clearTokens,
  cacheDuration = 5000,
  onAuthFailure
} = {}) {
  if (typeof fetchCurrentUser !== 'function') {
    throw new Error('fetchCurrentUser must be provided to useAuthCore')
  }

  const currentUser = ref(null)
  const isAuthenticated = ref(false)
  let checkAuthPromise = null
  let lastCheckTime = 0

  const resetState = () => {
    currentUser.value = null
    isAuthenticated.value = false
  }

  const handleAuthFailure = () => {
    if (typeof clearTokens === 'function') {
      clearTokens()
    }
    resetState()
    if (typeof onAuthFailure === 'function') {
      onAuthFailure()
    }
  }

  const shouldUseCache = (force) => {
    if (force) {
      return false
    }
    if (!isAuthenticated.value) {
      return false
    }
    if (cacheDuration <= 0) {
      return false
    }
    return Date.now() - lastCheckTime < cacheDuration
  }

  const checkAuth = async (force = false) => {
    if (shouldUseCache(force)) {
      return true
    }

    if (checkAuthPromise) {
      return checkAuthPromise
    }

    checkAuthPromise = (async () => {
      try {
        const user = await fetchCurrentUser()
        if (user && user.authenticated) {
          currentUser.value = user
          isAuthenticated.value = true
          lastCheckTime = Date.now()
          return true
        }
      } catch (error) {
        console.error('checkAuth failed', error)
      }

      handleAuthFailure()
      lastCheckTime = Date.now()
      return false
    })()

    try {
      return await checkAuthPromise
    } finally {
      checkAuthPromise = null
    }
  }

  const login = async (...args) => {
    if (typeof loginFn !== 'function') {
      throw new Error('loginFn must be provided to useAuthCore')
    }
    const result = await loginFn(...args)
    await checkAuth(true)
    return result
  }

  const logout = async () => {
    if (typeof logoutFn === 'function') {
      try {
        await logoutFn()
      } catch (error) {
        console.error('logout failed', error)
      }
    }
    handleAuthFailure()
  }

  const getUser = () => currentUser.value

  return {
    currentUser,
    isAuthenticated,
    checkAuth,
    login,
    logout,
    getUser
  }
}
