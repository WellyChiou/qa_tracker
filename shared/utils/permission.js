const toArray = (value) => {
  if (Array.isArray(value)) {
    return value
  }
  if (value === undefined || value === null) {
    return []
  }
  return [value]
}

export function hasPermission(user, code) {
  if (!user || !code) {
    return false
  }
  const permissions = Array.isArray(user.permissions) ? user.permissions : []
  return permissions.includes(code)
}

export function hasAnyPermission(user, codes) {
  if (!user || !codes) {
    return false
  }
  const permissions = Array.isArray(user.permissions) ? user.permissions : []
  return toArray(codes).some(code => permissions.includes(code))
}

export function hasRole(user, role) {
  if (!user || !role) {
    return false
  }
  const roles = Array.isArray(user.roles) ? user.roles : []
  return toArray(role).some(target => roles.includes(target))
}
