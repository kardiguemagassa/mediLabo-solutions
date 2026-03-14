export interface IQuery {
    page: number;
    size: number;
    status?: string;
    type?: string;
    filter?: string;
}

export const defaultQuery: IQuery = { page: 0, size: 12, status: '', type: '', filter: '' };