export interface IFile {
    fileId: number;
    fileUuid: string;
    originalName: string;
    storedName: string;
    extension: string;
    contentType: string;

    size: number;
    formattedSize: string;
    uri: string;
    
    uploadedByUuid: string;
    uploadedByName: string;
    uploadedByRole: string;
    uploadedAt: string;
}