import { hasPermission, hasAnyPermission, hasRole } from './permission'

const normalizeRedirect = (target) => {
  if (!target) {
    return { path: '/' }
  }
  if (typeof target === 'string') {
    return { path: target }
  }
  return target
}

const evaluateRoutePermissions = (user, route) => {
  if (!route?.meta) {
    return true
  }

  const { requiredPermission, requiredPermissions, requiredRole } = route.meta

  if (requiredPermission && !hasPermission(user, requiredPermission)) {
    return false
  }

  if (requiredPermissions && !hasAnyPermission(user, requiredPermissions)) {
    return false
  }

  if (requiredRole && !hasRole(user, requiredRole)) {
    return false
  }

  return true
}

export function createAuthGuard({
  checkAuth,
  currentUser,
  loginRouteName,
  authenticatedRedirect,
  strictPermissions = false
} = {}) {
  if (typeof checkAuth !== 'function') {
    throw new Error('checkAuth is required for createAuthGuard')
  }
  if (!loginRouteName) {
    throw new Error('loginRouteName is required for createAuthGuard')
  }

  const homeRedirect = normalizeRedirect(authenticatedRedirect)

  return async (to, from, next) => {
    if (to.name === loginRouteName) {
      const isAuth = await checkAuth()
      if (isAuth) {
        const redirectTarget = to.query.redirect || homeRedirect
        next(normalizeRedirect(redirectTarget))
        return
      }
      next()
      return
    }

    if (to.meta?.requiresAuth) {
      const isAuth = await checkAuth()
      if (!isAuth) {
        next({ name: loginRouteName, query: { redirect: to.fullPath } })
        return
      }

      const userValue = currentUser ? currentUser.value : null
      const hasAccess = evaluateRoutePermissions(userValue, to)
      if (!hasAccess && strictPermissions) {
        console.warn(`Permission guard blocked navigation to ${to.fullPath}`)
        next(homeRedirect)
        return
      }
    }

    next()
  }
}
