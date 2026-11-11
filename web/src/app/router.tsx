import { createBrowserRouter } from 'react-router'
import { AppShell } from '../components/AppShell'
import { AnswerPage } from '../features/answer/AnswerPage'
import { Landing } from '../pages/Landing'
import { NotFound } from '../pages/NotFound'

export const router = createBrowserRouter([
  {
    path: '/',
    Component: AppShell,
    children: [
      { index: true, Component: Landing },
      { path: 's/:publicId', Component: AnswerPage },
      { path: '*', Component: NotFound },
    ],
  },
])
