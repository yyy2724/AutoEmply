import {
  Alert,
  Badge,
  Button,
  Checkbox,
  Grid,
  Group,
  MultiSelect,
  NumberInput,
  ScrollArea,
  Stack,
  Table,
  Tabs,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core'
import PageSection from '../components/PageSection'
import { usePromptPresets } from '../hooks/usePromptPresets'
import type { PromptPreset, SampleTemplateSet } from '../types'

const mutedButtonStyles = {
  root: {
    border: '1px solid #2f353e',
    background: '#151a21',
    color: '#d6dbe3',
  },
}

const monoLabelStyles = {
  label: {
    fontFamily: 'JetBrains Mono, D2Coding, Consolas, monospace',
    fontSize: '0.74rem',
    letterSpacing: '0.08em',
    textTransform: 'uppercase' as const,
    color: '#87909d',
  },
  input: {
    background: '#0f1318',
    borderColor: '#2b313a',
    color: '#d6dbe3',
    fontFamily: 'JetBrains Mono, D2Coding, Consolas, monospace',
  },
}

const alertStyles = {
  root: {
    background: '#141920',
    borderColor: '#2b313a',
    color: '#d6dbe3',
  },
}

function PresetsPage() {
  const {
    activeCount,
    busy,
    editingId,
    form,
    loadPresets,
    loadSampleSets,
    message,
    presets,
    removePreset,
    removeSampleSet,
    reportTemplates,
    resetEditor,
    resetSampleSetEditor,
    sampleSetEditingId,
    sampleSetForm,
    sampleSetMessage,
    sampleSets,
    savePreset,
    saveSampleSet,
    selectPreset,
    selectSampleSet,
    setForm,
    setSampleSetForm,
  } = usePromptPresets()

  const reportTemplateOptions = reportTemplates.map((template) => ({
    value: template.id,
    label: `${template.name} [${template.category}]`,
  }))

  return (
    <Tabs defaultValue="language-presets">
      <Tabs.List>
        <Tabs.Tab value="language-presets">언어 프리셋</Tabs.Tab>
        <Tabs.Tab value="report-presets">결과지 프리셋</Tabs.Tab>
      </Tabs.List>

      <Tabs.Panel value="language-presets" pt="md">
        <Grid gutter="lg">
          <Grid.Col span={{ base: 12, lg: 5 }}>
            <PageSection title="프리셋 목록" description={`전체 ${presets.length}개 / 활성 ${activeCount}개`}>
              <Stack>
                <Group justify="space-between">
                  <Text size="sm" c="dimmed" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                    프리셋 선택
                  </Text>
                  <Button variant="default" styles={mutedButtonStyles} onClick={() => void loadPresets()} loading={busy}>
                    새로고침
                  </Button>
                </Group>
                <ScrollArea h={520}>
                  <Table stickyHeader highlightOnHover>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>이름</Table.Th>
                        <Table.Th>상태</Table.Th>
                        <Table.Th>수정일</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {presets.map((preset: PromptPreset) => (
                        <Table.Tr
                          key={preset.id}
                          onClick={() => selectPreset(preset)}
                          style={{
                            cursor: 'pointer',
                            background: editingId === preset.id ? '#171d24' : undefined,
                          }}
                        >
                          <Table.Td>{preset.name}</Table.Td>
                          <Table.Td>{preset.active ?? preset.isActive ? '활성' : '비활성'}</Table.Td>
                          <Table.Td>{new Date(preset.updatedAt).toLocaleString()}</Table.Td>
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                </ScrollArea>
              </Stack>
            </PageSection>
          </Grid.Col>

          <Grid.Col span={{ base: 12, lg: 7 }}>
            <PageSection title={editingId ? '프리셋 수정' : '프리셋 생성'}>
              <Stack>
                <Group justify="space-between">
                  <Badge color="gray" variant="light" radius="sm">
                    {editingId ? '수정 모드' : '생성 모드'}
                  </Badge>
                  <Text size="sm" c="#7f8794" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                    {editingId ? editingId.slice(0, 8) : '신규'}
                  </Text>
                </Group>
                <Group grow>
                  <TextInput
                    label="프리셋 이름"
                    styles={monoLabelStyles}
                    value={form.name}
                    onChange={(event) => setForm((current) => ({ ...current, name: event.currentTarget.value }))}
                  />
                  <TextInput
                    label="모델"
                    styles={monoLabelStyles}
                    value={form.model}
                    onChange={(event) => setForm((current) => ({ ...current, model: event.currentTarget.value }))}
                  />
                </Group>
                <Textarea
                  label="시스템 프롬프트"
                  styles={monoLabelStyles}
                  minRows={8}
                  autosize
                  value={form.systemPrompt}
                  onChange={(event) => setForm((current) => ({ ...current, systemPrompt: event.currentTarget.value }))}
                />
                <Textarea
                  label="사용자 프롬프트 템플릿"
                  styles={monoLabelStyles}
                  minRows={4}
                  autosize
                  value={form.userPromptTemplate}
                  onChange={(event) => setForm((current) => ({ ...current, userPromptTemplate: event.currentTarget.value }))}
                />
                <Textarea
                  label="스타일 규칙 JSON"
                  styles={monoLabelStyles}
                  minRows={4}
                  autosize
                  value={form.styleRulesJson}
                  onChange={(event) => setForm((current) => ({ ...current, styleRulesJson: event.currentTarget.value }))}
                />
                <Group grow align="end">
                  <NumberInput
                    label="온도"
                    styles={monoLabelStyles}
                    value={form.temperature}
                    onChange={(value) => setForm((current) => ({ ...current, temperature: Number(value ?? 0) }))}
                  />
                  <NumberInput
                    label="최대 토큰"
                    styles={monoLabelStyles}
                    value={form.maxTokens}
                    onChange={(value) => setForm((current) => ({ ...current, maxTokens: Number(value ?? 32000) }))}
                  />
                </Group>
                <Checkbox
                  label="활성 프리셋으로 사용"
                  styles={{ label: { color: '#d6dbe3' } }}
                  checked={form.isActive}
                  onChange={(event) => setForm((current) => ({ ...current, isActive: event.currentTarget.checked }))}
                />
                <Group>
                  <Button onClick={() => void savePreset()} loading={busy} color="gray">
                    저장
                  </Button>
                  <Button variant="default" styles={mutedButtonStyles} onClick={resetEditor}>
                    새 프리셋
                  </Button>
                  <Button variant="subtle" color="red" onClick={() => void removePreset()} disabled={!editingId} loading={busy}>
                    삭제
                  </Button>
                </Group>
                <Alert color="gray" styles={alertStyles}>
                  언어 프리셋은 AI 생성 시 사용되는 프롬프트 설정입니다. 결과지 프리셋은 별도 탭에서 관리하며, 생성 시 각각 독립적으로 선택합니다.
                </Alert>
                {message && <Alert color="gray" styles={alertStyles}>{message}</Alert>}
              </Stack>
            </PageSection>
          </Grid.Col>
        </Grid>
      </Tabs.Panel>

      <Tabs.Panel value="report-presets" pt="md">
        <Grid gutter="lg">
          <Grid.Col span={{ base: 12, lg: 5 }}>
            <PageSection title="결과지 프리셋 목록" description={`저장된 프리셋 ${sampleSets.length}개`}>
              <Stack>
                <Group justify="space-between">
                  <Text size="sm" c="dimmed" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                    결과지 프리셋 선택
                  </Text>
                  <Button variant="default" styles={mutedButtonStyles} onClick={() => void loadSampleSets()} loading={busy}>
                    새로고침
                  </Button>
                </Group>
                <ScrollArea h={520}>
                  <Table stickyHeader highlightOnHover>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>이름</Table.Th>
                        <Table.Th>파일 수</Table.Th>
                        <Table.Th>수정일</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {sampleSets.map((sampleSet: SampleTemplateSet) => (
                        <Table.Tr
                          key={sampleSet.id}
                          onClick={() => selectSampleSet(sampleSet)}
                          style={{
                            cursor: 'pointer',
                            background: sampleSetEditingId === sampleSet.id ? '#171d24' : undefined,
                          }}
                        >
                          <Table.Td>{sampleSet.name}</Table.Td>
                          <Table.Td>{sampleSet.templateIds.length}</Table.Td>
                          <Table.Td>{new Date(sampleSet.updatedAt).toLocaleString()}</Table.Td>
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                </ScrollArea>
              </Stack>
            </PageSection>
          </Grid.Col>

          <Grid.Col span={{ base: 12, lg: 7 }}>
            <PageSection title={sampleSetEditingId ? '결과지 프리셋 수정' : '결과지 프리셋 생성'}>
              <Stack>
                <Group justify="space-between">
                  <Badge color="gray" variant="light" radius="sm">
                    {sampleSetEditingId ? '수정 모드' : '생성 모드'}
                  </Badge>
                  <Text size="sm" c="#7f8794" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                    {sampleSetEditingId ? sampleSetEditingId.slice(0, 8) : '신규'}
                  </Text>
                </Group>
                <TextInput
                  label="결과지 프리셋 이름"
                  styles={monoLabelStyles}
                  value={sampleSetForm.name}
                  onChange={(event) => setSampleSetForm((current) => ({ ...current, name: event.currentTarget.value }))}
                />
                <MultiSelect
                  label="포함할 결과지 파일"
                  description="업로드된 DFM/PAS 파일 쌍을 선택하세요. 3개 이상도 포함할 수 있습니다."
                  data={reportTemplateOptions}
                  searchable
                  clearable
                  value={sampleSetForm.templateIds}
                  onChange={(value) => setSampleSetForm((current) => ({ ...current, templateIds: value }))}
                  styles={monoLabelStyles}
                />
                <Group>
                  <Button onClick={() => void saveSampleSet()} loading={busy} color="gray">
                    저장
                  </Button>
                  <Button variant="default" styles={mutedButtonStyles} onClick={resetSampleSetEditor}>
                    새 프리셋
                  </Button>
                  <Button variant="subtle" color="red" onClick={() => void removeSampleSet()} disabled={!sampleSetEditingId} loading={busy}>
                    삭제
                  </Button>
                </Group>
                <Alert color="gray" styles={alertStyles}>
                  결과지 프리셋은 업로드된 델파이 템플릿 파일(DFM/PAS)의 재사용 가능한 묶음입니다. 언어 프리셋에서 하나의 결과지 프리셋을 연결할 수 있습니다.
                </Alert>
                {sampleSetMessage && <Alert color="gray" styles={alertStyles}>{sampleSetMessage}</Alert>}
              </Stack>
            </PageSection>
          </Grid.Col>
        </Grid>
      </Tabs.Panel>
    </Tabs>
  )
}

export default PresetsPage
