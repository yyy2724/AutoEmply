import { requestJson } from '../../lib/api'
import type { SampleTemplateSet, SampleTemplateSetForm } from '../../types'

export function fetchSampleTemplateSets(): Promise<SampleTemplateSet[]> {
  return requestJson<SampleTemplateSet[]>('/api/sample-template-sets')
}

export function createSampleTemplateSet(payload: SampleTemplateSetForm): Promise<SampleTemplateSet> {
  return requestJson<SampleTemplateSet>('/api/sample-template-sets', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function updateSampleTemplateSet(id: string, payload: SampleTemplateSetForm): Promise<SampleTemplateSet> {
  return requestJson<SampleTemplateSet>(`/api/sample-template-sets/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function deleteSampleTemplateSet(id: string): Promise<void> {
  return requestJson<void>(`/api/sample-template-sets/${id}`, { method: 'DELETE' })
}
