import { notifications } from '@mantine/notifications'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { deleteReportTemplate, downloadReportTemplate, fetchReportTemplates, uploadReportTemplate } from '../features/library/api'
import { downloadBlob } from '../lib/download'
import type { ReportTemplate } from '../types'
import { useAsyncAction } from './useAsyncAction'

export function useTemplateLibrary() {
  const [templates, setTemplates] = useState<ReportTemplate[]>([])
  const [selectedTemplate, setSelectedTemplate] = useState<ReportTemplate | null>(null)
  const [activeCategory, setActiveCategory] = useState('all')
  const [searchText, setSearchText] = useState('')
  const [downloadFormName, setDownloadFormName] = useState('')
  const [uploadName, setUploadName] = useState('')
  const [uploadCategory, setUploadCategory] = useState('')
  const [dfmFile, setDfmFile] = useState<File | null>(null)
  const [pasFile, setPasFile] = useState<File | null>(null)
  const [previewFile, setPreviewFile] = useState<File | null>(null)
  const { busy, message, run, setMessage } = useAsyncAction()

  const categories = useMemo(
    () => [...new Set(templates.map((item) => item.category))],
    [templates],
  )

  // Fall back to 'all' at render time when the selected category no longer
  // exists (e.g. its last template was deleted) — derived instead of an
  // effect to avoid a cascading re-render.
  const effectiveCategory =
    activeCategory === 'all' || categories.includes(activeCategory) ? activeCategory : 'all'

  const filteredTemplates = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    const categoryFiltered = effectiveCategory === 'all'
      ? templates
      : templates.filter((item) => item.category === effectiveCategory)

    if (!keyword) return categoryFiltered
    return categoryFiltered.filter((item) =>
      [item.name, item.category, item.originalFormName].some((value) =>
        value.toLowerCase().includes(keyword),
      ),
    )
  }, [effectiveCategory, searchText, templates])

  const loadTemplates = useCallback(async () => {
    await run(async () => {
      const data = await fetchReportTemplates()
      setTemplates(data)
      setSelectedTemplate((current) =>
        current ? data.find((item) => item.id === current.id) ?? null : current,
      )
    }, '템플릿을 불러오지 못했습니다.')
  }, [run])

  useEffect(() => {
    void loadTemplates()
  }, [loadTemplates])

  async function createTemplate() {
    if (!uploadName.trim() || !uploadCategory.trim() || !dfmFile || !pasFile) {
      setMessage('템플릿 이름, 카테고리, DFM 파일, PAS 파일은 모두 필요합니다.')
      return
    }

    const request = { name: uploadName, category: uploadCategory, dfmFile, pasFile, previewFile }
    await run(async () => {
      await uploadReportTemplate(request)
      notifications.show({ color: 'teal', message: '템플릿 업로드가 완료되었습니다.' })
      setUploadName('')
      setUploadCategory('')
      setDfmFile(null)
      setPasFile(null)
      setPreviewFile(null)
      await loadTemplates()
    }, '템플릿 업로드에 실패했습니다.')
  }

  async function downloadTemplate() {
    if (!selectedTemplate || !downloadFormName.trim()) {
      return
    }

    const templateId = selectedTemplate.id
    await run(async () => {
      const blob = await downloadReportTemplate(templateId, downloadFormName)
      downloadBlob(blob, `${downloadFormName.trim()}.zip`)
      notifications.show({ color: 'teal', message: 'ZIP 다운로드를 시작했습니다.' })
    }, 'ZIP 다운로드에 실패했습니다.')
  }

  async function removeTemplate() {
    if (!selectedTemplate) {
      return
    }

    const templateId = selectedTemplate.id
    await run(async () => {
      await deleteReportTemplate(templateId)
      setSelectedTemplate(null)
      notifications.show({ color: 'teal', message: '템플릿이 삭제되었습니다.' })
      await loadTemplates()
    }, '템플릿 삭제에 실패했습니다.')
  }

  return {
    activeCategory: effectiveCategory,
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
    setActiveCategory,
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
