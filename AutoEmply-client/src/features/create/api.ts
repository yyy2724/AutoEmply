import { requestBlob, requestJson } from '../../lib/api'
import type { AiVersionResponse, LayoutSpec } from '../../types'

export function fetchAiVersion(): Promise<AiVersionResponse> {
  return requestJson<AiVersionResponse>('/api/ai-version')
}

export function generateLayoutFromImage(formName: string, file: File, presetIds: string[] = [], sampleTemplateSetIds: string[] = []): Promise<LayoutSpec> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  presetIds.forEach((presetId) => formData.append('presetIds', presetId))
  sampleTemplateSetIds.forEach((sampleTemplateSetId) => formData.append('sampleTemplateSetIds', sampleTemplateSetId))
  return requestJson<LayoutSpec>('/api/generate-json', { method: 'POST', body: formData })
}

export function exportZipFromImage(formName: string, file: File, presetIds: string[] = [], sampleTemplateSetIds: string[] = []): Promise<Blob> {
  const formData = new FormData()
  formData.append('formName', formName.trim())
  formData.append('image', file)
  presetIds.forEach((presetId) => formData.append('presetIds', presetId))
  sampleTemplateSetIds.forEach((sampleTemplateSetId) => formData.append('sampleTemplateSetIds', sampleTemplateSetId))
  return requestBlob('/api/export-from-image', { method: 'POST', body: formData })
}

export function exportZipFromLayout(formName: string, layoutSpec: LayoutSpec): Promise<Blob> {
  return requestBlob('/api/export', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ formName, layoutSpec }),
  })
}
