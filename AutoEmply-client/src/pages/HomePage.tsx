import { Button, Group, Stack } from '@mantine/core'
import { Link } from 'react-router-dom'
import PageSection from '../components/PageSection'

function HomePage() {
  return (
    <Stack gap="lg">
      <PageSection title="AutoEmply">
        <Group>
          <Button
            component={Link}
            to="/create"
            color="gray"
            styles={{
              root: {
                background: '#d6dbe3',
                color: '#0b0e12',
                border: '1px solid #d6dbe3',
              },
            }}
          >
            생성
          </Button>
          <Button
            component={Link}
            to="/library"
            variant="default"
            styles={{
              root: {
                border: '1px solid #2f353e',
                background: '#151a21',
                color: '#d6dbe3',
              },
            }}
          >
            라이브러리
          </Button>
        </Group>
      </PageSection>
    </Stack>
  )
}

export default HomePage
