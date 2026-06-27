import { Alert, Button, Code, FileInput, Grid, Group, List, Select, Stack, Text, TextInput, Textarea } from '@mantine/core'
import { IconAlertTriangle, IconInfoCircle, IconRefresh } from '@tabler/icons-react'
import PageSection from '../components/PageSection'
import { useCreateWorkspace } from '../hooks/useCreateWorkspace'
import { AI_MODEL_OPTIONS } from '../lib/aiModels'

function CreatePage() {
  const {
    aiVersion,
    busy,
    changeAiModel,
    formName,
    layoutSpecJson,
    selectedAiModel,
    status,
    selectedFile,
    presets,
    setFormName,
    setLayoutSpecJson,
    setSelectedFile,
    exportFromImage,
    exportFromJson,
    generateJson,
  } = useCreateWorkspace()

  const retryHandler = status.retryAction === 'generate-json'
    ? generateJson
    : status.retryAction === 'export-image'
      ? exportFromImage
      : status.retryAction === 'export-json'
        ? exportFromJson
        : null

  const activePresetCount = presets.filter((preset) => preset.isActive).length

  return (
    <Grid gutter="lg">
      <Grid.Col span={{ base: 12, lg: 8 }}>
        <PageSection title="생성" description="원본 파일을 업로드하고 LayoutSpec JSON을 생성한 뒤 Delphi ZIP 결과물을 내보냅니다.">
          <Stack>
            <TextInput label="폼 이름" value={formName} onChange={(event) => setFormName(event.currentTarget.value)} />
            <FileInput
              label="원본 파일"
              description="jpg, png, webp, gif, pdf · 최대 5MB"
              value={selectedFile}
              onChange={setSelectedFile}
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,image/jpeg,image/png,image/gif,image/webp,application/pdf"
            />
            <Alert color="gray" title="프리셋 전송">
              <Stack gap={6}>
                <Text size="sm">
                  생성 요청 시 활성화된 언어 프리셋 {activePresetCount}개가 API로 함께 전송됩니다.
                </Text>
                <Text size="sm" c="dimmed">
                  프리셋 관리 화면에서 대표로 지정한 항목이 가장 먼저 전송됩니다.
                </Text>
              </Stack>
            </Alert>
            <Group>
              <Button onClick={generateJson} loading={busy} color="dark">JSON 생성</Button>
              <Button onClick={exportFromImage} loading={busy} variant="default">ZIP 생성</Button>
              <Button onClick={exportFromJson} loading={busy} variant="subtle">현재 JSON 내보내기</Button>
              {status.retryable && retryHandler && (
                <Button leftSection={<IconRefresh size={16} />} onClick={retryHandler} disabled={busy} variant="subtle">
                  다시 시도
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
        <PageSection title="상태" description="생성에 사용되는 런타임 및 엔드포인트 정보입니다.">
          <Stack>
            <Text size="sm">AI 버전</Text>
            <Code block>{aiVersion}</Code>
            <Select
              label="모델"
              data={AI_MODEL_OPTIONS.map((option) => ({ value: option.value, label: option.label }))}
              value={selectedAiModel}
              allowDeselect={false}
              onChange={(value) => void changeAiModel(value)}
              disabled={busy}
            />
            <Text size="sm" c="dimmed">지원 API</Text>
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
