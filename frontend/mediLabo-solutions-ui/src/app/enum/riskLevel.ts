export enum RiskLevel {
  NONE = 'NONE',
  BORDERLINE = 'BORDERLINE',
  IN_DANGER = 'IN_DANGER',
  EARLY_ONSET = 'EARLY_ONSET',
  UNKNOWN = 'UNKNOWN'
};

export const RiskLevelUI = {
  [RiskLevel.NONE]: {label: 'Aucun risque', color: 'green'},
  [RiskLevel.BORDERLINE]: {label: 'Risque limité',color: 'orange'},
  [RiskLevel.IN_DANGER]: {label: 'Danger',color: 'red'},
  [RiskLevel.EARLY_ONSET]: {label: 'Apparition précoce',color: 'purple'},
  [RiskLevel.UNKNOWN]: {label: 'Inconnu',color: 'gray'}
};