import { Box, Skeleton, Stack } from '@mui/material'

/**
 * What a page shows while its data loads: the shape of the content to come instead of a spinner,
 * so the layout does not jump when it arrives. Assistive technology gets the label and the busy
 * state; the shapes are decoration.
 */
export function PageSkeleton({ label, rows = 3 }: { label: string; rows?: number }) {
  return (
    <Box role="status" aria-busy="true" aria-label={label}>
      <Stack spacing={2} aria-hidden="true">
        <Skeleton variant="text" width="40%" sx={{ fontSize: '1.75rem' }} />
        {Array.from({ length: rows }, (_, i) => (
          <Skeleton key={i} variant="rounded" height={64} />
        ))}
      </Stack>
    </Box>
  )
}
