import { Button, Group, Stack } from '@mantine/core'
import { Link } from 'react-router-dom'
import PageSection from '../components/PageSection'
import { colors, mutedButtonStyles } from '../lib/theme'

const primaryButtonStyles = {
  root: {
    background: colors.textPrimary,
    color: colors.appBackground,
    border: `1px solid ${colors.textPrimary}`,
  },
}

function HomePage() {
  return (
    <Stack gap="lg">
      <PageSection title="AutoEmply">
        <Group>
          <Button component={Link} to="/create" color="gray" styles={primaryButtonStyles}>
            생성
          </Button>
          <Button component={Link} to="/library" variant="default" styles={mutedButtonStyles}>
            라이브러리
          </Button>
        </Group>
      </PageSection>
    </Stack>
  )
}

export default HomePage
