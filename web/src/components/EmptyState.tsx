import { Box, Paper, Typography } from '@mui/material'
import type { ReactNode } from 'react'

/** A list with nothing in it yet: what it will hold, why it is empty and the way to fill it. */
export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon: ReactNode
  title: ReactNode
  description?: ReactNode
  action?: ReactNode
}) {
  return (
    <Paper
      variant="outlined"
      sx={{
        p: { xs: 3, sm: 5 },
        display: 'grid',
        justifyItems: 'center',
        textAlign: 'center',
        gap: 1.5,
      }}
    >
      <Box
        aria-hidden="true"
        sx={{
          width: 48,
          height: 48,
          borderRadius: 2,
          display: 'grid',
          placeItems: 'center',
          color: 'primary.main',
          backgroundColor: 'rgba(var(--delfos-palette-primary-mainChannel) / 0.12)',
        }}
      >
        {icon}
      </Box>
      <Typography variant="h6" component="p">
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: '48ch' }}>
          {description}
        </Typography>
      )}
      {action && <Box sx={{ mt: 1 }}>{action}</Box>}
    </Paper>
  )
}
