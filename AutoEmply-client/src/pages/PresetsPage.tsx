import {
  Alert,
  Badge,
  Button,
  Checkbox,
  Grid,
  Group,
  NumberInput,
  ScrollArea,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core'
import PageSection from '../components/PageSection'
import { usePromptPresets } from '../hooks/usePromptPresets'
import { alertStyles, colors, monoFontFamily, monoLabelStyles, mutedButtonStyles } from '../lib/theme'
import type { PromptPreset } from '../types'

const checkboxLabelStyles = { label: { color: colors.textPrimary } }

function PresetsPage() {
  const {
    activeCount,
    busy,
    editingId,
    form,
    loadPresets,
    message,
    presets,
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
              <Text size="sm" c="dimmed" ff={monoFontFamily}>
                언어 프리셋 선택
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
                  {presets.length === 0 ? (
                    <Table.Tr>
                      <Table.Td colSpan={3}>
                        <Text c="dimmed">등록된 프리셋이 없습니다.</Text>
                      </Table.Td>
                    </Table.Tr>
                  ) : (
                    presets.map((preset: PromptPreset) => (
                      <Table.Tr
                        key={preset.id}
                        onClick={() => selectPreset(preset)}
                        style={{
                          cursor: 'pointer',
                          background: editingId === preset.id ? colors.rowSelectedBackground : undefined,
                        }}
                      >
                        <Table.Td>{preset.name}</Table.Td>
                        <Table.Td>{preset.isActive ? '활성' : '비활성'}</Table.Td>
                        <Table.Td>{new Date(preset.updatedAt).toLocaleString()}</Table.Td>
                      </Table.Tr>
                    ))
                  )}
                </Table.Tbody>
              </Table>
            </ScrollArea>
          </Stack>
        </PageSection>
      </Grid.Col>

      <Grid.Col span={{ base: 12, lg: 7 }}>
        <PageSection title={editingId ? '언어 프리셋 수정' : '언어 프리셋 생성'}>
          <Stack>
            <Group justify="space-between">
              <Badge color="gray" variant="light" radius="sm">
                {editingId ? '수정 모드' : '생성 모드'}
              </Badge>
              <Text size="sm" c={colors.textMuted} ff={monoFontFamily}>
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
                label="온도 (Temperature)"
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
              label="생성 시 이 언어 프리셋을 사용"
              styles={checkboxLabelStyles}
              checked={form.isActive}
              onChange={(event) => setForm((current) => ({ ...current, isActive: event.currentTarget.checked }))}
            />
            <Checkbox
              label="대표 언어 프리셋으로 사용"
              styles={checkboxLabelStyles}
              checked={form.isPrimary}
              onChange={(event) => setForm((current) => ({ ...current, isPrimary: event.currentTarget.checked }))}
            />
            <Group>
              <Button onClick={() => void savePreset()} loading={busy} color="gray">
                저장
              </Button>
              <Button variant="default" styles={mutedButtonStyles} onClick={resetEditor}>
                초기화
              </Button>
              <Button variant="subtle" color="red" onClick={() => void removePreset()} disabled={!editingId} loading={busy}>
                삭제
              </Button>
            </Group>
            <Alert color="gray" styles={alertStyles}>
              언어 프리셋은 AI 생성에 사용되는 프롬프트 규칙을 정의합니다. 활성 프리셋은 생성 요청에 포함되며, 대표 프리셋이 가장 먼저 전송됩니다.
            </Alert>
            {message && <Alert color="gray" styles={alertStyles}>{message}</Alert>}
          </Stack>
        </PageSection>
      </Grid.Col>
    </Grid>
  )
}

export default PresetsPage
