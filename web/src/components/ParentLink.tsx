import { Link } from '@mui/material'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router'

/**
 * The page above this one, as an overline over the title: the way back that a manager's page
 * offers besides the shell. A themed link, so it reads in both schemes and underlines on hover.
 */
export function ParentLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link component={RouterLink} to={to} variant="overline" underline="hover">
      {children}
    </Link>
  )
}
