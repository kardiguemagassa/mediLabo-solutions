export interface IQuery {
    page: number;
    size: number;
    sortBy?: string;
    direction?: string;
    status?: string;
    type?: string;
    filter?: string;
}

export const defaultQuery: IQuery = { page: 0, size: 10, sortBy: 'createdAt', direction: 'desc', status: '', type: '', filter: ''};