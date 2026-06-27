import { notifications } from '@mantine/notifications'
import { useEffect, useMemo, useState } from 'react'
import { createPreset, deletePreset, fetchPresets, updatePreset } from '../features/prompts/api'
import type { PromptPreset, PromptPresetForm } from '../types'
import { useAsyncAction } from './useAsyncAction'

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
    ...createDefaultPresetForm(),
    userPromptTemplate: source?.userPromptTemplate ?? '',
    model: source?.model ?? 'claude-opus-4-7',
  }
}

export function usePromptPresets() {
  const [presets, setPresets] = useState<PromptPreset[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<PromptPresetForm>(createDefaultPresetForm())
  const { busy, message, run } = useAsyncAction()

  async function loadPresets() {
    await run(async () => {
      setPresets(await fetchPresets())
    }, '프리셋을 불러오지 못했습니다.')
  }

  useEffect(() => {
    void loadPresets()
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    await run(async () => {
      const payload: PromptPresetForm = {
        ...form,
        styleRulesJson: form.styleRulesJson.trim(),
        model: form.model.trim(),
      }

      if (editingId) {
        await updatePreset(editingId, payload)
      } else {
        await createPreset(payload)
      }

      notifications.show({ color: 'teal', message: '프리셋이 저장되었습니다.' })
      resetEditor()
      await loadPresets()
    }, '프리셋 저장에 실패했습니다.')
  }

  async function removePreset() {
    if (!editingId) {
      return
    }

    await run(async () => {
      await deletePreset(editingId)
      notifications.show({ color: 'teal', message: '프리셋이 삭제되었습니다.' })
      resetEditor()
      await loadPresets()
    }, '프리셋 삭제에 실패했습니다.')
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
