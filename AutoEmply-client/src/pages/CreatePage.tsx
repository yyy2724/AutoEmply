import { Alert, Button, Code, FileInput, Grid, Group, List, MultiSelect, Select, Stack, Text, TextInput, Textarea } from '@mantine/core'
import { IconAlertTriangle, IconInfoCircle, IconRefresh } from '@tabler/icons-react'
import PageSection from '../components/PageSection'
import { useCreateWorkspace } from '../hooks/useCreateWorkspace'

function CreatePage() {
  const {
    aiVersion,
    busy,
    formName,
    layoutSpecJson,
    status,
    selectedFile,
    presets,
    sampleSets,
    primaryPresetId,
    referencePresetIds,
    primarySampleSetId,
    referenceSampleSetIds,
    setFormName,
    setLayoutSpecJson,
    setSelectedFile,
    setPrimaryPresetId,
    setReferencePresetIds,
    setPrimarySampleSetId,
    setReferenceSampleSetIds,
    exportFromImage,
    exportFromJson,
    generateJson,
  } = useCreateWorkspace()

  const presetOptions = presets.map((preset) => ({
    value: preset.id,
    label: `${preset.name}${preset.active ?? preset.isActive ? ' [active]' : ''}`,
  }))

  const sampleSetOptions = sampleSets.map((sampleSet) => ({
    value: sampleSet.id,
    label: `${sampleSet.name}${sampleSet.active ?? sampleSet.isActive ? ' [active]' : ''}`,
  }))

  const retryHandler = status.retryAction === 'generate-json'
    ? generateJson
    : status.retryAction === 'export-image'
      ? exportFromImage
      : status.retryAction === 'export-json'
        ? exportFromJson
        : null

  return (
    <Grid gutter="lg">
      <Grid.Col span={{ base: 12, lg: 8 }}>
        <PageSection title="Create" description="Upload a source file, generate LayoutSpec JSON, and export Delphi ZIP output.">
          <Stack>
            <TextInput label="Form name" value={formName} onChange={(event) => setFormName(event.currentTarget.value)} />
            <FileInput
              label="Source file"
              description="jpg, png, webp, gif, pdf, up to 5MB"
              value={selectedFile}
              onChange={setSelectedFile}
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,image/jpeg,image/png,image/gif,image/webp,application/pdf"
            />
            <Select
              label="Primary language preset"
              description="This preset is sent first and treated as the main prompt."
              data={presetOptions}
              value={primaryPresetId}
              onChange={setPrimaryPresetId}
              clearable
            />
            <MultiSelect
              label="Reference language presets"
              description="These presets are appended after the primary preset as references."
              data={presetOptions.filter((option) => option.value !== primaryPresetId)}
              value={referencePresetIds.filter((id) => id !== primaryPresetId)}
              onChange={setReferencePresetIds}
              searchable
              clearable
            />
            <Select
              label="Primary report preset"
              description="This report preset set is sent first."
              data={sampleSetOptions}
              value={primarySampleSetId}
              onChange={setPrimarySampleSetId}
              clearable
            />
            <MultiSelect
              label="Reference report presets"
              description="Additional report preset sets are sent after the primary selection."
              data={sampleSetOptions.filter((option) => option.value !== primarySampleSetId)}
              value={referenceSampleSetIds.filter((id) => id !== primarySampleSetId)}
              onChange={setReferenceSampleSetIds}
              searchable
              clearable
            />
            <Alert color="gray" title="Preset payload">
              <Stack gap={6}>
                <Text size="sm">
                  The selected primary preset is sent first. Additional selections are sent after it as references.
                </Text>
                <Text size="sm" c="dimmed">
                  If nothing is selected, the request is sent without that preset type.
                </Text>
              </Stack>
            </Alert>
            <Group>
              <Button onClick={generateJson} loading={busy} color="dark">Generate JSON</Button>
              <Button onClick={exportFromImage} loading={busy} variant="default">Generate ZIP</Button>
              <Button onClick={exportFromJson} loading={busy} variant="subtle">Export current JSON</Button>
              {status.retryable && retryHandler && (
                <Button leftSection={<IconRefresh size={16} />} onClick={retryHandler} disabled={busy} variant="subtle">
                  Retry
                </Button>
              )}
            </Group>
            <Textarea
              label="LayoutSpec JSON"
              minRows={22}
              autosize
              value={layoutSpecJson}
              onChange={(event) => setLayoutSpecJson(event.currentTarget.value)}
            />
            {status.message && (
              <Alert
                icon={status.tone === 'error' ? <IconAlertTriangle size={16} /> : <IconInfoCircle size={16} />}
                color={status.tone === 'error' ? 'red' : 'gray'}
                title={status.title}
              >
                <Stack gap={6}>
                  <Text size="sm">{status.message}</Text>
                  {status.details.length > 0 && (
                    <List size="sm" spacing={4}>
                      {status.details.map((detail) => (
                        <List.Item key={detail}>{detail}</List.Item>
                      ))}
                    </List>
                  )}
                </Stack>
              </Alert>
            )}
          </Stack>
        </PageSection>
      </Grid.Col>

      <Grid.Col span={{ base: 12, lg: 4 }}>
        <PageSection title="Status" description="Runtime and endpoint information used during generation.">
          <Stack>
            <Text size="sm">AI version</Text>
            <Code block>{aiVersion}</Code>
            <Text size="sm" c="dimmed">Supported APIs</Text>
            <Code block>{`POST /api/generate-json
POST /api/export-from-image
POST /api/export`}</Code>
          </Stack>
        </PageSection>
      </Grid.Col>
    </Grid>
  )
}

export default CreatePage
