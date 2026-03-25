import { requestBlob, requestJson } from '../../lib/api'
import type { AiVersionResponse, LayoutSpec } from '../../types'

export function fetchAiVersion(): Promise<AiVersionResponse> {
  return requestJson<AiVersionResponse>('/api/ai-version')
}

export function updateAiVersion(model: string): Promise<AiVersionResponse> {
  return requestJson<AiVersionResponse>('/api/ai-version', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ model }),
  })
}

export function generateLayoutFromImage(formName: string, file: File, presetIds: string[] = []): Promise<LayoutSpec> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  presetIds.forEach((presetId) => formData.append('presetIds', presetId))
  return requestJson<LayoutSpec>('/api/generate-json', { method: 'POST', body: formData })
}

export function exportZipFromImage(formName: string, file: File, presetIds: string[] = []): Promise<Blob> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  presetIds.forEach((presetId) => formData.append('presetIds', presetId))
  return requestBlob('/api/export-from-image', { method: 'POST', body: formData })
}

export function exportZipFromLayout(formName: string, layoutSpec: LayoutSpec): Promise<Blob> {
  return requestBlob('/api/export', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ formName, layoutSpec }),
  })
}
