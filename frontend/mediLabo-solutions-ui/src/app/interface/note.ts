import { IComment } from './comment';
import { IFile } from './file';

export interface INote {
  id: string;
  noteUuid: string;
  patientUuid: string;
  practitionerUuid: string;
  practitionerName: string;
  content: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  files: IFile[];
  comments: IComment[];
  fileCount: number;
  commentCount: number;
}