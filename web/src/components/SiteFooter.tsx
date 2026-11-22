import { Box, Container, Link, Typography } from '@mui/material'
import { FormattedMessage } from 'react-intl'

const repository = 'https://github.com/majimtor/delfos'

/** The links every page ends with: the code, its licence, what was checked for accessibility, the version. */
export function SiteFooter() {
  return (
    <Box component="footer" sx={{ borderTop: 1, borderColor: 'divider', mt: 6 }}>
      <Container
        maxWidth="md"
        sx={{
          py: 2.5,
          display: 'flex',
          flexWrap: 'wrap',
          gap: { xs: 1.5, sm: 3 },
          alignItems: 'center',
        }}
      >
        <Link href={repository} color="text.secondary" underline="hover" variant="body2">
          <FormattedMessage id="footer.code" defaultMessage="Code on GitHub" />
        </Link>
        <Link
          href={`${repository}/blob/main/LICENSE`}
          color="text.secondary"
          underline="hover"
          variant="body2"
        >
          <FormattedMessage id="footer.licence" defaultMessage="MIT licence" />
        </Link>
        <Link
          href={`${repository}/blob/main/docs/accessibility.md`}
          color="text.secondary"
          underline="hover"
          variant="body2"
        >
          <FormattedMessage id="footer.accessibility" defaultMessage="Accessibility" />
        </Link>
        <Typography variant="body2" color="text.secondary" sx={{ marginLeft: { sm: 'auto' } }}>
          <Link href={`${repository}/releases`} color="inherit" underline="hover">
            <FormattedMessage
              id="footer.version"
              defaultMessage="Version {version}"
              values={{ version: __DELFOS_VERSION__ }}
            />
          </Link>
        </Typography>
      </Container>
    </Box>
  )
}
