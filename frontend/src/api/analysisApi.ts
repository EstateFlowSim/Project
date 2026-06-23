import axios from 'axios'
import type {
  BaseResponse,
  EventsListResponse,
  EventWindowRequest,
  EventWindowResponse,
} from '@/types/analysis'

const http = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

export async function getEvents(): Promise<BaseResponse<EventsListResponse>> {
  const res = await http.get<BaseResponse<EventsListResponse>>('/analysis/events')
  return res.data
}

export async function postEventWindow(
  req: EventWindowRequest,
): Promise<BaseResponse<EventWindowResponse>> {
  const res = await http.post<BaseResponse<EventWindowResponse>>(
    '/analysis/event-window',
    req,
  )
  return res.data
}
