export const AI_MODEL_OPTIONS = [
  { value: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6' },
  { value: 'claude-opus-4-6', label: 'Claude Opus 4.6' },
] as const

export function getAiModelLabel(model?: string | null): string {
  const matched = AI_MODEL_OPTIONS.find((option) => option.value === model)
  return matched?.label ?? (model?.trim() || 'Default')
}
