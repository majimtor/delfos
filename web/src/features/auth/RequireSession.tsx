import { Box, CircularProgress } from '@mui/material'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from './session'

/** Renders the manager pages only with a session; otherwise sends to the login and comes back after it. */
export function RequireSession() {
  const session = useSession()
  const location = useLocation()
  if (session.isPending) {
    return (
      <Box role="status" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
      </Box>
    )
  }
  if (!session.data) {
    return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />
  }
  return <Outlet />
}
