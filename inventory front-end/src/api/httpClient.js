/**
 * 轻量 HTTP 客户端：不依赖 auth store，只做 JSON 解析和基础错误处理。
 * api/request.js 在此基础上加 token/401 处理，stores/auth.js 直接用它做 login/logout。
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

function buildUrl(path, query) {
  const url = API_BASE_URL ? new URL(`${API_BASE_URL}${path}`) : new URL(path, window.location.origin)
  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return
      url.searchParams.set(key, String(value))
    })
  }
  return url.toString()
}

/**
 * 发送 HTTP 请求，自动 JSON 序列化/反序列化。
 * @param {object} opts
 * @param {string} opts.path       - 请求路径（如 /api/user/login）
 * @param {string} [opts.method]   - HTTP 方法，默认 GET
 * @param {object} [opts.query]    - 查询参数
 * @param {*}      [opts.body]     - 请求体（非 FormData 时 JSON 序列化）
 * @param {object} [opts.headers]  - 额外请求头
 * @returns {{ response: Response, data: any }} 包含原始 response 和解析后的 data
 */
export async function httpRequest({ path, method = 'GET', query, body, headers = {} }) {
  const url = buildUrl(path, query)
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData

  const fetchHeaders = { ...headers }
  if (body !== undefined && !isFormData) {
    fetchHeaders['Content-Type'] = 'application/json'
  }

  const response = await fetch(url, {
    method: method.toUpperCase(),
    headers: fetchHeaders,
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  })

  const responseText = await response.text()
  let data = null
  if (responseText) {
    try {
      data = JSON.parse(responseText)
    } catch {
      data = { message: responseText }
    }
  }

  return { response, data }
}
