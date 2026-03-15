import { ActionIcon, Alert, Button, FileInput, Grid, Group, Image, ScrollArea, Select, Stack, Table, Tabs, Text, TextInput, Title } from '@mantine/core'
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

  const categoryOptions = useMemo(
    () => categories.map((category) => ({ value: category, label: category })),
    [categories],
  )

  return (
    <Grid gutter="lg">
      <Grid.Col span={{ base: 12, lg: 4 }}>
        <Stack gap="lg">
          <PageSection title="?쒗뵆由?紐⑸줉" description="??λ맂 ?쒗뵆由우쓣 寃?됲븯怨??좏깮?⑸땲??">
            <Stack>
              <Group>
                <TextInput
                  placeholder="寃??"
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
                  <Tabs.Tab value="all">?꾩껜</Tabs.Tab>
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

          <PageSection title="?쒗뵆由??낅줈??">
            <Stack>
              <TextInput label="?쒗뵆由??대쫫" value={uploadName} onChange={(event) => setUploadName(event.currentTarget.value)} />
              <Select
                label="湲곗〈 移댄뀒怨좊━ ?좏깮"
                placeholder="?좏깮?섎㈃ 湲곗〈 移댄뀒怨좊━瑜??ъ슜?⑸땲??"
                data={categoryOptions}
                value={categories.includes(uploadCategory) ? uploadCategory : null}
                onChange={(value) => setUploadCategory(value ?? '')}
                searchable
                clearable
              />
              <TextInput
                label="?덈뒗 ??移댄뀒怨좊━ ?낅젰"
                placeholder="새 카테고리가 필요하면 직접 입력"
                value={uploadCategory}
                onChange={(event) => setUploadCategory(event.currentTarget.value)}
              />
              <FileInput label="DFM ?뚯씪" value={dfmFile} onChange={setDfmFile} accept=".dfm" />
              <FileInput label="PAS ?뚯씪" value={pasFile} onChange={setPasFile} accept=".pas" />
              <FileInput label="誘몃━蹂닿린 ?대?吏/PDF" value={previewFile} onChange={setPreviewFile} />
              <Button onClick={() => void createTemplate()} loading={busy} color="dark">
                ?낅줈??
              </Button>
            </Stack>
          </PageSection>
        </Stack>
      </Grid.Col>

      <Grid.Col span={{ base: 12, lg: 8 }}>
        <PageSection
          title={selectedTemplate ? selectedTemplate.name : '?쒗뵆由??곸꽭'}
          description="誘몃━蹂닿린 ?뺤씤怨?ZIP ?ㅼ슫濡쒕뱶瑜??????덉뒿?덈떎."
        >
          {!selectedTemplate ? (
            <Alert color="gray" icon={<IconPhoto size={16} />}>
              紐⑸줉?먯꽌 ?쒗뵆由우쓣 ?좏깮?섏꽭??
            </Alert>
          ) : (
            <Stack>
              <Group justify="space-between">
                <Stack gap={0}>
                  <Title order={4}>{selectedTemplate.name}</Title>
                  <Text size="sm" c="dimmed">
                    {selectedTemplate.category} 쨌 ?먮낯 ??{selectedTemplate.originalFormName}
                  </Text>
                </Stack>
                <ActionIcon variant="subtle" color="red" size="lg" onClick={() => void removeTemplate()} loading={busy}>
                  <IconTrash size={18} />
                </ActionIcon>
              </Group>
              {selectedTemplate.hasPreview ? (
                selectedTemplate.previewContentType === 'application/pdf' ? (
                  <iframe
                    title="誘몃━蹂닿린"
                    src={`${buildApiUrl(`/api/report-templates/${selectedTemplate.id}/preview`)}#toolbar=0`}
                    style={{ width: '100%', minHeight: 900, border: 0, borderRadius: 12 }}
                  />
                ) : (
                  <Image radius="md" src={buildApiUrl(`/api/report-templates/${selectedTemplate.id}/preview`)} alt={selectedTemplate.name} />
                )
              ) : (
                <Alert color="gray">誘몃━蹂닿린 ?먯궛???놁뒿?덈떎.</Alert>
              )}
              <Group align="end">
                <TextInput
                  label="?대낫?????대쫫"
                  value={downloadFormName}
                  onChange={(event) => setDownloadFormName(event.currentTarget.value)}
                  style={{ flex: 1 }}
                />
                <Button leftSection={<IconDownload size={16} />} onClick={() => void downloadTemplate()} loading={busy} color="dark">
                  ZIP ?ㅼ슫濡쒕뱶
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
            <Table.Th>?대쫫</Table.Th>
            <Table.Th>?곹깭</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {templates.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={2}>
                <Text c="dimmed">?쒗뵆由우씠 ?놁뒿?덈떎.</Text>
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
                <Table.Td>{item.hasPreview ? '誘몃━蹂닿린 ?덉쓬' : '?뚯씪留??덉쓬'}</Table.Td>
              </Table.Tr>
            ))
          )}
        </Table.Tbody>
      </Table>
    </ScrollArea>
  )
}

export default LibraryPage
