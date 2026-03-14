import { Gender } from "../enum/gender";
import { RiskLevel } from "../enum/riskLevel";

export interface IAssessmentDetail {
  patientUuid: string;
  patientName: string;

  age: number;
  gender: Gender;
  riskLevel: RiskLevel;
  triggerCount: number;
  triggersFound: string[];
  assessedAt: string;
  notes?: string; 
}