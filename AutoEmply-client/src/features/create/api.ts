import { requestBlob, requestJson } from '../../lib/api'
import type { AiVersionResponse, LayoutSpec } from '../../types'

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
  return requestJson<LayoutSpec>('/api/generate-json', { method: 'POST', body: formData })
}

export function exportZipFromImage(formName: string, file: File, presetId?: string | null): Promise<Blob> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  appendPresetId(formData, presetId)
  return requestBlob('/api/export-from-image', { method: 'POST', body: formData })
}

export function exportZipFromLayout(formName: string, layoutSpec: LayoutSpec): Promise<Blob> {
  return requestBlob('/api/export', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ formName, layoutSpec }),
  })
}
