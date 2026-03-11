function defaultParseResponse(response, apiResponse) {
  if (apiResponse && typeof apiResponse.success === 'boolean') {
    if (!apiResponse.success) {
      const error = new Error(apiResponse.message || '請求失敗')
      error.response = response
      throw error
    }

    return apiResponse.data !== undefined ? apiResponse.data : apiResponse
  }

  return apiResponse
}

export function createApiClient(config) {
  const loadingCallbacks = {
    show: null,
    hide: null
  }

  const getApiBaseUrl = () => config.getApiBaseUrl()

  const getAccessToken = () => localStorage.getItem(config.accessTokenKey)

  const getRefreshToken = () => {
    if (!config.refreshTokenKey) {
      return null
    }
    return localStorage.getItem(config.refreshTokenKey)
  }

  const setTokens = (accessToken, refreshToken) => {
    if (accessToken) {
      localStorage.setItem(config.accessTokenKey, accessToken)
    } else {
      localStorage.removeItem(config.accessTokenKey)
    }

    if (!config.refreshTokenKey) {
      return
    }

    if (refreshToken) {
      localStorage.setItem(config.refreshTokenKey, refreshToken)
    } else {
      localStorage.removeItem(config.refreshTokenKey)
    }
  }

  const clearTokens = () => {
    localStorage.removeItem(config.accessTokenKey)
    if (config.refreshTokenKey) {
      localStorage.removeItem(config.refreshTokenKey)
    }
  }

  const setLoadingCallbacks = (show, hide) => {
    loadingCallbacks.show = show
    loadingCallbacks.hide = hide
  }

  const parseResponse = async (response) => {
    try {
      const apiResponse = await response.json()

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          return null
        }

        const error = new Error(
          apiResponse?.message || `API 請求失敗: ${response.status} ${response.statusText}`
        )
        error.response = response
        throw error
      }

      return (config.parseResponse || defaultParseResponse)(response, apiResponse)
    } catch (error) {
      if (error instanceof SyntaxError) {
        if (response.status === 401 || response.status === 403) {
          return null
        }

        if (response.status === 200 || response.status === 201 || response.status === 204) {
          return {}
        }

        const parseError = new Error(`API 請求失敗: ${response.status} ${response.statusText}`)
        parseError.response = response
        throw parseError
      }

      throw error
    }
  }

  const refreshAccessToken = async () => {
    if (!config.refreshTokenKey || !config.refreshEndpoint) {
      throw new Error('未配置 Refresh Token 功能')
    }

    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      throw new Error('沒有 Refresh Token')
    }

    try {
      const response = await fetch(`${getApiBaseUrl()}${config.refreshEndpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(
          config.buildRefreshBody ? config.buildRefreshBody(refreshToken) : { refreshToken }
        ),
        credentials: config.refreshCredentials || config.credentials || 'omit'
      })

      if (!response.ok) {
        throw new Error('刷新 Token 失敗')
      }

      const json = await response.json()
      const data = json && json.data !== undefined ? json.data : json
      if (data && data.accessToken) {
        localStorage.setItem(config.accessTokenKey, data.accessToken)
        return data.accessToken
      }

      throw new Error('刷新 Token 響應無效')
    } catch (error) {
      clearTokens()
      throw error
    }
  }

  const request = async (url, options = {}, runtime = {}) => {
    const {
      loadingMessage = '載入中...',
      showLoading = false,
      needsAuth = false
    } = runtime

    const apiUrl = url.startsWith('http')
      ? url
      : `${getApiBaseUrl()}${url.startsWith('/') ? url : `/${url}`}`

    if (showLoading && loadingCallbacks.show) {
      loadingCallbacks.show(loadingMessage)
    }

    const fetchOptions = { ...options }
    delete fetchOptions.showLoading
    delete fetchOptions.loadingMessage

    try {
      const isFormData = fetchOptions.body instanceof FormData
      const headers = { ...(fetchOptions.headers || {}) }

      if (!isFormData && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json'
      }

      const needsToken = config.shouldAttachToken
        ? config.shouldAttachToken(url, needsAuth)
        : needsAuth

      if (needsToken) {
        const token = getAccessToken()
        if (token) {
          headers.Authorization = `Bearer ${token}`
        }
      }

      let response = await fetch(apiUrl, {
        ...fetchOptions,
        headers,
        credentials: fetchOptions.credentials || config.credentials || 'omit'
      })

      if (config.rejectRedirected && response.redirected) {
        throw new Error('請求被重定向，可能是認證失敗')
      }

      if (response.status === 401 && needsToken && config.refreshEndpoint) {
        const refreshToken = getRefreshToken()
        if (refreshToken) {
          try {
            const newAccessToken = await refreshAccessToken()
            headers.Authorization = `Bearer ${newAccessToken}`
            response = await fetch(apiUrl, {
              ...fetchOptions,
              headers,
              credentials: fetchOptions.credentials || config.credentials || 'omit'
            })
          } catch (error) {
            clearTokens()
            if (config.isAuthRelatedRequest && config.isAuthRelatedRequest(url)) {
              return null
            }
            throw new Error(config.authExpiredMessage || '認證已過期，請重新登入')
          }
        } else {
          clearTokens()
        }
      }

      return await parseResponse(response)
    } finally {
      if (showLoading && loadingCallbacks.hide) {
        loadingCallbacks.hide()
      }
    }
  }

  return {
    clearTokens,
    getAccessToken,
    getApiBaseUrl,
    getRefreshToken,
    refreshAccessToken,
    request,
    setLoadingCallbacks,
    setTokens
  }
}
