export interface IResponse {
    time: Date | string;
    code: number;
    status: string
    message: string
    path: string
    exception?: string
    data: any
}