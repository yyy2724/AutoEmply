import { notifications } from '@mantine/notifications'
import { useEffect, useMemo, useState } from 'react'
import { createPreset, deletePreset, fetchPresets, updatePreset } from '../features/prompts/api'
import type { PromptPreset, PromptPresetForm } from '../types'

const createDefaultPresetForm = (): PromptPresetForm => ({
  name: `preset-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`,
  systemPrompt: '',
  userPromptTemplate: '',
  styleRulesJson: '',
  model: 'claude-opus-4-7',
  temperature: 0,
  maxTokens: 32000,
  isActive: true,
  isPrimary: false,
})

function createPresetFormFromSource(source?: PromptPreset | null): PromptPresetForm {
  return {
    name: `preset-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`,
    systemPrompt: '',
    userPromptTemplate: source?.userPromptTemplate ?? '',
    styleRulesJson: '',
    model: source?.model ?? 'claude-opus-4-7',
    temperature: 0,
    maxTokens: 32000,
    isActive: true,
    isPrimary: false,
  }
}

export function usePromptPresets() {
  const [presets, setPresets] = useState<PromptPreset[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<PromptPresetForm>(createDefaultPresetForm())
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  async function loadPresets() {
    try {
      setBusy(true)
      setMessage('')
      const data = await fetchPresets()
      setPresets(data)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '프리셋을 불러오지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    void loadPresets()
  }, [])

  function selectPreset(preset: PromptPreset) {
    setEditingId(preset.id)
    setForm({
      name: preset.name,
      systemPrompt: preset.systemPrompt,
      userPromptTemplate: preset.userPromptTemplate,
      styleRulesJson: preset.styleRulesJson ?? '',
      model: preset.model ?? '',
      temperature: preset.temperature ?? 0,
      maxTokens: preset.maxTokens ?? 32000,
      isActive: preset.isActive,
      isPrimary: preset.isPrimary,
    })
  }

  function resetEditor() {
    setEditingId(null)
    const basePreset = presets.find((preset) => preset.isActive) ?? presets[0] ?? null
    setForm(basePreset ? createPresetFormFromSource(basePreset) : createDefaultPresetForm())
  }

  async function savePreset() {
    try {
      setBusy(true)
      setMessage('')
      const payload: PromptPresetForm = {
        ...form,
        styleRulesJson: form.styleRulesJson.trim() || '',
        model: form.model.trim() || '',
        isPrimary: form.isPrimary,
      }

      if (editingId) {
        await updatePreset(editingId, payload)
      } else {
        await createPreset(payload)
      }

      notifications.show({ color: 'teal', message: '프리셋이 저장되었습니다.' })
      resetEditor()
      await loadPresets()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '프리셋 저장에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  async function removePreset() {
    if (!editingId) {
      return
    }

    try {
      setBusy(true)
      setMessage('')
      await deletePreset(editingId)
      notifications.show({ color: 'teal', message: '프리셋이 삭제되었습니다.' })
      resetEditor()
      await loadPresets()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '프리셋 삭제에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  const activeCount = useMemo(
    () => presets.filter((preset) => preset.isActive).length,
    [presets],
  )

  return {
    activeCount,
    busy,
    editingId,
    form,
    message,
    presets,
    resetEditor,
    savePreset,
    selectPreset,
    setForm,
    loadPresets,
    removePreset,
  }
}
