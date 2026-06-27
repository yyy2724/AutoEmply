import { ScrollArea, Stack, Table, Text } from '@mantine/core'
import { colors } from '../lib/theme'
import type { ReportTemplate } from '../types'

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
                <Text c="dimmed">등록된 템플릿이 없습니다.</Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            templates.map((item) => (
              <Table.Tr
                key={item.id}
                bg={selectedTemplate?.id === item.id ? colors.rowSelectedBackground : undefined}
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

export default TemplateList
