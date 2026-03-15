import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { MantineProvider, createTheme } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import { BrowserRouter } from 'react-router-dom'
import App from './App'

const theme = createTheme({
  primaryColor: 'gray',
  defaultRadius: 'sm',
  fontFamily: 'SUIT Variable, Pretendard Variable, Pretendard, sans-serif',
  lineHeights: {
    md: '1.55',
  },
  headings: {
    fontFamily: 'JetBrains Mono, D2Coding, Consolas, monospace',
    sizes: {
      h1: { fontSize: '2rem', lineHeight: '1.2', fontWeight: '700' },
      h2: { fontSize: '1.55rem', lineHeight: '1.25', fontWeight: '700' },
      h3: { fontSize: '1.05rem', lineHeight: '1.3', fontWeight: '700' },
      h4: { fontSize: '1.05rem', lineHeight: '1.35', fontWeight: '700' },
    },
  },
  components: {
    Button: {
      defaultProps: {
        radius: 'md',
      },
    },
    TextInput: {
      defaultProps: {
        size: 'md',
      },
    },
    Textarea: {
      defaultProps: {
        size: 'md',
      },
    },
    Alert: {
      defaultProps: {
        radius: 'lg',
      },
    },
    Paper: {
      defaultProps: {
        radius: 'md',
        withBorder: true,
      },
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <MantineProvider theme={theme}>
      <Notifications position="top-right" />
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </MantineProvider>
  </React.StrictMode>,
)
