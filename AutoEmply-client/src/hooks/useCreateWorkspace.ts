import { notifications } from '@mantine/notifications'
import { useEffect, useState } from 'react'
import { exportZipFromImage, exportZipFromLayout, fetchAiVersion, generateLayoutFromImage } from '../features/create/api'
import { fetchPresets } from '../features/prompts/api'
import { fetchSampleTemplateSets } from '../features/sample-template-sets/api'
import { ApiRequestError } from '../lib/api'
import { downloadBlob } from '../lib/download'
import type { LayoutSpec, PromptPreset, SampleTemplateSet, WorkspaceStatus } from '../types'

const defaultJson = '{\n  "items": []\n}'
const emptyStatus: WorkspaceStatus = { tone: 'info', title: '', message: '', details: [], retryable: false, retryAction: null }

export function useCreateWorkspace() {
  const [formName, setFormName] = useState('Form_QREmply25')
  const [layoutSpecJson, setLayoutSpecJson] = useState(defaultJson)
  const [status, setStatus] = useState<WorkspaceStatus>(emptyStatus)
  const [aiVersion, setAiVersion] = useState('불러오는 중..')
  const [busy, setBusy] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [presets, setPresets] = useState<PromptPreset[]>([])
  const [sampleSets, setSampleSets] = useState<SampleTemplateSet[]>([])

  useEffect(() => {
    fetchAiVersion()
      .then((data) => setAiVersion(data?.version ?? '알 수 없음'))
      .catch(() => setAiVersion('확인 불가'))

    fetchPresets()
      .then((data) => setPresets(data))
      .catch(() => setPresets([]))

    fetchSampleTemplateSets()
      .then((data) => setSampleSets(data))
      .catch(() => setSampleSets([]))
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
      setValidationHint('JSON 생성 전에 이미지나 PDF를 업로드하세요.')
      return
    }

    try {
      setBusy(true)
      setStatus(emptyStatus)
      const data = await generateLayoutFromImage(
        formName,
        selectedFile,
        getGenerationPresetIds(presets),
        getGenerationSampleSetIds(sampleSets),
      )
      setLayoutSpecJson(JSON.stringify(data, null, 2))
      setInfo('LayoutSpec JSON 생성이 완료되었습니다.')
      notifications.show({ color: 'teal', message: 'LayoutSpec JSON 생성이 완료되었습니다.' })
    } catch (error) {
      setRequestError(error, 'LayoutSpec JSON 생성에 실패했습니다.', 'generate-json')
    } finally {
      setBusy(false)
    }
  }

  async function exportFromImage() {
    if (!selectedFile) {
      setValidationHint('ZIP 내보내기 전에 이미지나 PDF를 업로드하세요.')
      return
    }

    try {
      setBusy(true)
      setStatus(emptyStatus)
      const blob = await exportZipFromImage(
        formName,
        selectedFile,
        getGenerationPresetIds(presets),
        getGenerationSampleSetIds(sampleSets),
      )
      downloadBlob(blob, `${formName}.zip`)
      setInfo('이미지 기반 ZIP 내보내기를 시작했습니다.')
      notifications.show({ color: 'teal', message: '이미지 기반 ZIP 내보내기를 시작했습니다.' })
    } catch (error) {
      setRequestError(error, '업로드한 파일에서 ZIP 생성에 실패했습니다.', 'export-image')
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
      setInfo('JSON 기반 ZIP 내보내기를 시작했습니다.')
      notifications.show({ color: 'teal', message: 'JSON 기반 ZIP 내보내기를 시작했습니다.' })
    } catch (error) {
      setRequestError(error, '현재 JSON에서 ZIP 생성에 실패했습니다.', 'export-json')
    } finally {
      setBusy(false)
    }
  }

  return {
    aiVersion,
    busy,
    formName,
    layoutSpecJson,
    status,
    selectedFile,
    presets,
    sampleSets,
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

function getGenerationSampleSetIds(sampleSets: SampleTemplateSet[]) {
  return orderActiveItems(sampleSets, (item) => item.primary ?? item.isPrimary).map((item) => item.id)
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
    return '서버가 요청을 거부했습니다. 아래 검증 오류를 수정한 뒤 다시 시도하세요.'
  }

  if (error.retryable) {
    return error.message || '일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.'
  }

  return error.message || fallbackMessage
}
