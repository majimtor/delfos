import { createBrowserRouter } from 'react-router'
import { AppShell } from '../components/AppShell'
import { AnswerPage } from '../features/answer/AnswerPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RequireSession } from '../features/auth/RequireSession'
import { Landing } from '../pages/Landing'
import { NotFound } from '../pages/NotFound'

// The public pages ship with the application; the manager's pages load on first use, each as its
// own chunk, so a respondent downloads none of the editor, the tables or the results.
export const router = createBrowserRouter([
  {
    path: '/',
    Component: AppShell,
    children: [
      { index: true, Component: Landing },
      { path: 's/:publicId', Component: AnswerPage },
      { path: 'login', Component: LoginPage },
      {
        Component: RequireSession,
        children: [
          {
            path: 'surveys',
            lazy: async () => ({
              Component: (await import('../features/surveys/SurveysPage')).SurveysPage,
            }),
          },
          {
            path: 'surveys/new',
            lazy: async () => ({
              Component: (await import('../features/editor/NewSurveyPage')).NewSurveyPage,
            }),
          },
          {
            path: 'surveys/:surveyId/draft',
            lazy: async () => ({
              Component: (await import('../features/editor/EditorPage')).EditorPage,
            }),
          },
          {
            path: 'surveys/:surveyId/invitations',
            lazy: async () => ({
              Component: (await import('../features/invitations/InvitationsPage')).InvitationsPage,
            }),
          },
          {
            path: 'accounts',
            lazy: async () => ({
              Component: (await import('../features/accounts/AccountsPage')).AccountsPage,
            }),
          },
          {
            path: 'surveys/:surveyId/versions/:versionNumber/results',
            lazy: async () => ({
              Component: (await import('../features/results/ResultsPage')).ResultsPage,
            }),
          },
        ],
      },
      { path: '*', Component: NotFound },
    ],
  },
])
