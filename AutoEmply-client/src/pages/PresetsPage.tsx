import { Alert, Badge, Button, Checkbox, Grid, Group, NumberInput, ScrollArea, Stack, Table, Text, TextInput, Textarea } from '@mantine/core'
import PageSection from '../components/PageSection'
import { usePromptPresets } from '../hooks/usePromptPresets'
import type { PromptPreset } from '../types'

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
    message,
    presets,
    loadPresets,
    removePreset,
    resetEditor,
    savePreset,
    selectPreset,
    setForm,
  } = usePromptPresets()

  return (
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
              <Table
                stickyHeader
                highlightOnHover
                styles={{
                  th: {
                    fontFamily: 'JetBrains Mono, D2Coding, Consolas, monospace',
                    fontSize: '0.74rem',
                    textTransform: 'uppercase',
                    letterSpacing: '0.08em',
                    color: '#87909d',
                    background: '#141920',
                    borderColor: '#2b313a',
                  },
                  td: {
                    borderColor: '#2b313a',
                  },
                  tr: {
                    transition: 'background-color 140ms ease',
                  },
                }}
              >
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
                      <Table.Td>
                        <Text
                          component="span"
                          ff="JetBrains Mono, D2Coding, Consolas, monospace"
                          size="xs"
                          style={{
                            letterSpacing: '0.04em',
                            color: preset.active ?? preset.isActive ? '#cfd6e0' : '#6f7785',
                          }}
                        >
                          {preset.active ?? preset.isActive ? '활성' : '비활성'}
                        </Text>
                      </Table.Td>
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
        <PageSection title={editingId ? '프리셋 수정' : '새 프리셋'}>
          <Stack>
            <Group justify="space-between">
              <Badge color="gray" variant="light" radius="sm">
                {editingId ? '수정 모드' : '생성 모드'}
              </Badge>
              <Text size="sm" c="#7f8794" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                {editingId ? editingId.slice(0, 8) : '미저장'}
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
              styles={{
                label: { color: '#d6dbe3' },
              }}
              checked={form.isActive}
              onChange={(event) => setForm((current) => ({ ...current, isActive: event.currentTarget.checked }))}
            />
            <Group>
              <Button
                onClick={() => void savePreset()}
                loading={busy}
                color="gray"
                styles={{
                  root: {
                    background: '#d6dbe3',
                    color: '#0b0e12',
                    border: '1px solid #d6dbe3',
                  },
                }}
              >
                저장
              </Button>
              <Button variant="default" styles={mutedButtonStyles} onClick={resetEditor}>
                새 프리셋
              </Button>
              <Button variant="subtle" color="red" onClick={() => void removePreset()} disabled={!editingId} loading={busy}>
                삭제
              </Button>
            </Group>
            {message && <Alert color="gray" styles={alertStyles}>{message}</Alert>}
          </Stack>
        </PageSection>
      </Grid.Col>
    </Grid>
  )
}

export default PresetsPage
