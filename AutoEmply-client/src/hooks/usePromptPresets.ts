import { notifications } from '@mantine/notifications'
import { useEffect, useMemo, useState } from 'react'
import { fetchReportTemplates } from '../features/library/api'
import { createPreset, deletePreset, fetchPresets, updatePreset } from '../features/prompts/api'
import {
  createSampleTemplateSet,
  deleteSampleTemplateSet,
  fetchSampleTemplateSets,
  updateSampleTemplateSet,
} from '../features/sample-template-sets/api'
import type {
  PromptPreset,
  PromptPresetForm,
  ReportTemplate,
  SampleTemplateSet,
  SampleTemplateSetForm,
} from '../types'

const createDefaultPresetForm = (): PromptPresetForm => ({
  name: `preset-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`,
  systemPrompt: '',
  userPromptTemplate: '',
  styleRulesJson: '',
  sampleTemplateIds: [],
  model: '',
  temperature: 0,
  maxTokens: 32000,
  isActive: true,
})

const createDefaultSampleSetForm = (): SampleTemplateSetForm => ({
  name: `sample-set-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`,
  templateIds: [],
})

function createPresetFormFromSource(source?: PromptPreset | null): PromptPresetForm {
  return {
    name: `preset-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`,
    systemPrompt: '',
    userPromptTemplate: source?.userPromptTemplate ?? '',
    styleRulesJson: '',
    sampleTemplateIds: [],
    model: '',
    temperature: 0,
    maxTokens: 32000,
    isActive: true,
  }
}

export function usePromptPresets() {
  const [presets, setPresets] = useState<PromptPreset[]>([])
  const [sampleSets, setSampleSets] = useState<SampleTemplateSet[]>([])
  const [reportTemplates, setReportTemplates] = useState<ReportTemplate[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [sampleSetEditingId, setSampleSetEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<PromptPresetForm>(createDefaultPresetForm())
  const [sampleSetForm, setSampleSetForm] = useState<SampleTemplateSetForm>(createDefaultSampleSetForm())
  const [message, setMessage] = useState('')
  const [sampleSetMessage, setSampleSetMessage] = useState('')
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

  async function loadSampleSets() {
    try {
      setBusy(true)
      setSampleSetMessage('')
      const data = await fetchSampleTemplateSets()
      setSampleSets(data)
    } catch (error) {
      setSampleSetMessage(error instanceof Error ? error.message : '결과지 프리셋을 불러오지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  async function loadReportTemplates() {
    try {
      const data = await fetchReportTemplates()
      setReportTemplates(data)
    } catch {
      setReportTemplates([])
    }
  }

  async function refreshAll() {
    await Promise.all([loadPresets(), loadSampleSets(), loadReportTemplates()])
  }

  useEffect(() => {
    void refreshAll()
  }, [])

  function selectPreset(preset: PromptPreset) {
    setEditingId(preset.id)
    setForm({
      name: preset.name,
      systemPrompt: preset.systemPrompt,
      userPromptTemplate: preset.userPromptTemplate,
      styleRulesJson: preset.styleRulesJson ?? '',
      sampleTemplateIds: preset.sampleTemplateIds ?? [],
      model: preset.model ?? '',
      temperature: preset.temperature ?? 0,
      maxTokens: preset.maxTokens ?? 32000,
      isActive: preset.active ?? preset.isActive ?? false,
    })
  }

  function selectSampleSet(sampleSet: SampleTemplateSet) {
    setSampleSetEditingId(sampleSet.id)
    setSampleSetForm({
      name: sampleSet.name,
      templateIds: sampleSet.templateIds,
    })
  }

  function resetEditor() {
    setEditingId(null)
    const basePreset = presets.find((preset) => preset.active ?? preset.isActive) ?? presets[0] ?? null
    setForm(basePreset ? createPresetFormFromSource(basePreset) : createDefaultPresetForm())
  }

  function resetSampleSetEditor() {
    setSampleSetEditingId(null)
    setSampleSetForm(createDefaultSampleSetForm())
  }

  async function savePreset() {
    try {
      setBusy(true)
      setMessage('')
      const payload: PromptPresetForm = {
        ...form,
        styleRulesJson: form.styleRulesJson.trim() || '',
        sampleTemplateIds: [],
        model: form.model.trim() || '',
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

  async function saveSampleSet() {
    try {
      setBusy(true)
      setSampleSetMessage('')
      const payload: SampleTemplateSetForm = {
        name: sampleSetForm.name.trim(),
        templateIds: sampleSetForm.templateIds,
      }

      if (sampleSetEditingId) {
        await updateSampleTemplateSet(sampleSetEditingId, payload)
      } else {
        await createSampleTemplateSet(payload)
      }

      notifications.show({ color: 'teal', message: '결과지 프리셋이 저장되었습니다.' })
      resetSampleSetEditor()
      await loadSampleSets()
    } catch (error) {
      setSampleSetMessage(error instanceof Error ? error.message : '결과지 프리셋 저장에 실패했습니다.')
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

  async function removeSampleSet() {
    if (!sampleSetEditingId) {
      return
    }

    try {
      setBusy(true)
      setSampleSetMessage('')
      await deleteSampleTemplateSet(sampleSetEditingId)
      notifications.show({ color: 'teal', message: '결과지 프리셋이 삭제되었습니다.' })
      resetSampleSetEditor()
      await Promise.all([loadSampleSets(), loadPresets()])
    } catch (error) {
      setSampleSetMessage(error instanceof Error ? error.message : '결과지 프리셋 삭제에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  const activeCount = useMemo(
    () => presets.filter((preset) => preset.active ?? preset.isActive).length,
    [presets],
  )

  return {
    activeCount,
    busy,
    editingId,
    form,
    message,
    presets,
    reportTemplates,
    resetEditor,
    savePreset,
    selectPreset,
    setForm,
    loadPresets,
    removePreset,
    sampleSets,
    sampleSetEditingId,
    sampleSetForm,
    sampleSetMessage,
    selectSampleSet,
    setSampleSetForm,
    saveSampleSet,
    removeSampleSet,
    resetSampleSetEditor,
    loadSampleSets,
  }
}
