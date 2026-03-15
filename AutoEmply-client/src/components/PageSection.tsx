import { Paper, Stack, Text, Title } from '@mantine/core'
import type { PageSectionProps } from '../types'

const panelStyle = {
  border: '1px solid #2a2f38',
  background: '#11151b',
  boxShadow: 'none',
}
//아이고야1
function PageSection({ title, description, children }: PageSectionProps) {
  return (
    <Paper p="xl" style={panelStyle}>
      <Stack gap="lg">
        {(title || description) && (
          <Stack gap={4}>
            {title && <Title order={3}>{title}</Title>}
            {description && (
              <Text size="sm" lh={1.55} c="#8b93a1">
                {description}
              </Text>
            )}
          </Stack>
        )}
        {children}
      </Stack>
    </Paper>
  )
}

export default PageSection
