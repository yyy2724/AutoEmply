import { requestBlob, requestJson } from '../../lib/api'
import type { ReportTemplate, UploadTemplateRequest } from '../../types'

export function fetchReportTemplates(): Promise<ReportTemplate[]> {
  return requestJson<ReportTemplate[]>('/api/report-templates')
}

export function uploadReportTemplate({ name, category, dfmFile, pasFile, previewFile }: UploadTemplateRequest): Promise<ReportTemplate> {
  const formData = new FormData()
  formData.append('name', name.trim())
  formData.append('category', category.trim())
  formData.append('dfmFile', dfmFile)
  formData.append('pasFile', pasFile)
  if (previewFile) {
    formData.append('previewFile', previewFile)
  }
  return requestJson<ReportTemplate>('/api/report-templates', { method: 'POST', body: formData })
}

export function deleteReportTemplate(id: string): Promise<void> {
  return requestJson<void>(`/api/report-templates/${id}`, { method: 'DELETE' })
}

export function downloadReportTemplate(id: string, formName: string): Promise<Blob> {
  return requestBlob(`/api/report-templates/${id}/download?formName=${encodeURIComponent(formName.trim())}`)
}
