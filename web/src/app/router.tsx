import { createBrowserRouter } from 'react-router'
import { AppShell } from '../components/AppShell'
import { AnswerPage } from '../features/answer/AnswerPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RequireSession } from '../features/auth/RequireSession'
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
          { path: 'surveys/:surveyId/versions/:versionNumber/results', Component: ResultsPage },
        ],
      },
      { path: '*', Component: NotFound },
    ],
  },
])
