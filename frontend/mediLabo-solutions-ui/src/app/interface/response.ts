import { IComment } from './comment';
import { IDevice } from './device';
import { IFile } from './file';
import { IMessage } from './message';
import { IMedilaboSupport } from './medilabosupport';
import { IPatient } from './patient';
import { IAssessment } from './assessment';
import { INote } from './note';
import { IUser } from './user';
import { IPage } from './pagination.interface';

export interface IResponse {
  time: Date | string;
  code: number;
  status: string;
  message: string;
  path: string;
  exception?: string;
  data: {
    user?: IUser;
    users?: IUser[];
    devices: IDevice[];
    task: any;
    comment?: IComment;
    conversation: IMessage[];
    messages: IMessage[];
    message: IMessage;
    patient: IPatient;
    patients: IPatient[];
    report?: IPatient[];
    note: INote;
    notes: INote[];
    assessment: IAssessment;
    assessments: IAssessment[];
    pages?: number;
    comments?: IComment[];
    files?: IFile[];
    file?: IFile;
    tasks?: any[];
    assignee?: IMedilaboSupport;
    medilaboSupports?: IMedilaboSupport[];
    currentPage?: number;
    totalPages?: number;
    totalElements?: number;
    size?: number;
  };
}
