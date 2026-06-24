import { http } from './http'
import type {
  BaseResponse,
  EventsListResponse,
  EventWindowRequest,
  EventWindowResponse,
} from '@/types/analysis'

export async function getEvents(): Promise<BaseResponse<EventsListResponse>> {
  const res = await http.get<BaseResponse<EventsListResponse>>('/analysis/events', { timeout: 30_000 })
  return res.data
}

export async function postEventWindow(
  req: EventWindowRequest,
): Promise<BaseResponse<EventWindowResponse>> {
  const res = await http.post<BaseResponse<EventWindowResponse>>(
    '/analysis/event-window',
    req,
    { timeout: 30_000 },
  )
  return res.data
}
