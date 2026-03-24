import { ActionIcon, Alert, Button, ComboboxItem, FileInput, Grid, Group, Image, ScrollArea, Select, Stack, Table, Tabs, Text, TextInput, Title } from '@mantine/core'
import { IconDownload, IconPhoto, IconRefresh, IconTrash } from '@tabler/icons-react'
import { useMemo } from 'react'
import PageSection from '../components/PageSection'
import { useTemplateLibrary } from '../hooks/useTemplateLibrary'
import { buildApiUrl } from '../lib/api'
import type { ReportTemplate } from '../types'

function LibraryPage() {
  const {
    activeCategory,
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
    setActiveCategory,
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
  } = useTemplateLibrary()

  const categoryOptions = useMemo<ComboboxItem[]>(() => {
    const options = categories.map((category) => ({ value: category, label: category }))
    if (uploadCategory && !categories.some((c) => c.toLowerCase() === uploadCategory.toLowerCase())) {
      options.unshift({ value: uploadCategory, label: `+ ${uploadCategory}` })
    }
    return options
  }, [categories, uploadCategory])

  return (
    <Grid gutter="lg">
      <Grid.Col span={{ base: 12, lg: 4 }}>
        <Stack gap="lg">
          <PageSection title="템플릿 목록" description="등록된 템플릿을 검색하고 선택합니다.">
            <Stack>
              <Group>
                <TextInput
                  placeholder="검색"
                  value={searchText}
                  onChange={(event) => setSearchText(event.currentTarget.value)}
                  style={{ flex: 1 }}
                />
                <ActionIcon variant="subtle" size="lg" onClick={() => void loadTemplates()} loading={busy}>
                  <IconRefresh size={18} />
                </ActionIcon>
              </Group>
              <Tabs value={activeCategory} onChange={(value) => setActiveCategory(value ?? 'all')}>
                <Tabs.List>
                  <Tabs.Tab value="all">전체</Tabs.Tab>
                  {categories.map((category) => (
                    <Tabs.Tab key={category} value={category}>
                      {category}
                    </Tabs.Tab>
                  ))}
                </Tabs.List>
                <Tabs.Panel value="all" pt="md">
                  <TemplateList
                    templates={filteredTemplates}
                    selectedTemplate={selectedTemplate}
                    onSelect={setSelectedTemplate}
                    onDownloadName={setDownloadFormName}
                  />
                </Tabs.Panel>
                {categories.map((category) => (
                  <Tabs.Panel key={category} value={category} pt="md">
                    <TemplateList
                      templates={filteredTemplates}
                      selectedTemplate={selectedTemplate}
                      onSelect={setSelectedTemplate}
                      onDownloadName={setDownloadFormName}
                    />
                  </Tabs.Panel>
                ))}
              </Tabs>
            </Stack>
          </PageSection>

          <PageSection title="템플릿 업로드">
            <Stack>
              <TextInput label="템플릿 이름" value={uploadName} onChange={(event) => setUploadName(event.currentTarget.value)} />
              <Select
                label="카테고리"
                placeholder="기존 카테고리를 선택하거나 새 이름을 입력"
                data={categoryOptions}
                value={uploadCategory || null}
                onChange={(value) => setUploadCategory(value ?? '')}
                onSearchChange={setUploadCategory}
                searchValue={uploadCategory}
                searchable
                clearable
                nothingFoundMessage="일치하는 카테고리가 없습니다"
              />
              <FileInput label="DFM 파일" value={dfmFile} onChange={setDfmFile} accept=".dfm" />
              <FileInput label="PAS 파일" value={pasFile} onChange={setPasFile} accept=".pas" />
              <FileInput label="미리보기 이미지/PDF" value={previewFile} onChange={setPreviewFile} />
              <Button onClick={() => void createTemplate()} loading={busy} color="dark">
                업로드
              </Button>
            </Stack>
          </PageSection>
        </Stack>
      </Grid.Col>

      <Grid.Col span={{ base: 12, lg: 8 }}>
        <PageSection
          title={selectedTemplate ? selectedTemplate.name : '템플릿 상세'}
          description="미리보기를 확인하고 ZIP 파일을 다운로드할 수 있습니다."
        >
          {!selectedTemplate ? (
            <Alert color="gray" icon={<IconPhoto size={16} />}>
              목록에서 템플릿을 선택해 주세요.
            </Alert>
          ) : (
            <Stack>
              <Group justify="space-between">
                <Stack gap={0}>
                  <Title order={4}>{selectedTemplate.name}</Title>
                  <Text size="sm" c="dimmed">
                    {selectedTemplate.category} · 원본 폼명 {selectedTemplate.originalFormName}
                  </Text>
                </Stack>
                <ActionIcon variant="subtle" color="red" size="lg" onClick={() => void removeTemplate()} loading={busy}>
                  <IconTrash size={18} />
                </ActionIcon>
              </Group>
              {selectedTemplate.hasPreview ? (
                selectedTemplate.previewContentType === 'application/pdf' ? (
                  <iframe
                    title="미리보기"
                    src={`${buildApiUrl(`/api/report-templates/${selectedTemplate.id}/preview`)}#toolbar=0`}
                    style={{ width: '100%', minHeight: 900, border: 0, borderRadius: 12 }}
                  />
                ) : (
                  <Image radius="md" src={buildApiUrl(`/api/report-templates/${selectedTemplate.id}/preview`)} alt={selectedTemplate.name} />
                )
              ) : (
                <Alert color="gray">미리보기 파일이 없습니다.</Alert>
              )}
              <Group align="end">
                <TextInput
                  label="다운로드 폼 이름"
                  value={downloadFormName}
                  onChange={(event) => setDownloadFormName(event.currentTarget.value)}
                  style={{ flex: 1 }}
                />
                <Button leftSection={<IconDownload size={16} />} onClick={() => void downloadTemplate()} loading={busy} color="dark">
                  ZIP 다운로드
                </Button>
              </Group>
            </Stack>
          )}
          {message && <Alert color="gray">{message}</Alert>}
        </PageSection>
      </Grid.Col>
    </Grid>
  )
}

type TemplateListProps = {
  templates: ReportTemplate[]
  selectedTemplate: ReportTemplate | null
  onSelect: (template: ReportTemplate | null) => void
  onDownloadName: (name: string) => void
}

function TemplateList({ templates, selectedTemplate, onSelect, onDownloadName }: TemplateListProps) {
  return (
    <ScrollArea h={420}>
      <Table highlightOnHover stickyHeader>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>이름</Table.Th>
            <Table.Th>상태</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {templates.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={2}>
                <Text c="dimmed">템플릿이 없습니다.</Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            templates.map((item) => (
              <Table.Tr
                key={item.id}
                bg={selectedTemplate?.id === item.id ? '#f1f3f5' : undefined}
                style={{ cursor: 'pointer' }}
                onClick={() => {
                  onSelect(item)
                  onDownloadName(item.originalFormName)
                }}
              >
                <Table.Td>
                  <Stack gap={0}>
                    <Text fw={600}>{item.name}</Text>
                    <Text size="xs" c="dimmed">
                      {item.category}
                    </Text>
                  </Stack>
                </Table.Td>
                <Table.Td>{item.hasPreview ? '미리보기 있음' : '파일만 있음'}</Table.Td>
              </Table.Tr>
            ))
          )}
        </Table.Tbody>
      </Table>
    </ScrollArea>
  )
}

export default LibraryPage
