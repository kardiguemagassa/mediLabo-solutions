import { IUser } from "./user";

export interface IResponse {
    time: Date | string;
    code: number;
    status: string
    message: string
    path: string
    exception?: string
    data: {user?: IUser, users?: IUser[]};
}