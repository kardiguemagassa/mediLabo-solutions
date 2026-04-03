export interface IPage<T> {
  content: T[];       
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}