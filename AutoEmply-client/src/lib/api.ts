export class ApiRequestError extends Error {
  status?: number
  details: string[]
  body: unknown
  retryable: boolean

  constructor(message: string, { status, details = [], body = null, retryable = false }: { status?: number; details?: string[]; body?: unknown; retryable?: boolean } = {}) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.details = details
    this.body = body
    this.retryable = retryable
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const API_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 360000)

async function parseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return response.json()
  }
  return response.text()
}

function normalizeDetails(body: unknown): string[] {
  if (!Array.isArray((body as { details?: unknown[] })?.details)) {
    return []
  }

  return ((body as { details: unknown[] }).details).filter((detail): detail is string => typeof detail === 'string' && detail.trim().length > 0)
}

function extractErrorMessage(body: unknown, fallbackMessage: string): string {
  if (typeof body === 'string' && body.trim()) {
    return body
  }
  if (typeof body === 'object' && body !== null && 'error' in body && typeof (body as { error?: unknown }).error === 'string') {
    return (body as { error: string }).error
  }
  return fallbackMessage
}

function isRetryableStatus(status: number): boolean {
  return status === 408 || status === 429 || status === 502 || status === 503 || status === 504
}

async function request(path: string, options: RequestInit = {}, fallbackMessage = '요청 처리에 실패했습니다.'): Promise<Response> {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT_MS)

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      signal: options.signal ?? controller.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiRequestError('요청 시간이 초과되었습니다. 서버 상태를 확인한 뒤 다시 시도하세요.', {
        status: 408,
        retryable: true,
      })
    }

    throw new ApiRequestError('서버에 연결할 수 없습니다. API 주소와 서버 상태를 확인하세요.', {
      body: error,
      retryable: true,
    })
  } finally {
    clearTimeout(timeoutId)
  }

  if (!response.ok) {
    const body = await parseBody(response)
    throw new ApiRequestError(extractErrorMessage(body, fallbackMessage), {
      status: response.status,
      details: normalizeDetails(body),
      body,
      retryable: isRetryableStatus(response.status),
    })
  }

  return response
}

export async function requestJson<T>(path: string, options: RequestInit = {}, fallbackMessage?: string): Promise<T> {
  const response = await request(path, options, fallbackMessage)
  return parseBody(response) as Promise<T>
}

export async function requestBlob(path: string, options: RequestInit = {}, fallbackMessage = '다운로드에 실패했습니다.'): Promise<Blob> {
  const response = await request(path, options, fallbackMessage)
  return response.blob()
}

export function buildApiUrl(path: string): string {
  return `${API_BASE_URL}${path}`
}
