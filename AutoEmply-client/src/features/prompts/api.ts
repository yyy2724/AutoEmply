import { requestJson } from '../../lib/api'
import type { PromptPreset, PromptPresetForm } from '../../types'

export function fetchPresets(): Promise<PromptPreset[]> {
  return requestJson<PromptPreset[]>('/api/prompts')
}

export function createPreset(payload: PromptPresetForm): Promise<PromptPreset> {
  return requestJson<PromptPreset>('/api/prompts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function updatePreset(id: string, payload: PromptPresetForm): Promise<PromptPreset> {
  return requestJson<PromptPreset>(`/api/prompts/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function deletePreset(id: string): Promise<void> {
  return requestJson<void>(`/api/prompts/${id}`, { method: 'DELETE' })
}
