import { http } from './http'
import type { PageResponse } from '@/api/noticeApi'

export interface Qna {
  qnaId:     number
  title:     string
  writerId:  number
  writer:    string
  content:   string
  answered:  boolean
  createdAt: string
  updatedAt: string
}

export interface QnaComment {
  commentId: number
  qnaId:     number
  content:   string
  writer:    string
  writerId:  number
  createdAt: string
  updatedAt: string
}

export const getQnas    = (page = 1, size = 10) =>
  http.get<PageResponse<Qna>>('/qnas', { params: { page, size } }).then(r => r.data)

export const getQna     = (id: number) =>
  http.get<Qna>(`/qnas/${id}`).then(r => r.data)

export const createQna  = (title: string, content: string) =>
  http.post('/qnas', { title, content })

export const updateQna  = (id: number, title: string, content: string) =>
  http.put(`/qnas/${id}`, { title, content })

export const deleteQna  = (id: number) =>
  http.delete(`/qnas/${id}`)

export const getComments   = (qnaId: number) =>
  http.get<QnaComment[]>(`/qnas/${qnaId}/comments`).then(r => r.data)

export const createComment = (qnaId: number, content: string) =>
  http.post(`/qnas/${qnaId}/comments`, { content })

export const updateComment = (qnaId: number, commentId: number, content: string) =>
  http.put(`/qnas/${qnaId}/comments/${commentId}`, { content })

export const deleteComment = (qnaId: number, commentId: number) =>
  http.delete(`/qnas/${qnaId}/comments/${commentId}`)

export const updateAnswered = (id: number, answered: boolean) =>
  http.patch(`/qnas/${id}/answered`, { answered })
