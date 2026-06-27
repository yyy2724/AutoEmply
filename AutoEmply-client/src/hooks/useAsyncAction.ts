import { useCallback, useState } from 'react'

type ErrorHandler = string | ((error: unknown) => void)

/**
 * Shared busy/message state for async actions.
 *
 * `run` sets `busy`, clears `message`, awaits the action, and routes failures:
 * pass a fallback string to surface the error via `message`, or a callback for
 * custom error handling.
 */
export function useAsyncAction() {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  const run = useCallback(async (action: () => Promise<void>, onError: ErrorHandler) => {
    setBusy(true)
    setMessage('')
    try {
      await action()
    } catch (error) {
      if (typeof onError === 'function') {
        onError(error)
      } else {
        setMessage(error instanceof Error ? error.message : onError)
      }
    } finally {
      setBusy(false)
    }
  }, [])

  return { busy, message, run, setMessage }
}
