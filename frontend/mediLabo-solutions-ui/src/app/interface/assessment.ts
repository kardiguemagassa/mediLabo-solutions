import { Gender } from "../enum/gender";
import { RiskLevel } from "../enum/riskLevel";

export interface IAssessment {
  patientUuid: string;
  patientName: string;
  age: number;
  gender: Gender;
  riskLevel: RiskLevel;
  triggerCount: number;
  triggersFound: string[];
  assessedAt: string;
}

export interface IAssessmentDetail extends IAssessment {notes: string;}