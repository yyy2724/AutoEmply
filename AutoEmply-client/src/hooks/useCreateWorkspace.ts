import { notifications } from '@mantine/notifications'
import { useEffect, useState } from 'react'
import { exportZipFromImage, exportZipFromLayout, fetchAiVersion, generateLayoutFromImage, updateAiVersion } from '../features/create/api'
import { fetchPresets } from '../features/prompts/api'
import { ApiRequestError } from '../lib/api'
import { downloadBlob } from '../lib/download'
import type { LayoutSpec, PromptPreset, WorkspaceStatus } from '../types'

const defaultJson = '{\n  "items": []\n}'
const emptyStatus: WorkspaceStatus = { tone: 'info', title: '', message: '', details: [], retryable: false, retryAction: null }

export function useCreateWorkspace() {
  const [formName, setFormName] = useState('Form_QREmply25')
  const [layoutSpecJson, setLayoutSpecJson] = useState(defaultJson)
  const [status, setStatus] = useState<WorkspaceStatus>(emptyStatus)
  const [aiVersion, setAiVersion] = useState('Loading...')
  const [selectedAiModel, setSelectedAiModel] = useState('claude-opus-4-7')
  const [busy, setBusy] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [presets, setPresets] = useState<PromptPreset[]>([])

  useEffect(() => {
    fetchAiVersion()
      .then((data) => {
        setAiVersion(data?.version ?? 'Unknown')
        setSelectedAiModel(data?.configuredModel ?? data?.model ?? 'claude-opus-4-7')
      })
      .catch(() => setAiVersion('Unavailable'))

    fetchPresets()
      .then((data) => setPresets(data))
      .catch(() => setPresets([]))
  }, [])

  function setInfo(message: string) {
    setStatus({ tone: 'info', title: 'Status', message, details: [], retryable: false, retryAction: null })
  }

  function setValidationHint(message: string) {
    setStatus({ tone: 'error', title: 'Input required', message, details: [], retryable: false, retryAction: null })
  }

  function setRequestError(error: unknown, fallbackMessage: string, retryAction: WorkspaceStatus['retryAction']) {
    if (error instanceof ApiRequestError) {
      setStatus({
        tone: 'error',
        title: error.retryable ? 'Temporary error' : 'Request failed',
        message: mapApiErrorMessage(error, fallbackMessage),
        details: error.details,
        retryable: error.retryable,
        retryAction,
      })
      return
    }

    setStatus({
      tone: 'error',
      title: 'Request failed',
      message: error instanceof Error ? error.message : fallbackMessage,
      details: [],
      retryable: false,
      retryAction: null,
    })
  }

  async function generateJson() {
    if (!selectedFile) {
      setValidationHint('Upload an image or PDF before generating JSON.')
      return
    }

    try {
      setBusy(true)
      setStatus(emptyStatus)
      const data = await generateLayoutFromImage(formName, selectedFile, getGenerationPresetIds(presets))
      setLayoutSpecJson(JSON.stringify(data, null, 2))
      setInfo('LayoutSpec JSON generated.')
      notifications.show({ color: 'teal', message: 'LayoutSpec JSON generated.' })
    } catch (error) {
      setRequestError(error, 'Failed to generate LayoutSpec JSON.', 'generate-json')
    } finally {
      setBusy(false)
    }
  }

  async function exportFromImage() {
    if (!selectedFile) {
      setValidationHint('Upload an image or PDF before exporting ZIP.')
      return
    }

    try {
      setBusy(true)
      setStatus(emptyStatus)
      const blob = await exportZipFromImage(formName, selectedFile, getGenerationPresetIds(presets))
      downloadBlob(blob, `${formName}.zip`)
      setInfo('ZIP export from image started.')
      notifications.show({ color: 'teal', message: 'ZIP export from image started.' })
    } catch (error) {
      setRequestError(error, 'Failed to generate ZIP from uploaded file.', 'export-image')
    } finally {
      setBusy(false)
    }
  }

  async function exportFromJson() {
    try {
      setBusy(true)
      setStatus(emptyStatus)
      const blob = await exportZipFromLayout(formName, JSON.parse(layoutSpecJson) as LayoutSpec)
      downloadBlob(blob, `${formName}.zip`)
      setInfo('ZIP export from current JSON started.')
      notifications.show({ color: 'teal', message: 'ZIP export from current JSON started.' })
    } catch (error) {
      setRequestError(error, 'Failed to generate ZIP from current JSON.', 'export-json')
    } finally {
      setBusy(false)
    }
  }

  async function changeAiModel(model: string | null) {
    const nextModel = model ?? 'claude-opus-4-7'
    try {
      setBusy(true)
      setStatus(emptyStatus)
      const data = await updateAiVersion(nextModel)
      setSelectedAiModel(data?.configuredModel ?? data?.model ?? nextModel)
      setAiVersion(data?.version ?? nextModel)
      setInfo(`AI model changed to ${nextModel}.`)
      notifications.show({ color: 'teal', message: `AI model changed to ${nextModel}.` })
    } catch (error) {
      setRequestError(error, 'Failed to update AI model.', null)
    } finally {
      setBusy(false)
    }
  }

  return {
    aiVersion,
    busy,
    changeAiModel,
    formName,
    layoutSpecJson,
    presets,
    selectedAiModel,
    selectedFile,
    status,
    setFormName,
    setLayoutSpecJson,
    setSelectedFile,
    exportFromImage,
    exportFromJson,
    generateJson,
  }
}

function getGenerationPresetIds(presets: PromptPreset[]) {
  return orderActiveItems(presets, (item) => item.primary ?? item.isPrimary).map((item) => item.id)
}

function orderActiveItems<T extends { id: string; updatedAt: string; active?: boolean; isActive?: boolean }>(
  items: T[],
  isPrimary: (item: T) => boolean | undefined,
) {
  return items
    .filter((item) => item.active ?? item.isActive)
    .slice()
    .sort((left, right) => {
      const primaryDiff = Number(Boolean(isPrimary(right))) - Number(Boolean(isPrimary(left)))
      if (primaryDiff !== 0) {
        return primaryDiff
      }
      return Date.parse(right.updatedAt) - Date.parse(left.updatedAt)
    })
}

function mapApiErrorMessage(error: ApiRequestError, fallbackMessage: string) {
  if (error.status === 400 && error.details.length > 0) {
    return 'The server rejected the request. Fix the validation errors and try again.'
  }

  if (error.retryable) {
    return error.message || 'A temporary error occurred. Try again shortly.'
  }

  return error.message || fallbackMessage
}
