import type { components } from './schema'

export type Schemas = components['schemas']
export type PublicSurvey = Schemas['PublicSurvey']
export type SurveyVersion = Schemas['SurveyVersion']
export type Question = Schemas['Question']
export type Answer = Schemas['Answer']
export type EvaluationResult = Schemas['EvaluationResult']
export type FieldError = Schemas['FieldError']
export type Problem = Schemas['Problem']
