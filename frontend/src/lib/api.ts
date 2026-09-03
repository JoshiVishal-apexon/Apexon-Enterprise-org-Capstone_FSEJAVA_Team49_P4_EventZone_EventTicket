const API_BASE = (import.meta as any).env.VITE_API_BASE || ''

/** Shape the backend's GlobalExceptionHandler returns for every failure. */
export type ApiError = {
  timestamp: string
  path: string
  error: string
  message: string
}

type Options = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  token?: string
}

/**
 * Single place that knows how to talk to the API. It rethrows the backend's
 * own `message` so callers can show a useful reason ("This event has bookings
 * and cannot be deleted") instead of a generic failure.
 */
export async function apiFetch<T>(path: string, opts: Options = {}): Promise<T> {
  const res = await fetch(API_BASE + path, {
    method: opts.method ?? 'GET',
    headers: {
      ...(opts.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(opts.token ? { Authorization: 'Bearer ' + opts.token } : {}),
    },
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  })

  // 204 (delete, logout) has no body to parse.
  if (res.status === 204) return undefined as T

  const text = await res.text()
  let data: unknown = undefined
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      // Fall through: a non-JSON body still needs to produce a sane error.
    }
  }

  if (!res.ok) {
    const message = (data as ApiError | undefined)?.message
    throw new Error(message || `Request failed (${res.status})`)
  }

  return data as T
}

export type TicketCategory = {
  id: string
  name: string
  price: number
  totalSeats: number
  availableSeats: number
  bookedQuantity: number
}

export type EventItem = {
  id: string
  title: string
  description: string
  eventDate: string
  venue: string
  coverImageUrl?: string
  organiserId?: string
  organiserName?: string
  categoryId?: string
  categoryName?: string
  active: boolean
  ticketCategories: TicketCategory[]
}

export type Category = { id: string; name: string }

export type Role = 'ATTENDEE' | 'ORGANISER' | 'ADMIN'

export type AppUser = {
  id: string
  email: string
  name: string
  role: Role
}
