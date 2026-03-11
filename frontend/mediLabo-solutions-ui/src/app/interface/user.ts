export interface IUser {
  userId: number;
  userUuid: string;
  firstName: string;
  lastName: string;
  email: string;
  memberId: string;
  address?: string;
  phone?: string;
  title?: string;
  bio?: string;
  imageUrl?: string;
  mfa: boolean;
  enabled: boolean;
  notLocked: boolean;
  role: string;
  permissions: string;
  createdAt?: Date;
}
