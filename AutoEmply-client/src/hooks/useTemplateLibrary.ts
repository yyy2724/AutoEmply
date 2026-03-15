import { notifications } from '@mantine/notifications'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { deleteReportTemplate, downloadReportTemplate, fetchReportTemplates, uploadReportTemplate } from '../features/library/api'
import { downloadBlob } from '../lib/download'
import type { ReportTemplate } from '../types'

export function useTemplateLibrary() {
  const [templates, setTemplates] = useState<ReportTemplate[]>([])
  const [selectedTemplate, setSelectedTemplate] = useState<ReportTemplate | null>(null)
  const [searchText, setSearchText] = useState('')
  const [downloadFormName, setDownloadFormName] = useState('')
  const [uploadName, setUploadName] = useState('')
  const [uploadCategory, setUploadCategory] = useState('')
  const [dfmFile, setDfmFile] = useState<File | null>(null)
  const [pasFile, setPasFile] = useState<File | null>(null)
  const [previewFile, setPreviewFile] = useState<File | null>(null)
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const filteredTemplates = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) return templates
    return templates.filter((item) =>
      [item.name, item.category, item.originalFormName].some((value) =>
        value.toLowerCase().includes(keyword),
      ),
    )
  }, [searchText, templates])

  const categories = useMemo(
    () => [...new Set(filteredTemplates.map((item) => item.category))],
    [filteredTemplates],
  )

  const loadTemplates = useCallback(async () => {
    try {
      setBusy(true)
      setMessage('')
      const data = await fetchReportTemplates()
      setTemplates(data)
      if (selectedTemplate) {
        setSelectedTemplate(data.find((item) => item.id === selectedTemplate.id) ?? null)
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '템플릿을 불러오지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }, [selectedTemplate])

  useEffect(() => {
    void loadTemplates()
  }, [loadTemplates])

  async function createTemplate() {
    if (!uploadName.trim() || !uploadCategory.trim() || !dfmFile || !pasFile) {
      setMessage('템플릿 이름, 카테고리, DFM 파일, PAS 파일은 모두 필요합니다.')
      return
    }

    try {
      setBusy(true)
      setMessage('')
      await uploadReportTemplate({
        name: uploadName,
        category: uploadCategory,
        dfmFile,
        pasFile,
        previewFile,
      })
      notifications.show({ color: 'teal', message: '템플릿 업로드가 완료되었습니다.' })
      setUploadName('')
      setUploadCategory('')
      setDfmFile(null)
      setPasFile(null)
      setPreviewFile(null)
      await loadTemplates()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '템플릿 업로드에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  async function downloadTemplate() {
    if (!selectedTemplate || !downloadFormName.trim()) {
      return
    }

    try {
      setBusy(true)
      setMessage('')
      const blob = await downloadReportTemplate(selectedTemplate.id, downloadFormName)
      downloadBlob(blob, `${downloadFormName.trim()}.zip`)
      notifications.show({ color: 'teal', message: 'ZIP 다운로드를 시작했습니다.' })
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'ZIP 다운로드에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  async function removeTemplate() {
    if (!selectedTemplate) {
      return
    }

    try {
      setBusy(true)
      setMessage('')
      await deleteReportTemplate(selectedTemplate.id)
      setSelectedTemplate(null)
      notifications.show({ color: 'teal', message: '템플릿이 삭제되었습니다.' })
      await loadTemplates()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '템플릿 삭제에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return {
    busy,
    categories,
    dfmFile,
    downloadFormName,
    filteredTemplates,
    message,
    pasFile,
    previewFile,
    searchText,
    selectedTemplate,
    uploadCategory,
    uploadName,
    setDfmFile,
    setDownloadFormName,
    setPasFile,
    setPreviewFile,
    setSearchText,
    setSelectedTemplate,
    setUploadCategory,
    setUploadName,
    createTemplate,
    downloadTemplate,
    loadTemplates,
    removeTemplate,
  }
}
