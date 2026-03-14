export interface IComment {
  commentUuid: string;
  content: string;
  authorUuid: string;
  authorName: string;
  authorRole: string;
  authorImageUrl: string;
  edited: boolean;
  createdAt: string;
  updatedAt: string;
}