import { requestBlob, requestJson } from '../../lib/api'
import type { AiVersionResponse, LayoutSpec } from '../../types'

function isPdf(file: File | null): boolean {
  if (!file) return false
  return file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
}

export function fetchAiVersion(): Promise<AiVersionResponse> {
  return requestJson<AiVersionResponse>('/api/ai-version')
}

function appendPresetId(formData: FormData, presetId?: string | null) {
  if (presetId && presetId.trim()) {
    formData.append('presetId', presetId.trim())
  }
}

export function generateLayoutFromImage(formName: string, file: File, presetId?: string | null): Promise<LayoutSpec> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  appendPresetId(formData, presetId)
  return requestJson<LayoutSpec>(isPdf(file) ? '/api/generate-json-v2' : '/api/generate-json', { method: 'POST', body: formData })
}

export function exportZipFromImage(formName: string, file: File, presetId?: string | null): Promise<Blob> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  appendPresetId(formData, presetId)
  return requestBlob(isPdf(file) ? '/api/export-from-image-v2' : '/api/export-from-image', { method: 'POST', body: formData })
}

export function exportZipFromLayout(formName: string, layoutSpec: LayoutSpec): Promise<Blob> {
  return requestBlob('/api/export', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ formName, layoutSpec }),
  })
}
