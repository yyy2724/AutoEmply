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

  const activeSampleSetCount = sampleSets.filter((sampleSet) => sampleSet.isActive).length

  return (
    <Tabs defaultValue="language-presets">
      <Tabs.List>
        <Tabs.Tab value="language-presets">Language presets</Tabs.Tab>
        <Tabs.Tab value="report-presets">Report presets</Tabs.Tab>
      </Tabs.List>

      <Tabs.Panel value="language-presets" pt="md">
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
                  <TextInput
                    label="Model"
                    styles={monoLabelStyles}
                    value={form.model}
                    onChange={(event) => setForm((current) => ({ ...current, model: event.currentTarget.value }))}
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
      </Tabs.Panel>

      <Tabs.Panel value="report-presets" pt="md">
        <Grid gutter="lg">
          <Grid.Col span={{ base: 12, lg: 5 }}>
            <PageSection title="Report preset list" description={`Total ${sampleSets.length} / Active ${activeSampleSetCount}`}>
              <Stack>
                <Group justify="space-between">
                  <Text size="sm" c="dimmed" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                    Report preset selection
                  </Text>
                  <Button variant="default" styles={mutedButtonStyles} onClick={() => void loadSampleSets()} loading={busy}>
                    Reload
                  </Button>
                </Group>
                <ScrollArea h={520}>
                  <Table stickyHeader highlightOnHover>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>Status</Table.Th>
                        <Table.Th>Files</Table.Th>
                        <Table.Th>Updated</Table.Th>
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
                          <Table.Td>{sampleSet.isActive ? 'Active' : 'Inactive'}</Table.Td>
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
            <PageSection title={sampleSetEditingId ? 'Edit report preset' : 'Create report preset'}>
              <Stack>
                <Group justify="space-between">
                  <Badge color="gray" variant="light" radius="sm">
                    {sampleSetEditingId ? 'Edit mode' : 'Create mode'}
                  </Badge>
                  <Text size="sm" c="#7f8794" ff="JetBrains Mono, D2Coding, Consolas, monospace">
                    {sampleSetEditingId ? sampleSetEditingId.slice(0, 8) : 'new'}
                  </Text>
                </Group>
                <TextInput
                  label="Report preset name"
                  styles={monoLabelStyles}
                  value={sampleSetForm.name}
                  onChange={(event) => setSampleSetForm((current) => ({ ...current, name: event.currentTarget.value }))}
                />
                <MultiSelect
                  label="Included report files"
                  description="Select uploaded DFM/PAS templates to include in this preset."
                  data={reportTemplateOptions}
                  searchable
                  clearable
                  value={sampleSetForm.templateIds}
                  onChange={(value) => setSampleSetForm((current) => ({ ...current, templateIds: value }))}
                  styles={monoLabelStyles}
                />
                <Checkbox
                  label="Use this report preset in generation"
                  styles={{ label: { color: '#d6dbe3' } }}
                  checked={sampleSetForm.isActive}
                  onChange={(event) => setSampleSetForm((current) => ({ ...current, isActive: event.currentTarget.checked }))}
                />
                <Checkbox
                  label="Use this as representative report preset"
                  styles={{ label: { color: '#d6dbe3' } }}
                  checked={sampleSetForm.isPrimary}
                  onChange={(event) => setSampleSetForm((current) => ({ ...current, isPrimary: event.currentTarget.checked }))}
                />
                <Group>
                  <Button onClick={() => void saveSampleSet()} loading={busy} color="gray">
                    Save
                  </Button>
                  <Button variant="default" styles={mutedButtonStyles} onClick={resetSampleSetEditor}>
                    Reset
                  </Button>
                  <Button variant="subtle" color="red" onClick={() => void removeSampleSet()} disabled={!sampleSetEditingId} loading={busy}>
                    Delete
                  </Button>
                </Group>
                <Alert color="gray" styles={alertStyles}>
                  Report presets are reusable DFM/PAS reference bundles. Active report presets are included in create requests, and the representative preset is sent first.
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
