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
        <PageSection title="Preset list" description={`Total ${presets.length} / Active ${activeCount}`}>
          <Stack>
            <Group justify="space-between">
              <Text size="sm" c="dimmed" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                Language preset selection
              </Text>
              <Button variant="default" styles={mutedButtonStyles} onClick={() => void loadPresets()} loading={busy}>
                Reload
              </Button>
            </Group>
            <ScrollArea h={520}>
              <Table stickyHeader highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Name</Table.Th>
                    <Table.Th>Status</Table.Th>
                    <Table.Th>Updated</Table.Th>
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
                      <Table.Td>{preset.isActive ? 'Active' : 'Inactive'}</Table.Td>
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
        <PageSection title={editingId ? 'Edit language preset' : 'Create language preset'}>
          <Stack>
            <Group justify="space-between">
              <Badge color="gray" variant="light" radius="sm">
                {editingId ? 'Edit mode' : 'Create mode'}
              </Badge>
              <Text size="sm" c="#7f8794" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                {editingId ? editingId.slice(0, 8) : 'new'}
              </Text>
            </Group>
            <Group grow>
              <TextInput
                label="Preset name"
                styles={monoLabelStyles}
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.currentTarget.value }))}
              />
            </Group>
            <Textarea
              label="System prompt"
              styles={monoLabelStyles}
              minRows={8}
              autosize
              value={form.systemPrompt}
              onChange={(event) => setForm((current) => ({ ...current, systemPrompt: event.currentTarget.value }))}
            />
            <Textarea
              label="User prompt template"
              styles={monoLabelStyles}
              minRows={4}
              autosize
              value={form.userPromptTemplate}
              onChange={(event) => setForm((current) => ({ ...current, userPromptTemplate: event.currentTarget.value }))}
            />
            <Textarea
              label="Style rules JSON"
              styles={monoLabelStyles}
              minRows={4}
              autosize
              value={form.styleRulesJson}
              onChange={(event) => setForm((current) => ({ ...current, styleRulesJson: event.currentTarget.value }))}
            />
            <Group grow align="end">
              <NumberInput
                label="Temperature"
                styles={monoLabelStyles}
                value={form.temperature}
                onChange={(value) => setForm((current) => ({ ...current, temperature: Number(value ?? 0) }))}
              />
              <NumberInput
                label="Max tokens"
                styles={monoLabelStyles}
                value={form.maxTokens}
                onChange={(value) => setForm((current) => ({ ...current, maxTokens: Number(value ?? 32000) }))}
              />
            </Group>
            <Checkbox
              label="Use this language preset in generation"
              styles={{ label: { color: '#d6dbe3' } }}
              checked={form.isActive}
              onChange={(event) => setForm((current) => ({ ...current, isActive: event.currentTarget.checked }))}
            />
            <Checkbox
              label="Use this as representative language preset"
              styles={{ label: { color: '#d6dbe3' } }}
              checked={form.isPrimary}
              onChange={(event) => setForm((current) => ({ ...current, isPrimary: event.currentTarget.checked }))}
            />
            <Group>
              <Button onClick={() => void savePreset()} loading={busy} color="gray">
                Save
              </Button>
              <Button variant="default" styles={mutedButtonStyles} onClick={resetEditor}>
                Reset
              </Button>
              <Button variant="subtle" color="red" onClick={() => void removePreset()} disabled={!editingId} loading={busy}>
                Delete
              </Button>
            </Group>
            <Alert color="gray" styles={alertStyles}>
              Language presets define prompt rules used during AI generation. Active presets are included in create requests, and the representative preset is sent first.
            </Alert>
            {message && <Alert color="gray" styles={alertStyles}>{message}</Alert>}
          </Stack>
        </PageSection>
      </Grid.Col>
    </Grid>
  )
}

export default PresetsPage
