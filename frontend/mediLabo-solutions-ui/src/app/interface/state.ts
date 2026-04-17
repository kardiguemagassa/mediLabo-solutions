import { IAssessment, IAssessmentDetail } from "./assessment";
import { IDevice } from "./device";
import { IMessage } from "./message";
import { INote } from "./note";
import { IPage } from "./pagination.interface";
import { IPatient } from "./patient";
import { IPatientDetail } from "./patientDetail";
import { defaultQuery, IQuery } from "./query";
import { IUser } from "./user";

export interface IState {
    loading: boolean;
    profile?: IUser;
    user?: IUser;
    patient?: IPatient;               
    patients?: IPatient[];
    PatientDetail?: IPatientDetail;
    allPatients?: IPatient[];
    noteDetail?: INote;                
    notes?: INote[];
    allNotes?: INote[];
    assessments?: IAssessment[];
    assessmentDetail?: IAssessmentDetail;
    allAssessments?: IAssessment[];
    pages?: number;
    currentPage?: number;
    reportRequest?: {},
    error?: string;
    query?: IQuery;
    users?: IUser[];
    report?: IPatient[];
    messages?: IMessage[];
    conversation?: IMessage[];
    devices?: IDevice[];
    userPage?: IPage<IUser>;
    patientPage?: IPage<IPatient>;
    notePage?: IPage<INote>;
}

export const initialState: IState = {
    profile: null, user: null, users: null,
    patient: null,                    
    PatientDetail: null,
    patients: null, allPatients: null,
    noteDetail: null, notes: null, allNotes: null,
    assessments: null, assessmentDetail: null, allAssessments: null,
    pages: null, currentPage: 0, reportRequest: undefined,
    loading: false, error: null,
    messages: null, conversation: null, devices: null,
    query: defaultQuery,
    userPage: null,
    patientPage: null, 
    notePage: null
};