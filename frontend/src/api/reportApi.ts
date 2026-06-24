import { http } from './http'
import type { BaseResponse } from '@/types/analysis'
import type { CreateReportRequest, ReportDocument } from '@/types/report'

export async function createReport(request: CreateReportRequest): Promise<BaseResponse<ReportDocument>> {
  const response = await http.post<BaseResponse<ReportDocument>>('/reports', request, { timeout: 150_000 })
  return response.data
}

export async function downloadReportPdf(reportId: string): Promise<{ blob: Blob; filename?: string }> {
  const response = await http.get<Blob>(`/reports/${reportId}/pdf`, { responseType: 'blob', timeout: 60_000 })
  const disposition = response.headers['content-disposition'] as string | undefined
  const filename = disposition?.match(/filename="?([^";]+)"?/)?.[1]
  return { blob: response.data, filename }
}
