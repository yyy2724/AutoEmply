import { Paper, Stack, Text, Title } from '@mantine/core'
import { colors, panelStyle } from '../lib/theme'
import type { PageSectionProps } from '../types'

function PageSection({ title, description, children }: PageSectionProps) {
  return (
    <Paper p="xl" style={panelStyle}>
      <Stack gap="lg">
        {(title || description) && (
          <Stack gap={4}>
            {title && <Title order={3}>{title}</Title>}
            {description && (
              <Text size="sm" lh={1.55} c={colors.textSecondary}>
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
