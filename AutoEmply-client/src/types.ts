import type { ReactNode } from 'react'

export type PageSectionProps = {
  title?: string
  description?: string
  children?: ReactNode
}

export type PromptPreset = {
  id: string
  name: string
  systemPrompt: string
  userPromptTemplate: string
  styleRulesJson?: string | null
  model?: string | null
  temperature?: number | null
  maxTokens?: number | null
  isActive: boolean
  isPrimary: boolean
  createdAt: string
  updatedAt: string
}

export type PromptPresetForm = {
  name: string
  systemPrompt: string
  userPromptTemplate: string
  styleRulesJson: string
  model: string
  temperature: number
  maxTokens: number
  isActive: boolean
  isPrimary: boolean
}

export type SampleTemplateSet = {
  id: string
  name: string
  templateIds: string[]
  isActive: boolean
  isPrimary: boolean
  createdAt: string
  updatedAt: string
}

export type SampleTemplateSetForm = {
  name: string
  templateIds: string[]
  isActive: boolean
  isPrimary: boolean
}

export type LayoutItem = {
  name?: string
  type: string
  left: number
  top: number
  width: number
  height: number
  caption?: string
  align?: string
  fontSize?: number
  bold?: boolean
  transparent?: boolean
  textColor?: string
  orientation?: string
  thickness?: number
  strokeColor?: string
  fillColor?: string
  filled?: boolean
  stretch?: boolean
  onPrint?: string
}

export type LayoutSpec = {
  items: LayoutItem[]
  pas?: {
    uses?: string[]
    methods?: Array<{
      declaration: string
      body: string[]
    }>
  }
}

export type WorkspaceStatus = {
  tone: 'info' | 'error'
  title: string
  message: string
  details: string[]
  retryable: boolean
  retryAction: 'generate-json' | 'export-image' | 'export-json' | null
}

export type AiVersionResponse = {
  version?: string
  model?: string
}

export type ReportTemplate = {
  id: string
  name: string
  category: string
  originalFormName: string
  hasPreview: boolean
  previewContentType?: string | null
  createdAt?: string
  updatedAt?: string
}

export type UploadTemplateRequest = {
  name: string
  category: string
  dfmFile: File
  pasFile: File
  previewFile?: File | null
}
