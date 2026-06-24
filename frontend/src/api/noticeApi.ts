import { http, publicHttp } from './http'

export interface Notice {
  noticeId:  number
  title:     string
  writer:    string
  content:   string
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content:    T[]
  page:       number
  size:       number
  totalCount: number
}

export async function getNotices(page = 1, size = 5): Promise<PageResponse<Notice>> {
  const res = await publicHttp.get<PageResponse<Notice>>('/notices', { params: { page, size } })
  return res.data
}

export async function getNotice(id: number): Promise<Notice> {
  const res = await publicHttp.get<Notice>(`/notices/${id}`)
  return res.data
}

export const createNotice = (title: string, content: string) =>
  http.post<Notice>('/notices', { title, content }).then(r => r.data)

export const updateNotice = (id: number, title: string, content: string) =>
  http.put<Notice>(`/notices/${id}`, { title, content }).then(r => r.data)

export const deleteNotice = (id: number) =>
  http.delete(`/notices/${id}`)
