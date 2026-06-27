import { AppShell, Box, Button, Drawer, Group, Stack, Text } from '@mantine/core'
import { IconCommand, IconLayoutDashboard, IconLibrary, IconSparkles } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { colors, monoFontFamily, mutedButtonStyles } from './lib/theme'
import CreatePage from './pages/CreatePage'
import HomePage from './pages/HomePage'
import LibraryPage from './pages/LibraryPage'
import PresetsPage from './pages/PresetsPage'

const navItems = [
  { label: '대시보드', path: '/', icon: IconLayoutDashboard },
  { label: '생성', path: '/create', icon: IconSparkles },
  { label: '라이브러리', path: '/library', icon: IconLibrary },
]

function App() {
  const location = useLocation()
  const [presetDrawerOpened, setPresetDrawerOpened] = useState(false)

  useEffect(() => {
    function handleKeydown(event: KeyboardEvent) {
      if (event.key !== 'F1') {
        return
      }

      event.preventDefault()
      setPresetDrawerOpened((current) => !current)
    }

    window.addEventListener('keydown', handleKeydown)
    return () => window.removeEventListener('keydown', handleKeydown)
  }, [])

  return (
    <AppShell
      header={{ height: 68 }}
      navbar={{ width: 260, breakpoint: 'sm' }}
      padding="lg"
      styles={{
        main: {
          background: colors.appBackground,
          minHeight: '100vh',
        },
        header: {
          background: colors.surface,
          borderBottom: `1px solid ${colors.surfaceBorder}`,
        },
        navbar: {
          background: colors.surface,
          borderRight: `1px solid ${colors.surfaceBorder}`,
        },
      }}
    >
      <AppShell.Header>
        <Group justify="space-between" px="lg" h="100%">
          <Box>
            <Text fw={800} ff={monoFontFamily}>
              AutoEmply
            </Text>
            <Text size="xs" c={colors.textMuted} ff={monoFontFamily}>
              QuickReport 작업 화면
            </Text>
          </Box>
          <Button
            variant="default"
            styles={mutedButtonStyles}
            leftSection={<IconCommand size={15} />}
            onClick={() => setPresetDrawerOpened((current) => !current)}
          >
            프리셋 / F1
          </Button>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <Stack gap="xs">
          {navItems.map((item) => (
            <Button
              key={item.path}
              component={NavLink}
              to={item.path}
              justify="flex-start"
              variant={location.pathname === item.path ? 'filled' : 'subtle'}
              color="gray"
              leftSection={<item.icon size={17} />}
              styles={{
                root: {
                  border: `1px solid ${colors.controlBorder}`,
                  background:
                    location.pathname === item.path ? colors.navActiveBackground : 'transparent',
                },
                inner: { justifyContent: 'flex-start' },
                label: { fontFamily: monoFontFamily, color: colors.textPrimary },
              }}
            >
              {item.label}
            </Button>
          ))}
        </Stack>
        <Stack gap={4} mt="lg">
          <Text size="xs" fw={700} c={colors.textFaint} ff={monoFontFamily}>
            SYS
          </Text>
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/create" element={<CreatePage />} />
          <Route path="/library" element={<LibraryPage />} />
          <Route path="/presets" element={<PresetsPage />} />
        </Routes>
      </AppShell.Main>

      <Drawer
        opened={presetDrawerOpened}
        onClose={() => setPresetDrawerOpened(false)}
        position="right"
        size="xl"
        title={
          <Text ff={monoFontFamily} fw={700}>
            프리셋 관리
          </Text>
        }
        overlayProps={{ backgroundOpacity: 0.45, blur: 2 }}
        styles={{
          content: {
            background: colors.surface,
            borderLeft: `1px solid ${colors.controlBorder}`,
          },
          header: {
            background: colors.surface,
            borderBottom: `1px solid ${colors.surfaceBorder}`,
          },
          body: {
            background: colors.surface,
          },
        }}
      >
        <PresetsPage />
      </Drawer>
    </AppShell>
  )
}

export default App
