import { createBrowserRouter } from 'react-router'
import { AppShell } from '../components/AppShell'
import { AnswerPage } from '../features/answer/AnswerPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RequireSession } from '../features/auth/RequireSession'
import { EditorPage } from '../features/editor/EditorPage'
import { InvitationsPage } from '../features/invitations/InvitationsPage'
import { NewSurveyPage } from '../features/editor/NewSurveyPage'
import { ResultsPage } from '../features/results/ResultsPage'
import { SurveysPage } from '../features/surveys/SurveysPage'
import { Landing } from '../pages/Landing'
import { NotFound } from '../pages/NotFound'

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
          { path: 'surveys', Component: SurveysPage },
          { path: 'surveys/new', Component: NewSurveyPage },
          { path: 'surveys/:surveyId/draft', Component: EditorPage },
          { path: 'surveys/:surveyId/invitations', Component: InvitationsPage },
          { path: 'surveys/:surveyId/versions/:versionNumber/results', Component: ResultsPage },
        ],
      },
      { path: '*', Component: NotFound },
    ],
  },
])
