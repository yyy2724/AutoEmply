import { notifications } from '@mantine/notifications'
import { useEffect, useState } from 'react'
import { exportZipFromImage, exportZipFromLayout, fetchAiVersion, generateLayoutFromImage, updateAiVersion } from '../features/create/api'
import { fetchPresets } from '../features/prompts/api'
import { ApiRequestError } from '../lib/api'
import { downloadBlob } from '../lib/download'
import type { LayoutSpec, PromptPreset, WorkspaceStatus } from '../types'
import { useAsyncAction } from './useAsyncAction'

const DEFAULT_AI_MODEL = 'claude-opus-4-7'
const defaultJson = '{\n  "items": []\n}'
const emptyStatus: WorkspaceStatus = { tone: 'info', title: '', message: '', details: [], retryable: false, retryAction: null }

export function useCreateWorkspace() {
  const [formName, setFormName] = useState('Form_QREmply25')
  const [layoutSpecJson, setLayoutSpecJson] = useState(defaultJson)
  const [status, setStatus] = useState<WorkspaceStatus>(emptyStatus)
  const [aiVersion, setAiVersion] = useState('불러오는 중...')
  const [selectedAiModel, setSelectedAiModel] = useState(DEFAULT_AI_MODEL)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [presets, setPresets] = useState<PromptPreset[]>([])
  const { busy, run } = useAsyncAction()

  useEffect(() => {
    fetchAiVersion()
      .then((data) => {
        setAiVersion(data.version ?? '알 수 없음')
        setSelectedAiModel(data.configuredModel ?? data.model ?? DEFAULT_AI_MODEL)
      })
      .catch(() => setAiVersion('확인 불가'))

    fetchPresets()
      .then((data) => setPresets(data))
      .catch(() => setPresets([]))
  }, [])

  function setInfo(message: string) {
    setStatus({ tone: 'info', title: '상태', message, details: [], retryable: false, retryAction: null })
  }

  function setValidationHint(message: string) {
    setStatus({ tone: 'error', title: '입력 필요', message, details: [], retryable: false, retryAction: null })
  }

  function setRequestError(error: unknown, fallbackMessage: string, retryAction: WorkspaceStatus['retryAction']) {
    if (error instanceof ApiRequestError) {
      setStatus({
        tone: 'error',
        title: error.retryable ? '일시적 오류' : '요청 실패',
        message: mapApiErrorMessage(error, fallbackMessage),
        details: error.details,
        retryable: error.retryable,
        retryAction,
      })
      return
    }

    setStatus({
      tone: 'error',
      title: '요청 실패',
      message: error instanceof Error ? error.message : fallbackMessage,
      details: [],
      retryable: false,
      retryAction: null,
    })
  }

  async function generateJson() {
    if (!selectedFile) {
      setValidationHint('JSON을 생성하려면 이미지 또는 PDF 파일을 먼저 업로드하세요.')
      return
    }

    const file = selectedFile
    setStatus(emptyStatus)
    await run(async () => {
      const data = await generateLayoutFromImage(formName, file, getGenerationPresetIds(presets))
      setLayoutSpecJson(JSON.stringify(data, null, 2))
      setInfo('LayoutSpec JSON이 생성되었습니다.')
      notifications.show({ color: 'teal', message: 'LayoutSpec JSON이 생성되었습니다.' })
    }, (error) => setRequestError(error, 'LayoutSpec JSON 생성에 실패했습니다.', 'generate-json'))
  }

  async function exportFromImage() {
    if (!selectedFile) {
      setValidationHint('ZIP을 생성하려면 이미지 또는 PDF 파일을 먼저 업로드하세요.')
      return
    }

    const file = selectedFile
    setStatus(emptyStatus)
    await run(async () => {
      const blob = await exportZipFromImage(formName, file, getGenerationPresetIds(presets))
      downloadBlob(blob, `${formName}.zip`)
      setInfo('이미지 기반 ZIP 내보내기를 시작했습니다.')
      notifications.show({ color: 'teal', message: '이미지 기반 ZIP 내보내기를 시작했습니다.' })
    }, (error) => setRequestError(error, '업로드한 파일로 ZIP을 생성하지 못했습니다.', 'export-image'))
  }

  async function exportFromJson() {
    setStatus(emptyStatus)
    await run(async () => {
      const blob = await exportZipFromLayout(formName, JSON.parse(layoutSpecJson) as LayoutSpec)
      downloadBlob(blob, `${formName}.zip`)
      setInfo('현재 JSON 기반 ZIP 내보내기를 시작했습니다.')
      notifications.show({ color: 'teal', message: '현재 JSON 기반 ZIP 내보내기를 시작했습니다.' })
    }, (error) => setRequestError(error, '현재 JSON으로 ZIP을 생성하지 못했습니다.', 'export-json'))
  }

  async function changeAiModel(model: string | null) {
    const nextModel = model ?? DEFAULT_AI_MODEL
    setStatus(emptyStatus)
    await run(async () => {
      const data = await updateAiVersion(nextModel)
      setSelectedAiModel(data.configuredModel ?? data.model ?? nextModel)
      setAiVersion(data.version ?? nextModel)
      setInfo(`AI 모델이 ${nextModel}(으)로 변경되었습니다.`)
      notifications.show({ color: 'teal', message: `AI 모델이 ${nextModel}(으)로 변경되었습니다.` })
    }, (error) => setRequestError(error, 'AI 모델 변경에 실패했습니다.', null))
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

/** Active preset ids, primary preset first, then most recently updated. */
function getGenerationPresetIds(presets: PromptPreset[]) {
  return presets
    .filter((preset) => preset.isActive)
    .sort((left, right) => {
      const primaryDiff = Number(right.isPrimary) - Number(left.isPrimary)
      if (primaryDiff !== 0) {
        return primaryDiff
      }
      return Date.parse(right.updatedAt) - Date.parse(left.updatedAt)
    })
    .map((preset) => preset.id)
}

function mapApiErrorMessage(error: ApiRequestError, fallbackMessage: string) {
  if (error.status === 400 && error.details.length > 0) {
    return '서버가 요청을 거부했습니다. 입력 값을 확인한 뒤 다시 시도하세요.'
  }

  if (error.retryable) {
    return error.message || '일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.'
  }

  return error.message || fallbackMessage
}
